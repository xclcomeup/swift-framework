package com.liepin.swift.framework.plugin.schedule;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.client.schedule.annotation.DecoratorScheduler;
import com.liepin.client.schedule.annotation.SwiftScheduler;
import com.liepin.client.schedule.binding.Scheduler;
import com.liepin.client.schedule.binding.SchedulerBindingFactory;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginCutPoing;
import com.liepin.swift.framework.plugin.PluginScan;

public class SchedulePlugin implements IPlugin<SchedulerBindingFactory> {

    private static final Logger logger = Logger.getLogger(SchedulePlugin.class);

    private SchedulerBindingFactory factory;

    private List<Scheduler> schedulers = new ArrayList<Scheduler>();

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("SchedulePlugin init.");
        List<Scheduler> list = new PluginScan<Scheduler>(applicationContext).scanObjects(new ScheduleObjectFilter());
        List<PluginCutPoing> list2 = new PluginScan<PluginCutPoing>(applicationContext)
                .scanMethods(new ScheduleMethodFilter());
        if (list.isEmpty() && list2.isEmpty()) {
            return;
        }
        StringBuilder log = new StringBuilder();
        list.forEach(s -> {
            schedulers.add(s);
            log.append("Added {" + s.getClass().getName()).append("} to Scheduler from interface\n");
        });

        list2.forEach(t -> {
            DecoratorScheduler decoratorScheduler = toDecoratorScheduler(t);
            schedulers.add((Scheduler) decoratorScheduler);
            log.append("Added {" + t.getClazz().getName()).append("} to Scheduler from annotation\n");
        });
        this.factory = new SchedulerBindingFactory();
        this.factory.setSchedulers(schedulers);
        logger.info(log.toString());
    }

    public void start() {
        if (this.factory != null) {
            if (SwiftConfig.enableStartupPreload()) {
                this.factory.init();
            } else {
                new Thread(() -> {
                    this.factory.init();
                }).start();
            }
        }
    }

    @Override
    public void destroy() {
        if (this.factory != null) {
            this.factory.shutdown();
            this.factory = null;
            logger.info("SchedulePlugin destroy.");
        }
        this.schedulers.clear();
    }

    @Override
    public SchedulerBindingFactory getObject() {
        return this.factory;
    }

    public DecoratorScheduler toDecoratorScheduler(PluginCutPoing describe) {
        SwiftScheduler annotation = describe.getMethod().getAnnotation(SwiftScheduler.class);
        return new DecoratorScheduler(annotation.taskKey(), describe.getInstance(), describe.getMethod());
    }

    @Override
    public String name() {
        return "调度加载";
    }

}
