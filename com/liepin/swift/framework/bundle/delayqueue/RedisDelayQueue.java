package com.liepin.swift.framework.bundle.delayqueue;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.liepin.cache.redis.EnumExist;
import com.liepin.cache.redis.EnumTime;
import com.liepin.cache.redis.IRedisCacheClient;
import com.liepin.cache.redis.conf.ConfRedisCacheClientBean;
import com.liepin.cache.redis.pubsub.AbstractSubscriber;
import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.other.NamedThreadFactory;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.zookeeper.client.leader.NewLeader;
import com.liepin.zookeeper.client.listener.LeaderChangeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 基于redis的key过期事件通知功能实现的分布式延迟队列
 * <p>
 * 一个redis分组看成一个延迟队列，在同一个服务进程节点里构建此延迟队列对象传入相同的redis分组相当于单例.<br>
 * 功能支持：key前缀过滤、延迟事件同步或异步多线程处理、服务单节点消费或多节点同时消费.<br>
 * <p>
 * 申请流程：<br>
 * 缓存中心申请项目新的缓存分组，只需要创建一个shard分片.(最好独立rdis节点，key过期事件实时性好，因为如果有大量的其他缓存
 * 过期，那么key过期事件有可能不是那么准时。并且开启key过期通知功能，cpu有稍微的性能损耗，不是特别大). 开启此节点的key过期通
 * 知功能："config set notify-keyspace-events Ex"<br>
 * <p>
 * 创建延迟队列代码:<br>
 * {code}<br>
 * RedisDelayQueue redisDelayQueue = new RedisDelayQueue("default", "key前缀", new
 * IDelayHandle() {<br>
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;@Override<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;public void entry(String element) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// do something<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * <br>
 * });<br>
 * // 添加延迟事件<br>
 * redisDelayQueue.add("key***", "多少秒后触发");<br>
 * {code} <br>
 * 
 * @author yuanxl
 *
 */
public class RedisDelayQueue {

    private static final Logger logger = Logger.getLogger(RedisDelayQueue.class);

    private String redisGroup;// redis分组
    private boolean cluster = false;// 是否每个节点都处理
    private boolean asyn = false; // 是否异步线程执行
    private int nThreads = 10; // 异步线程数，默认10
    private String pattern = null; // key匹配，前缀匹配
    private IDelayHandle delayHandle;// 延迟回调处理器
    private boolean envIsolated;// 是否环境隔离
    private String areaPrefix = SystemUtil.getLogicAreaStr() + "_Q_";// 环境隔离前缀

    private IRedisCacheClient redisCacheClient;
    private NewLeader leader;
    private volatile boolean isLeader = true;// 判断是否本节点处理
    private String leaderNode;
    private final AtomicInteger pk = new AtomicInteger(1);

    private static final String KEY_EXPIRED_PATTERN = "__keyevent@*__:expired";
    private AbstractSubscriber keyExpiredSubscriber;
    private static final String KEY_VALUE = "1";

    private ExecutorService executorService;
    private LinkedBlockingQueue<Runnable> handleQueue;
    private static final int QUEUE_CAPACITY = 1000;
    private final AtomicLong doneStat = new AtomicLong(0);

    private volatile boolean initialized = false;

    /**
     * 场景：同步、单节点消费
     * 
     * @param redisGroup 缓存分组
     * @param pattern 可空，匹配所有；非空，前缀匹配
     * @param delayHandle 延迟事件处理器
     */
    public RedisDelayQueue(String redisGroup, String pattern, IDelayHandle delayHandle) {
        this(redisGroup, pattern, delayHandle, false, false, 0);
    }

    /**
     * 场景：同步、可以多节点消费
     * 
     * @param redisGroup 缓存分组
     * @param pattern 可空，匹配所有；非空，前缀匹配
     * @param delayHandle 延迟事件处理器
     * @param cluster true:多节点消费、false:单节点消费
     */
    public RedisDelayQueue(String redisGroup, String pattern, IDelayHandle delayHandle, boolean cluster) {
        this(redisGroup, pattern, delayHandle, cluster, false, 0);
    }

    /**
     * 场景：可以异步、单节点消费
     * 
     * @param redisGroup 缓存分组
     * @param pattern 可空，匹配所有；非空，前缀匹配
     * @param delayHandle 延迟事件处理器
     * @param asyn true:异步、false:同步
     * @param threadCnt 线程数
     */
    public RedisDelayQueue(String redisGroup, String pattern, IDelayHandle delayHandle, boolean asyn, int threadCnt) {
        this(redisGroup, pattern, delayHandle, false, asyn, threadCnt);
    }

    /**
     * 场景：可以异步、可以多节点消费
     * 
     * @param redisGroup 缓存分组
     * @param pattern 可空，匹配所有；非空，前缀匹配
     * @param delayHandle 延迟事件处理器
     * @param cluster true:多节点消费、false:单节点消费
     * @param asyn true:异步、false:同步
     * @param threadCnt 线程数
     */
    public RedisDelayQueue(String redisGroup, String pattern, IDelayHandle delayHandle, boolean cluster, boolean asyn,
            int threadCnt) {
        this(redisGroup, pattern, delayHandle, cluster, asyn, threadCnt, false);
    }

    /**
     * 场景：可以异步、可以多节点消费
     * 
     * @param redisGroup 缓存分组
     * @param pattern 可空，匹配所有；非空，前缀匹配
     * @param delayHandle 延迟事件处理器
     * @param cluster true:多节点消费、false:单节点消费
     * @param asyn true:异步、false:同步
     * @param threadCnt 线程数
     * @param envIsolated 是否环境隔离，比如线上环境，sandbox和formal隔离. 隔离方式通过创建不同队列
     */
    public RedisDelayQueue(String redisGroup, String pattern, IDelayHandle delayHandle, boolean cluster, boolean asyn,
            int threadCnt, boolean envIsolated) {
        this.envIsolated = envIsolated;
        pattern = addPrefix(pattern);
        this.redisGroup = redisGroup;
        this.pattern = pattern;
        this.delayHandle = delayHandle;
        this.cluster = cluster;
        this.asyn = asyn;
        this.nThreads = threadCnt;
        this.leaderNode = "redisDelayQueue_" + ProjectId.getProjectName() + "_" + redisGroup + "_" + pattern;
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException("redis延迟队列启动失败: " + leaderNode, e);
        }
    }

    private String addPrefix(String pattern) {
        return (this.envIsolated) ? areaPrefix + pattern : pattern;
    }

    private String removePrefix(String message) {
        return (this.envIsolated) ? message.substring(areaPrefix.length()) : message;
    }

    private void init() throws Exception {
        ConfRedisCacheClientBean bean = new ConfRedisCacheClientBean(ProjectId.getProjectName(), redisGroup);
        this.redisCacheClient = bean.getObject();

        // 单节点消费
        if (!cluster) {
            if (SwiftConfig.enableZookeeper()) {
                startLeader();
            }
        }
        if (isLeader) {
            startSubscriber();
            logger.warn("redis延迟队列, 本服务节点启动消费: " + leaderNode);
        }

        if (asyn) {
            createThreadPoolExecutor();
        }
        initialized = true;
        logger.warn("redis延迟队列, 启动成功: " + leaderNode);
    }

    private String getLeaderId() {
        StringBuilder idBuilder = new StringBuilder(SystemUtil.getInNetworkIp());
        String pod = SystemUtil.getPod();
        if (pod != null) {
            idBuilder.append('_').append(pod);
        }
        idBuilder.append('_').append(Thread.currentThread().getName()).append('_').append(pk.getAndIncrement());
        return idBuilder.toString();
    }

    private void startLeader() {
        this.leader = ZookeeperFactory.useDefaultZookeeper().createLeader(leaderNode, getLeaderId());
        this.isLeader = leader.start(new LeaderChangeListener() {

            @Override
            public void leaderChanged(boolean isSelfLeader) {
                if (initialized) {
                    if (isLeader == true && isSelfLeader == false) {
                        stopSubscriber();
                        isLeader = isSelfLeader;
                        logger.warn("redis延迟队列, 本服务节点停止消费: " + leaderNode);
                        return;
                    }

                    if (isLeader == false && isSelfLeader == true) {
                        startSubscriber();
                        isLeader = isSelfLeader;
                        logger.warn("redis延迟队列, 本服务节点启动消费: " + leaderNode);
                        return;
                    }
                }
            }

        });
    }

    private void stopLeader() {
        if (leader != null) {
            try {
                leader.close();
            } catch (IOException e) {
            }
            leader = null;
        }
    }

    private void startSubscriber() {
        this.keyExpiredSubscriber = new AbstractSubscriber() {

            @Override
            public void onPMessage(String pattern, String channel, String message) {
                if (filter(message)) {
                    try {
                        if (!asyn) {
                            call2wait(message);
                        } else {
                            call2nowait(message);
                        }
                    } catch (Throwable e) {
                        logger.error("redis延迟队列回调业务处理器抛异常, key=" + message, e);
                    }
                }
            }

        };
        redisCacheClient.psubscribe(this.keyExpiredSubscriber, KEY_EXPIRED_PATTERN);
    }

    private void stopSubscriber() {
        if (this.keyExpiredSubscriber != null) {
            this.keyExpiredSubscriber.punsubscribe(KEY_EXPIRED_PATTERN);
            this.keyExpiredSubscriber = null;
            this.isLeader = false;
        }
    }

    private void createThreadPoolExecutor() {
        this.handleQueue = new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY);
        this.executorService = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, handleQueue,
                new NamedThreadFactory(this.leaderNode), new ThreadPoolExecutor.CallerRunsPolicy()) {

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (handleQueue.size() > QUEUE_CAPACITY - 1) {
                    logger.warn("redis延迟队列回调队列堵塞，size=" + handleQueue.size() + "，需优化业务处理速度或者配置更大的处理线程数");
                }
            }

        };
    }

    private boolean filter(String key) {
        return pattern == null || key.startsWith(pattern);
    }

    private void call2wait(String message) {
        message = removePrefix(message);
        this.delayHandle.entry(message);
        MonitorLogger.getInstance()
                .log("redis延迟队列: " + leaderNode + ", 处理完元素: " + message + ", 总共处理完： " + doneStat.incrementAndGet());
    }

    private void call2nowait(String message) {
        executorService.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    call2wait(message);
                } catch (Throwable e) {
                    logger.error("redis延迟队列回调业务处理器抛异常, key=" + message, e);
                }
            }

        });
    }

    /**
     * 将指定元素插入此延迟队列中
     * 
     * @param element
     * @param seconds 延迟处理时间，单位秒
     */
    public void add(String element, int seconds) {
        element = addPrefix(element);
        redisCacheClient.stringSetex(element, KEY_VALUE, seconds);
    }

    /**
     * 将指定元素插入此延迟队列中(只在键不存在时)
     * 
     * @param element
     * @param seconds 延迟处理时间，单位秒
     */
    public void addIfAbsent(String element, int seconds) {
        element = addPrefix(element);
        redisCacheClient.stringSet(element, KEY_VALUE, EnumExist.NX, EnumTime.EX, seconds);
    }

    /**
     * 将指定元素插入此延迟队列中(只在键不存在时)
     * 
     * @param element
     * @param seconds 延迟处理时间，单位秒
     */
    public Boolean addIfAbsent2(String element, int seconds) {
        element = addPrefix(element);
        return redisCacheClient.stringSet(element, KEY_VALUE, EnumExist.NX, EnumTime.EX, seconds);
    }

    /**
     * 从此队列中移除指定元素，无论它是否到期
     * 
     * @param element
     */
    public void remove(String element) {
        element = addPrefix(element);
        redisCacheClient.delete(element);
    }

    /**
     * 判断当前节点是否在处理
     * 
     * @return
     */
    public boolean isProcessed() {
        return isLeader;
    }

    /**
     * 停止消费，关闭资源
     */
    public synchronized void destory() {
        initialized = false;
        stopSubscriber();
        logger.warn("redis延迟队列, 本服务节点停止消费: " + leaderNode);
        stopLeader();
    }

    /**
     * 重启延迟队列，一般用在测试场景
     */
    public synchronized void restart() {
        destory();
        // 单节点消费
        if (!cluster) {
            startLeader();
        } else {
            startSubscriber();
        }
        initialized = true;
        logger.warn("redis延迟队列, 重启成功: " + leaderNode);
    }

}
