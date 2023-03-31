package com.liepin.swift.framework.plugin.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.common.other.NamedThreadFactory;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class AfterListenerPlugin implements IPlugin<List<IAfterListener>> {

    private static final Logger logger = Logger.getLogger(AfterListenerPlugin.class);

    private final List<IAfterListener> listeners = new ArrayList<IAfterListener>();
    private final List<IAfterListener> syncAfterListeners = new ArrayList<IAfterListener>();// 同步
    private final List<IAsynAfterListener> asynAfterListeners = new ArrayList<IAsynAfterListener>();// 异步
    private final List<ITimingAfterListener> timingAfterListeners = new ArrayList<ITimingAfterListener>();// 定时

    private ScheduledExecutorService scheduleExecutor;

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("AfterListenerPlugin init.");
        StringBuilder asynAfterListenerLog = new StringBuilder();
        StringBuilder timingAfterListenerLog = new StringBuilder();
        StringBuilder syncAfterListenerLog = new StringBuilder();
        new PluginScan<IAfterListener>(applicationContext).scanObjects(new AfterListenerObjectFilter()).forEach(a -> {
            if (a instanceof IAsynAfterListener) {
                asynAfterListeners.add((IAsynAfterListener) a);
                asynAfterListenerLog.append("Added {" + a.getClass().getName()).append("} to IAsynAfterListener\n");
            } else if (a instanceof ITimingAfterListener) {
                timingAfterListeners.add((ITimingAfterListener) a);
                timingAfterListenerLog.append("Added {" + a.getClass().getName()).append("} to ITimingAfterListener\n");
            } else {
                syncAfterListeners.add(a);
                syncAfterListenerLog.append("Added {" + a.getClass().getName()).append("} to IAfterListener\n");
            }
            listeners.add(a);
        });
        logger.info(asynAfterListenerLog.toString());
        logger.info(timingAfterListenerLog.toString());
        logger.info(syncAfterListenerLog.toString());
    }

    public void start() {
        // 执行同步
        for (IAfterListener afterListener : syncAfterListeners) {
            afterListener.onApplicationEvent();
        }

        // 执行异步
        for (final IAsynAfterListener asynAfterListener : asynAfterListeners) {
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        asynAfterListener.onApplicationEvent();
                    } catch (Exception e) {
                        logger.error("启动结束异步回调事件执行失败: " + asynAfterListener.getClass(), e);
                    }
                }

            }, "Swift-Listener-AsynAfterListener" + asynAfterListener.getClass());
            thread.setDaemon(true);
            thread.start();
        }

        // 执行定时
        Random random = new Random();
        if (timingAfterListeners.size() > 0) {
            this.scheduleExecutor = Executors.newScheduledThreadPool(timingAfterListeners.size(),
                    new NamedThreadFactory("Swift-Listener-TimingAfterListener"));
            for (final ITimingAfterListener timingAfterListener : timingAfterListeners) {
                scheduleExecutor.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            timingAfterListener.onApplicationEvent();
                        } catch (Exception e) {
                            logger.error("启动结束定时回调事件执行失败: " + timingAfterListener.getClass(), e);
                        }
                    }

                }, random.nextInt(5), timingAfterListener.period(), timingAfterListener.timeUnit());
            }
        }
    }

    @Override
    public void destroy() {
        if (scheduleExecutor != null) {
            scheduleExecutor.shutdownNow();
        }
        listeners.clear();
        logger.info("AfterListenerPlugin destroy.");
    }

    @Override
    public List<IAfterListener> getObject() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public String name() {
        return "启动结束回调事件加载";
    }

}
