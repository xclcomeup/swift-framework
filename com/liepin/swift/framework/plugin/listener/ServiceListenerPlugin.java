package com.liepin.swift.framework.plugin.listener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.router.discovery.IServiceFlowListener;
import com.liepin.router.discovery.ServiceDiscovery;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class ServiceListenerPlugin implements IPlugin<Set<IServiceFlowListener>> {

    private static final Logger logger = Logger.getLogger(ServiceListenerPlugin.class);

    private final Set<IServiceFlowListener> listeners = Collections
            .synchronizedSet(new HashSet<IServiceFlowListener>());

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ServiceListenerPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<IServiceFlowListener>(applicationContext).scanObjects(new ServiceFlowListenerObjectFilter())
                .forEach(s -> {
                    listeners.add(s);
                    ServiceDiscovery.getInstance().createServiceFlowListener(s);
                    log.append("Added {" + s.getClass().getName()).append("} to IServiceFlowListener\n");
                });
        logger.info(log.toString());
    }

    @Override
    public synchronized void destroy() {
        listeners.clear();
        logger.info("ServiceListenerPlugin destroy.");
    }

    @Override
    public Set<IServiceFlowListener> getObject() {
        return Collections.unmodifiableSet(listeners);
    }

    @Override
    public String name() {
        return "服务流量监听事件加载";
    }

}
