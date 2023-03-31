package com.liepin.swift.framework.mvc.eventInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.datastructure.ThreadLocalMessageFormat;
import com.liepin.common.other.NamedThreadFactory;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.Log4jUtil;
import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class FullJournal {

    private static final Logger logger = Log4jUtil.register2("fulljournal", "fulljournal");

    // 全链路日志定时全量采集
    private static final String FJ_NODE_NAME = "common/log/fullJournal";
    private volatile boolean enable = false;
    private volatile int period = 3600;// 单位秒
    private Random random = new Random();
    private ScheduledExecutorService heartbeat;
    private volatile Set<String> disableProjects = new HashSet<>();
    private static final Object VAL = new Object();
    private static final int SIZE = 200;
    private final ConcurrentHashMap<String, Object> judge = new ConcurrentHashMap<>(SIZE);

    private static final int BUFFERED_SIZE = 1000;
    private ArrayBlockingQueue<String> buffered = new ArrayBlockingQueue<String>(BUFFERED_SIZE);
    private final AtomicBoolean fuse = new AtomicBoolean(false);

    private static final String pattern = "servletPath={0}, input={1}, output={2}";
    private static final ThreadLocalMessageFormat messageFormat = new ThreadLocalMessageFormat(pattern);

    public FullJournal() {
        load();
        createListener();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                FJ_NODE_NAME);
        if (data != null && !data.isEmpty()) {
            Boolean enableObj = (Boolean) data.get("enable");
            this.enable = (enableObj != null) ? enableObj.booleanValue() : false;
            List<String> list = (List<String>) data.get("disableProjects");
            if (list != null && list.size() > 0) {
                this.disableProjects = new HashSet<>(list);
            }
            Integer periodObj = (Integer) data.get("period");
            int periodNow = (periodObj != null) ? periodObj.intValue() : 3600;
            refreshTimer(periodNow);
        }
    }

    /**
     * 监听节点变化
     */
    private void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/" + FJ_NODE_NAME;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                // 错开时间
                StaggerTime.waited();
                load();
            }

        });
    }

    private void refreshTimer(int period) {
        if (!this.enable || disableProjects.contains(ProjectId.getProjectName())) {
            closeTimer();
            onEvent();
            return;
        }

        boolean reload = (heartbeat == null) ? true : (this.period != period ? true : false);
        if (!reload) {
            return;
        }
        closeTimer();

        heartbeat = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Swift-FullJournal", true));
        heartbeat.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    onEvent();
                } catch (Exception e) {
                    MonitorLogger.getInstance().log("FullJournal listen event invoke fail for: " + e.getMessage(), e);
                }
            }
        }, random.nextInt(10), period, TimeUnit.SECONDS);
        this.period = period;
    }

    private void closeTimer() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
    }

    private void onEvent() {
        fuse.compareAndSet(false, true);
        List<String> tmp = new ArrayList<String>(buffered);
        buffered.clear();
        fuse.compareAndSet(true, false);

        tmp.forEach(t -> log(t));
        tmp.clear();

        this.judge.clear();
    }

    /**
     * @param eventInfo
     */
    public void log(final Event eventInfo) {
        if (!enable) {
            return;
        }
        if (disableProjects.contains(ProjectId.getProjectName())) {
            return;
        }
        if (judge.put(eventInfo.getActionPath(), VAL) != null) {
            return;
        }

        try {
            String message = messageFormat.get().format(new Object[] { eventInfo.getActionPath(),
                    EscapeText.confuseChars(eventInfo.getInput()), EscapeText.confuseChars(eventInfo.getOutput()) });
            if (fuse.get() || !buffered.offer(message)) {
                log(message);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void log(String message) {
        if (message != null) {
            logger.info(message);
        }
    }

}
