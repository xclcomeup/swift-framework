package com.liepin.swift.framework.mvc.dispatcher;

import java.lang.reflect.Method;

public class DispatcherBean {

    public Object target;
    public Method method;

    public Object invoke(Object... args) throws Exception {
        return method.invoke(target, args);
    }

}
