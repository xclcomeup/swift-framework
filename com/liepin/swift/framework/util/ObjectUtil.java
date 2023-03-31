package com.liepin.swift.framework.util;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

public class ObjectUtil {

    public static boolean isAopProxy(Object object) {
        return (Proxy.isProxyClass(object.getClass()) || ClassUtils.isCglibProxyClass(object.getClass()));
    }

    public static boolean isJdkDynamicProxy(Object object) {
        return Proxy.isProxyClass(object.getClass());
    }

    public static boolean isCglibProxy(Object object) {
        return ClassUtils.isCglibProxy(object);
    }

    public static Object getActual(Object object) {
        Object actual = object;
        try {
            if (AopUtils.isAopProxy(actual)) {
                if (AopUtils.isCglibProxy(actual)) {
                    actual = getCglibProxyTargetObject(actual);
                } else if (AopUtils.isJdkDynamicProxy(actual)) {
                    actual = getJdkDynamicProxyTargetObject(actual);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(object.getClass() + "获取代理类的原始类失败: " + e.getMessage(), e);
        }
        return actual;
    }

    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
    }

}
