package com.liepin.swift.framework.rpc.limit;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.other.ReflectorUtil;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.util.CommonUtil;
import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeChildListener;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

public class HystrixConfigurationCurator {

    private static final Logger logger = Logger.getLogger(HystrixConfigurationCurator.class);
    private static final MonitorLogger monitorLog = MonitorLogger.getInstance();

    public static final String CONFIG_NODE_PATH = "/rpc/client/limit";

    private static final int MAXIMUM_TIMEOUT_THRESHOLD = 10 * 60 * 1000;// 10分钟

    public static final HystrixConfigurationCurator instance = new HystrixConfigurationCurator();

    // 默认全局限流熔断配置
    private volatile HystrixCommandProperties.Setter commandPropertiesDefaults = HystrixCommandProperties.Setter();

    // 自身项目自定义限流熔断配置，默认null
    private volatile LimitCustom myselfLimitCustom = null;

    // 引用项目自定义限流熔断配置，默认空
    // 数据格式：appName -> LimitCustom
    private ConcurrentMap<String, LimitCustom> commandPropertiesCustoms = new ConcurrentHashMap<String, LimitCustom>();

    // 项目接口运行时使用限流熔断配置，粒度：接口
    // 赖加载
    // 数据格式：appName -> interface -> HystrixCommand.Setter
    private ConcurrentMap<String, ConcurrentMap<String, HystrixCommand.Setter>> commandUsedSetters = new ConcurrentHashMap<String, ConcurrentMap<String, HystrixCommand.Setter>>();

    // 存不超时的接口列表
    private final Set<String> noTimeoutUris = Collections.synchronizedSet(new HashSet<String>());

    public static HystrixConfigurationCurator getInstance() {
        return instance;
    }

    private HystrixConfigurationCurator() {
        // 加载默认配置
        this.commandPropertiesDefaults = syncDefaultSetter();

        // 监听默认配置
        createListenerForDefaultSetter();

        // 加载自身自定义配置
        this.myselfLimitCustom = readCustomConfig(ProjectId.getProjectName());

        // 监听自定义配置
        createListenerForCustomSetter();
    }

    /**
     * 获取客户端调用熔断限流配置
     * 
     * @param appName 被调用方项目名
     * @param url 被调用方接口名
     * @param timeout 是否有超时, true:有超时 false:无超时
     * @return
     */
    public Setter getClientHystrixCommandSetter(String appName, String url, boolean timeout) {
        ConcurrentMap<String, Setter> map = commandUsedSetters.get(appName);
        if (map == null) {
            commandUsedSetters.put(appName, map = new ConcurrentHashMap<String, Setter>());
        }
        Setter setter = map.get(url);
        if (setter == null) {
            if (!timeout) {
                noTimeoutUris.add(appName + ":" + url);
            }
            map.put(url, setter = buildClientHystrixCommandSetter(ProjectId.getProjectName(), appName, url));
        }
        return setter;
    }

    private Setter buildClientHystrixCommandSetter(String projectName, String callAppName, String callUrl) {
        Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(callAppName))
                .andCommandKey(HystrixCommandKey.Factory.asKey(callUrl));
        HystrixCommandProperties.Setter clone = clone(commandPropertiesDefaults);

        LimitCustom callLimitCustom = commandPropertiesCustoms.get(callAppName);

        if (myselfLimitCustom == null || myselfLimitCustom.noConfigured()) {// 自身项目无自定义配置
            if (callLimitCustom != null && !callLimitCustom.noConfigured()) {// 对方项目有自定义配置
                Integer executionAllowMaxConcurrentRequests = callLimitCustom
                        .getExecutionAllowMaxConcurrentRequests(projectName, callUrl);
                if (executionAllowMaxConcurrentRequests != null) {
                    clone.withExecutionIsolationSemaphoreMaxConcurrentRequests(executionAllowMaxConcurrentRequests);
                }
            }
        } else {// 自身项目有自定义配置
            Integer executionCallTimeoutInMilliseconds = myselfLimitCustom
                    .getExecutionCallTimeoutInMilliseconds(callAppName, callUrl);
            if (executionCallTimeoutInMilliseconds != null) {
                clone.withExecutionTimeoutInMilliseconds(executionCallTimeoutInMilliseconds);
            }
            Integer circuitBreakerCallErrorThresholdPercentage = myselfLimitCustom
                    .getCircuitBreakerCallErrorThresholdPercentage(callAppName, callUrl);
            if (circuitBreakerCallErrorThresholdPercentage != null) {
                clone.withCircuitBreakerErrorThresholdPercentage(circuitBreakerCallErrorThresholdPercentage);
            }
            Integer circuitBreakerSleepWindowInMilliseconds = myselfLimitCustom
                    .getCircuitBreakerSleepWindowInMilliseconds();
            if (circuitBreakerSleepWindowInMilliseconds != null) {
                clone.withCircuitBreakerSleepWindowInMilliseconds(circuitBreakerSleepWindowInMilliseconds);
            }

            if (callLimitCustom != null && !callLimitCustom.noConfigured()) {// 对方项目有自定义配置
                Integer executionAllowMaxConcurrentRequests = callLimitCustom
                        .getExecutionAllowMaxConcurrentRequests(projectName, callUrl);
                if (executionAllowMaxConcurrentRequests != null) {
                    clone.withExecutionIsolationSemaphoreMaxConcurrentRequests(executionAllowMaxConcurrentRequests);
                }
            }
        }
        // 不超时接口单独设置
        if (noTimeoutUris.contains(callAppName + ":" + callUrl)) {
            // hystrix also uses this time to release
            // resources.(TimerReference(ScheduledFuture))
            clone.withExecutionTimeoutInMilliseconds(MAXIMUM_TIMEOUT_THRESHOLD);
        }

        setter.andCommandPropertiesDefaults(clone);
        return setter;
    }

    /**
     * 构建默认配置
     * 
     * @return
     */
    private HystrixCommandProperties.Setter syncDefaultSetter() {
        Map<String, String> config = readDefaultConfig();

        HystrixCommandProperties.Setter setter = HystrixCommandProperties.Setter();
        setter.withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE);
        setter.withExecutionTimeoutInMilliseconds(
                getValueOrDefault(config, LimitDefault.EXECUTION_TIMEOUTINMILLISECONDS));
        setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(
                getValueOrDefault(config, LimitDefault.EXECUTION_MAXCONCURRENTREQUESTS));
        setter.withFallbackIsolationSemaphoreMaxConcurrentRequests(
                getValueOrDefault(config, LimitDefault.FALLBACK_MAXCONCURRENTREQUESTS));
        setter.withCircuitBreakerRequestVolumeThreshold(
                getValueOrDefault(config, LimitDefault.CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD));
        setter.withCircuitBreakerSleepWindowInMilliseconds(
                getValueOrDefault(config, LimitDefault.CIRCUITBREAKER_SLEEPWINDOWINMILLISECONDS));
        setter.withCircuitBreakerErrorThresholdPercentage(
                getValueOrDefault(config, LimitDefault.CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE));
        setter.withMetricsRollingStatisticalWindowInMilliseconds(
                getValueOrDefault(config, LimitDefault.METRICS_ROLLINGSTATS_TIMEINMILLISECONDS));
        setter.withMetricsRollingStatisticalWindowBuckets(
                getValueOrDefault(config, LimitDefault.METRICS_ROLLINGSTATS_NUMBUCKETS));
        setter.withRequestCacheEnabled(false);

        if (setter.getMetricsRollingStatisticalWindowInMilliseconds()
                % setter.getMetricsRollingStatisticalWindowBuckets() != 0) {
            logger.warn("the configuration of zookeeper node: " + CONFIG_NODE_PATH
                    + " following must be true — “metrics.rollingStats.timeInMilliseconds % metrics.rollingStats.numBuckets==0” — otherwise it will throw an exception. now used default value");
            setter.withMetricsRollingStatisticalWindowInMilliseconds(
                    LimitDefault.METRICS_ROLLINGSTATS_TIMEINMILLISECONDS.getValue());
            setter.withMetricsRollingStatisticalWindowBuckets(LimitDefault.METRICS_ROLLINGSTATS_NUMBUCKETS.getValue());
        }
        return setter;
    }

    public void loadLimitCustom(String appName) {
        LimitCustom limitCustom = readCustomConfig(appName);
        if (limitCustom == null) {
            limitCustom = LimitCustom.newOne();
        }
        commandPropertiesCustoms.put(appName, limitCustom);
    }

    private int getValueOrDefault(final Map<String, String> config, LimitDefault limitDefault) {
        int value = limitDefault.getValue();
        if (config != null) {
            String string = config.get(limitDefault.getKey());
            if (string != null) {
                try {
                    value = Integer.parseInt(string);
                } catch (Exception e) {
                }
            }
        }
        return value;
    }

    private LimitCustom readCustomConfig(String appName) {
        String data = ZookeeperFactory.useServerZookeeperWithoutException().getString(EnumNamespace.PUBLIC,
                CONFIG_NODE_PATH + "/" + appName);
        if (data == null) {
            return null;
        }
        String[] lines = data.split("\r\n");
        Map<String, String> map = toMap(lines);
        if (map.isEmpty()) {
            return null;
        }
        LimitCustom limitCustom = LimitCustom.newOne();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            LimitCustom.classifying(entry, limitCustom);
        }
        return limitCustom;
    }

    private Map<String, String> readDefaultConfig() {
        String data = ZookeeperFactory.useServerZookeeperWithoutException().getString(EnumNamespace.PUBLIC,
                CONFIG_NODE_PATH);
        if (data == null) {
            return null;
        }
        String[] lines = data.split("\r\n");
        return toMap(lines);
    }

    private Map<String, String> toMap(String[] lines) {
        Map<String, String> configs = new LinkedHashMap<String, String>();
        for (String line : lines) {
            if (CommonUtil.ignore(line)) {
                continue;
            }
            String[] array = line.split("=");
            if (array.length != 2) {
                continue;
            }
            configs.put(array[0].trim(), array[1].trim());
        }
        return configs;
    }

    private void createListenerForDefaultSetter() {
        ZookeeperFactory.useServerZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + CONFIG_NODE_PATH;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                log("listen path=" + listeningPath() + " event=" + type);

                if (EnumChangedEvent.UPDATED == type) {
                    // 错开时间
                    StaggerTime.waited();
                    commandPropertiesDefaults = syncDefaultSetter();
                    // 触发刷新事件
                    refreshAllConfig();
                }
            }

        });
    }

    private void createListenerForCustomSetter() {
        ZookeeperFactory.useServerZookeeperWithoutException().addListener(new NewNodeChildListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + CONFIG_NODE_PATH;
            }

            @Override
            public void childChanged(IZookeeperClient zookeeperClient, String childName, EnumChangedEvent type) {
                log("listen path=" + listeningPath() + " childName=" + childName + " event=" + type);

                if (EnumChangedEvent.CHILD_ADDED == type) {
                    // 错开时间
                    StaggerTime.waited();
                    LimitCustom updated = readCustomConfig(childName);
                    if (updated == null || updated.noConfigured()) {
                        // 新增一个空配置忽略
                        return;
                    }
                    refreshConfig(childName, updated);
                } else if (EnumChangedEvent.CHILD_UPDATED == type) {
                    LimitCustom updated = readCustomConfig(childName);
                    if (updated == null) {
                        return;
                    }
                    refreshConfig(childName, (updated.noConfigured()) ? null : updated);
                } else if (EnumChangedEvent.CHILD_REMOVED == type) {
                    refreshConfig(childName, null);
                }

            }

        });
    }

    /**
     * 重新生成已加载的Setter配置
     */
    private synchronized void refreshAllConfig() {
        ConcurrentMap<String, ConcurrentMap<String, HystrixCommand.Setter>> tmp = new ConcurrentHashMap<String, ConcurrentMap<String, HystrixCommand.Setter>>(
                commandUsedSetters);
        for (Map.Entry<String, ConcurrentMap<String, HystrixCommand.Setter>> entry1 : tmp.entrySet()) {
            String appName = entry1.getKey();
            ConcurrentMap<String, HystrixCommand.Setter> map = entry1.getValue();
            if (map != null) {
                for (Map.Entry<String, HystrixCommand.Setter> entry2 : map.entrySet()) {
                    String url = entry2.getKey();
                    Setter nowSetter = buildClientHystrixCommandSetter(ProjectId.getProjectName(), appName, url);
                    // 更新
                    commandUsedSetters.get(appName).put(url, nowSetter);
                }
            }
        }

    }

    /**
     * @param appName
     * @param updated 为null标示删除
     */
    private synchronized void refreshConfig(String appName, LimitCustom updated) {
        // 判断是否自己项目
        if (ProjectId.getProjectName().equals(appName)) {
            myselfLimitCustom = updated;
            refreshAllConfig();
        } else {
            // 判断是否引用的项目
            if (!commandPropertiesCustoms.containsKey(appName)) {
                // 非引用项目，忽略
                return;
            }
            if (updated != null) {
                commandPropertiesCustoms.put(appName, updated);
            } else {
                commandPropertiesCustoms.put(appName, LimitCustom.newOne());
            }
            ConcurrentMap<String, Setter> current = commandUsedSetters.get(appName);
            if (current != null) {
                Set<String> urls = new HashSet<String>(current.keySet());
                for (String url : urls) {
                    Setter nowSetter = buildClientHystrixCommandSetter(ProjectId.getProjectName(), appName, url);
                    current.put(url, nowSetter);
                }
            }
        }
    }

    private static class LimitCustom {
        /**
         * 调用对方超时时间<br>
         * 模板格式：execution.call.{appName}.{url}.timeoutInMilliseconds
         * <p>
         * 默认全局配置：execution.call.timeoutInMilliseconds<br>
         * 粒度到项目：execution.call.{appName}.timeoutInMilliseconds，
         * 配置调用某一个项目所有接口的超时时间<br>
         * 粒度到接口：execution.call.{appName}.{url}.timeoutInMilliseconds，
         * 配置调用某一个项目某一个接口的超时时间<br>
         * 错误的配置：execution.call.{url}.timeoutInMilliseconds<br>
         * <p>
         * 同时存在的时候优先级：粒度到接口 -> 粒度到项目
         */
        Map<String, Integer> executionCallTimeoutInMilliseconds = new LinkedHashMap<String, Integer>();
        /**
         * 允许对方调用最大并发数<br>
         * 模板格式：execution.allow.{appName}.{url}.maxConcurrentRequests
         * <p>
         * 默认全局配置：execution.allow.maxConcurrentRequests<br>
         * 粒度到项目：
         * execution.allow.{appName}.maxConcurrentRequests，配置限额某一个项目调自己最大并发数<br>
         * 粒度到项目接口：execution.allow.{appName}.{url}.maxConcurrentRequests，
         * 配置限额某一个项目调自己某一个接口最大并发数<br>
         * 粒度到接口：
         * execution.allow.{url}.maxConcurrentRequests，配置限额所有项目调自己某一个接口最大并发数<br>
         * <p>
         * 同时存在的时候优先级：粒度到项目接口 -> 粒度到接口 -> 粒度到项目
         */
        Map<String, Integer> executionAllowMaxConcurrentRequests = new LinkedHashMap<String, Integer>();
        /**
         * 当调用对方出错率超过n%后熔断器启动<br>
         * 模板格式：circuitBreaker.call.{appName}.{url}.errorThresholdPercentage
         * <p>
         * 默认配置：circuitBreaker.call.errorThresholdPercentage<br>
         * 粒度到项目： circuitBreaker.call.{appName}.errorThresholdPercentage，
         * 配置调某一个项目所有接口的错误率<br>
         * 粒度到项目接口：circuitBreaker.call.{appName}.{url}.errorThresholdPercentage，
         * 配置调某一个项目某一个接口的错误率<br>
         * 错误的配置： circuitBreaker.call.{url}.errorThresholdPercentage<br>
         * <p>
         * 同时存在的时候优先级：粒度到项目接口 -> 粒度到项目
         */
        Map<String, Integer> circuitBreakerCallErrorThresholdPercentage = new LinkedHashMap<String, Integer>();
        /**
         * 熔断器默认工作时间
         */
        int circuitBreakerSleepWindowInMilliseconds = -1;

        static LimitCustom newOne() {
            return new LimitCustom();
        }

        static void classifying(final Map.Entry<String, String> entry, final LimitCustom limitCustom) {
            int value;
            try {
                value = Integer.parseInt(entry.getValue());
            } catch (NumberFormatException e) {
                return;
            }

            if (entry.getKey().startsWith("execution.call.")) {
                limitCustom.executionCallTimeoutInMilliseconds.put(entry.getKey(), value);
            }
            if (entry.getKey().startsWith("execution.allow.")) {
                limitCustom.executionAllowMaxConcurrentRequests.put(entry.getKey(), value);
            }
            if (entry.getKey().startsWith("circuitBreaker.call.")) {
                limitCustom.circuitBreakerCallErrorThresholdPercentage.put(entry.getKey(), value);
            }
            if (entry.getKey().equals("circuitBreaker.sleepWindowInMilliseconds")) {
                limitCustom.circuitBreakerSleepWindowInMilliseconds = value;
            }
        }

        public boolean noConfigured() {
            return circuitBreakerSleepWindowInMilliseconds == -1 && executionCallTimeoutInMilliseconds.isEmpty()
                    && executionAllowMaxConcurrentRequests.isEmpty()
                    && circuitBreakerCallErrorThresholdPercentage.isEmpty();
        }

        public Integer getExecutionAllowMaxConcurrentRequests(String appName, String url) {
            if (executionAllowMaxConcurrentRequests.isEmpty()) {
                return null;
            }
            String key = "execution.allow." + appName + "." + url + ".maxConcurrentRequests";
            Integer value = executionAllowMaxConcurrentRequests.get(key);
            if (value != null) {
                return value;
            }
            key = "execution.allow." + url + ".maxConcurrentRequests";
            value = executionAllowMaxConcurrentRequests.get(key);
            if (value != null) {
                return value;
            }
            key = "execution.allow." + appName + ".maxConcurrentRequests";
            value = executionAllowMaxConcurrentRequests.get(key);
            if (value != null) {
                return value;
            }
            key = "execution.allow.maxConcurrentRequests";
            value = executionAllowMaxConcurrentRequests.get(key);
            if (value != null) {
                return value;
            }
            return null;
        }

        public Integer getExecutionCallTimeoutInMilliseconds(String appName, String url) {
            if (executionCallTimeoutInMilliseconds.isEmpty()) {
                return null;
            }
            String key = "execution.call." + appName + "." + url + ".timeoutInMilliseconds";
            Integer value = executionCallTimeoutInMilliseconds.get(key);
            if (value != null) {
                return value;
            }
            key = "execution.call." + appName + ".timeoutInMilliseconds";
            value = executionCallTimeoutInMilliseconds.get(key);
            if (value != null) {
                return value;
            }
            key = "execution.call.timeoutInMilliseconds";
            value = executionCallTimeoutInMilliseconds.get(key);
            if (value != null) {
                return value;
            }
            return null;
        }

        public Integer getCircuitBreakerCallErrorThresholdPercentage(String appName, String url) {
            if (circuitBreakerCallErrorThresholdPercentage.isEmpty()) {
                return null;
            }
            String key = "circuitBreaker.call." + appName + "." + url + ".errorThresholdPercentage";
            Integer value = circuitBreakerCallErrorThresholdPercentage.get(key);
            if (value != null) {
                return value;
            }
            key = "circuitBreaker.call." + appName + ".errorThresholdPercentage";
            value = circuitBreakerCallErrorThresholdPercentage.get(key);
            if (value != null) {
                return value;
            }
            key = "circuitBreaker.call.errorThresholdPercentage";
            value = circuitBreakerCallErrorThresholdPercentage.get(key);
            if (value != null) {
                return value;
            }
            return null;
        }

        public Integer getCircuitBreakerSleepWindowInMilliseconds() {
            return (circuitBreakerSleepWindowInMilliseconds != -1) ? circuitBreakerSleepWindowInMilliseconds : null;
        }

        @SuppressWarnings("unused")
        public Map<String, Integer> getExecutionCallTimeoutInMilliseconds() {
            return executionCallTimeoutInMilliseconds;
        }

        @SuppressWarnings("unused")
        public Map<String, Integer> getExecutionAllowMaxConcurrentRequests() {
            return executionAllowMaxConcurrentRequests;
        }

        @SuppressWarnings("unused")
        public Map<String, Integer> getCircuitBreakerCallErrorThresholdPercentage() {
            return circuitBreakerCallErrorThresholdPercentage;
        }

        @Override
        public String toString() {
            return "LimitCustom [executionCallTimeoutInMilliseconds=" + executionCallTimeoutInMilliseconds
                    + ", executionAllowMaxConcurrentRequests=" + executionAllowMaxConcurrentRequests
                    + ", circuitBreakerCallErrorThresholdPercentage=" + circuitBreakerCallErrorThresholdPercentage
                    + ", circuitBreakerSleepWindowInMilliseconds=" + circuitBreakerSleepWindowInMilliseconds + "]";
        }

    }

    private enum LimitDefault {
        EXECUTION_TIMEOUTINMILLISECONDS("execution.timeoutInMilliseconds", 3100), // 超时时间，单位毫秒
        EXECUTION_MAXCONCURRENTREQUESTS("execution.maxConcurrentRequests", 300), // 调用最大并发数，粒度：接口
        FALLBACK_MAXCONCURRENTREQUESTS("fallback.maxConcurrentRequests", 150), // 降级调用最大并发数
        CIRCUITBREAKER_REQUESTVOLUMETHRESHOLD("circuitBreaker.requestVolumeThreshold", 20), // 熔断器在整个统计时间内是否开启的阀值。配置20表示10秒钟内至少请求20次，熔断器才发挥起作用
        CIRCUITBREAKER_SLEEPWINDOWINMILLISECONDS("circuitBreaker.sleepWindowInMilliseconds", 5000), // 熔断器默认工作时间，单位毫秒。熔断器中断请求n秒后会进入半打开状态，放部分流量过去重试
        CIRCUITBREAKER_ERRORTHRESHOLDPERCENTAGE("circuitBreaker.errorThresholdPercentage", 80), // 当出错率超过n%后熔断器启动，单位百分比
        METRICS_ROLLINGSTATS_TIMEINMILLISECONDS("metrics.rollingStats.timeInMilliseconds", 10000), // 统计滚动时间窗口，单位毫秒，
                                                                                                   // 默认10000毫秒
        METRICS_ROLLINGSTATS_NUMBUCKETS("metrics.rollingStats.numBuckets", 10); // 统计窗口的buckets数量，默认10

        private String key;
        private int value;

        private LimitDefault(String key, int value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

    }

    private void log(String message) {
        monitorLog.log("[HystrixConfigurationCurator] " + message);
    }

    private HystrixCommandProperties.Setter clone(final HystrixCommandProperties.Setter source) {
        HystrixCommandProperties.Setter target = HystrixCommandProperties.Setter();
        ReflectorUtil.clone(target, source);
        return target;
    }

    public String debug() {
        StringBuilder log = new StringBuilder();
        log.append("commandPropertiesDefaults: ").append(JsonUtil.toJson(commandPropertiesDefaults)).append("\r\n");
        log.append("myselfLimitCustom: ");
        if (myselfLimitCustom != null) {
            log.append(JsonUtil.toJson(myselfLimitCustom));
        } else {
            log.append("null");
        }
        log.append("\r\n");
        log.append("commandPropertiesCustoms: ").append(JsonUtil.toJson(commandPropertiesCustoms)).append("\r\n");
        log.append("commandUsedSetters: ").append("\r\n");
        for (Map.Entry<String, ConcurrentMap<String, HystrixCommand.Setter>> entry : commandUsedSetters.entrySet()) {
            for (Map.Entry<String, HystrixCommand.Setter> entry1 : entry.getValue().entrySet()) {
                log.append(entry.getKey() + ":" + entry1.getKey() + " ").append(entry1.getValue()).append("\r\n");
            }
        }
        return log.toString();
    }

}
