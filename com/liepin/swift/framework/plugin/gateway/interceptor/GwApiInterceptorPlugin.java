package com.liepin.swift.framework.plugin.gateway.interceptor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.gateway.api.spi.GwApiInterceptor;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.plugin.gateway.interceptor.chain.GwApiInterceptorProxy;

public class GwApiInterceptorPlugin implements IPlugin<List<GwApiInterceptorProxy>> {

    private static final Logger logger = Logger.getLogger(GwApiInterceptorPlugin.class);

    private List<GwApiInterceptorProxy> interceptors;

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("GwApiInterceptorPlugin init.");
        StringBuilder log = new StringBuilder();
        interceptors = new PluginScan<GwApiInterceptor>(applicationContext)
                .scanObjects(new GwApiInterceptorObjectFilter()).stream().map(g -> {
                    log.append("Added {" + g.getClass().getName()).append("} to GwApiInterceptor\n");
                    return new GwApiInterceptorProxy(g);
                }).sorted(new FilterComparator()).collect(Collectors.toList());
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        logger.info("GwApiInterceptorPlugin destroy.");
    }

    @Override
    public List<GwApiInterceptorProxy> getObject() {
        return Collections.unmodifiableList(interceptors);
    }

    private class FilterComparator implements Comparator<GwApiInterceptorProxy> {

        @Override
        public int compare(GwApiInterceptorProxy o1, GwApiInterceptorProxy o2) {
            if (o1.getOrder() == o2.getOrder()) {
                return 0;
            }
            return (o1.getOrder() > o2.getOrder()) ? 1 : -1;
        }

    }

    @Override
    public String name() {
        return "GW拦截器加载";
    }

}
