package com.liepin.swift.framework.plugin.gateway.interceptor.chain;

import java.util.List;
import java.util.Objects;

public class DefaultGwApiInterceptorChain implements GwApiInterceptorChain {

    private final List<GwApiInterceptorProxy> apiInterceptors;
    private final GwApiContext context;
    private int currentPosition = 0;
    private Object beforeObject;

    private DefaultGwApiInterceptorChain(List<GwApiInterceptorProxy> gwApiInterceptorProxies, GwApiContext context) {
        this.apiInterceptors = gwApiInterceptorProxies;
        this.context = context;
    }

    public static DefaultGwApiInterceptorChain newProcessorChain(List<GwApiInterceptorProxy> gwApiInterceptorProxies,
            GwApiContext context) {
        return new DefaultGwApiInterceptorChain(gwApiInterceptorProxies, context);
    }

    @Override
    public Object call() throws Exception {
        if (currentPosition != apiInterceptors.size()) {
            currentPosition++;
            GwApiInterceptorProxy proxy = apiInterceptors.get(currentPosition - 1);
            return proxy.process(this);
        } else {
            if (Objects.isNull(beforeObject)) {
                // api调用
                return context.invoke();
            } else {
                return beforeObject;
            }
        }
    }

    @Override
    public void setBeforeResult(Object obj) {
        this.beforeObject = obj;
    }

    @Override
    public Object getBeforeResult() {
        return this.beforeObject;
    }

    @Override
    public GwApiContext getContext() {
        return this.context;
    }

}
