package com.liepin.swift.framework.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.other.NamedThreadFactory;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.Log4jUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public abstract class AbstractLogPlugin implements ILogPlugin {

    protected static final int DEFAULT_PERIOD = 60;// 秒

    protected ScheduledExecutorService heartbeat;
    protected int period = DEFAULT_PERIOD;

    private final Logger logger;
    private volatile boolean enable;
    private Random random = new Random();

    public AbstractLogPlugin() {
        this.logger = Log4jUtil.register2(category(), category());
        createListener();
    }

    /**
     * 类型
     * 
     * @return
     */
    public abstract String category();

    /**
     * zk上的配置路径
     * 
     * @return
     */
    public abstract String zkListenPath();

    /**
     * 是否定时采集
     * 
     * @return
     */
    public abstract boolean timer();

    /**
     * son class cover
     */
    public void onEvent() {
        // default do nothing
    }

    protected void log(String message) {
        if (message != null) {
            logger.info(message);
        }
    }

    protected Map<String, Object> readFromZk() {
        return ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC, zkListenPath());
    }

    protected int getPeriod(Map<String, Object> map, int defaultValue) {
        Integer period = (Integer) map.get("period");
        return (period != null) ? period.intValue() : defaultValue;
    }

    protected boolean getEnable(Map<String, Object> map, boolean defaultValue) {
        Boolean enable = (Boolean) map.get("enable");
        return (enable != null) ? enable.booleanValue() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected boolean getEnable(Map<String, Object> map) {
        boolean enable = getEnable(map, true);
        List<String> projects = (List<String>) map.get("disableProjects");
        if (projects == null) {
            projects = new ArrayList<String>();
        }
        return enable && !projects.contains(ProjectId.getProjectName());
    }

    /**
     * 根据配置触发事件
     * 
     * @param map
     */
    protected void handle(Map<String, Object> map) {
        boolean enable = getEnable(map);
        if (enable) {
            if (timer()) {
                refreshTimer(getPeriod(map, DEFAULT_PERIOD));
            }
            setEnable(enable);
        } else {
            uninstall();
        }
    }

    protected void refreshTimer(int period) {
        boolean reload = (heartbeat == null) ? true : (this.period != period ? true : false);
        if (!reload) {
            return;
        }
        closeTimer();

        heartbeat = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("Swift-Monitor-" + category(), true));
        heartbeat.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    onEvent();
                } catch (Exception e) {
                    MonitorLogger.getInstance().log(category() + "监控模块执行失败: " + e.getMessage(), e);
                }
            }
        }, random.nextInt(10), period, TimeUnit.SECONDS);
        this.period = period;
    }

    protected void closeTimer() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
    }

    @Override
    public void install() {
        Map<String, Object> map = readFromZk();
        if (!SwiftConfig.enableZookeeper() && Objects.isNull(map)) {
            // 默认配置
            map = new HashMap<>();
        }
        if (map != null) {
            handle(map);
        }
    }

    @Override
    public void uninstall() {
        setEnable(false);
        if (timer()) {
            closeTimer();
        }
    }

    private void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + zkListenPath();
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type) {
                    try {
                        Map<String, Object> map = readFromZk();
                        if (map == null) {
                            return;
                        }
                        // 错开时间
                        StaggerTime.waited();
                        handle(map);
                    } catch (Exception e) {
                        MonitorLogger.getInstance().log("监控模块读取zk配置执行失败, path=" + listeningPath(), e);
                    }
                }
            }
        });
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isMac() {
        return System.getProperty("os.name").startsWith("Mac OS X");
    }

}
