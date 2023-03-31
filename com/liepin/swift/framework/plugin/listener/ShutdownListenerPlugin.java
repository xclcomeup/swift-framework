package com.liepin.swift.framework.plugin.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class ShutdownListenerPlugin implements IPlugin<List<IShutdownListener>> {

    private static final Logger logger = Logger.getLogger(ShutdownListenerPlugin.class);

    private final List<IShutdownListener> listeners = Collections.synchronizedList(new ArrayList<IShutdownListener>());

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ShutdownListenerPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<IShutdownListener>(applicationContext).scanObjects(new ShutdownListenerObjectFilter())
                .forEach(s -> {
                    listeners.add(s);
                    log.append("Added {" + s.getClass().getName()).append("} to IShutdownListener\n");
                });
        Collections.sort(listeners, new FilterComparator());
        logger.info(log.toString());
    }

    @Override
    public synchronized void destroy() {
        LinkedHashMap<Integer, List<IShutdownListener>> sortMap = new LinkedHashMap<>();
        for (IShutdownListener listener : listeners) {
            List<IShutdownListener> list = sortMap.get(listener.priority());
            if (list == null) {
                sortMap.put(listener.priority(), list = new ArrayList<>());
            }
            list.add(listener);
        }
        // 同一优先级的并行处理
        for (Map.Entry<Integer, List<IShutdownListener>> entry : sortMap.entrySet()) {
            List<IShutdownListener> list = entry.getValue();
            CountDownLatch latch = new CountDownLatch(list.size());
            int second = 5;
            for (IShutdownListener listener : list) {
                second = Math.max(listener.awaitSecond(), second);
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        try {
                            listener.onApplicationEvent();
                        } catch (Exception e) {
                            logger.error("停止之前回调事件执行失败: " + listener.getClass(), e);
                        } finally {
                            latch.countDown();
                        }
                    }

                });
                t.setDaemon(true);
                t.start();
            }
            try {
                latch.await(second, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
        listeners.clear();
        logger.info("ShutdownListenerPlugin destroy.");
    }

    @Override
    public List<IShutdownListener> getObject() {
        return Collections.unmodifiableList(listeners);
    }

    private static class FilterComparator implements Comparator<IShutdownListener> {

        @Override
        public int compare(IShutdownListener o1, IShutdownListener o2) {
            if (o1.priority() == o2.priority()) {
                return 0;
            }
            return (o1.priority() > o2.priority()) ? -1 : 1;
        }

    }

    @Override
    public String name() {
        return "停止之前回调事件加载";
    }

}
