package com.liepin.swift.framework.plugin.gateway.interceptor.chain;

import java.lang.reflect.Method;

import com.liepin.swift.framework.mvc.dispatcher.DispatcherBean;

public class GwApiContext {

    private Object target;
    private Object[] params;
    private Method method;

    public GwApiContext(DispatcherBean bean, Object[] params) {
        this.target = bean.target;
        this.method = bean.method;
        this.params = params;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object invoke() throws Exception {
        return method.invoke(target, params);
    }

}
