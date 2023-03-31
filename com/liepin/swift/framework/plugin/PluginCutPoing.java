package com.liepin.swift.framework.plugin;

import java.lang.reflect.Method;

public class PluginCutPoing {

    private Class<?> clazz;
    private Object instance;
    private Method method;

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "PluginCutPoing [clazz=" + clazz + ", instance=" + instance + ", method=" + method + "]";
    }

}
