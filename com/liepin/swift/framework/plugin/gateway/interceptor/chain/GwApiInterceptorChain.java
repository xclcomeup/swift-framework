package com.liepin.swift.framework.plugin.gateway.interceptor.chain;

public interface GwApiInterceptorChain {

    public Object call() throws Exception;

    public void setBeforeResult(Object obj);

    public Object getBeforeResult();

    public GwApiContext getContext();

}
