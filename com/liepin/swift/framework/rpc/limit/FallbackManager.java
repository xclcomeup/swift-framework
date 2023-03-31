package com.liepin.swift.framework.rpc.limit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.liepin.swift.core.annotation.SwiftInterface;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherBean;

public class FallbackManager {

    private static final ConcurrentMap<String, DispatcherBean> fallbackContext = new ConcurrentHashMap<String, DispatcherBean>();

    /**
     * 添加降级方法
     * 
     * @param service
     * @return
     */
    public static List<String> addFallbackService(Object service) {
        List<String> uris = new ArrayList<String>();
        Class<?> clazz = service.getClass();
        if (!clazz.getSimpleName().endsWith("Fallback")) {
            return null;
        }
        String serviceName = clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf("Fallback"));
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                String uri = serviceName + "/" + method.getName();
                SwiftInterface swiftInterface = method.getAnnotation(SwiftInterface.class);
                if (swiftInterface != null && !"".equals(swiftInterface.uri())) {
                    uri = serviceName + "/" + swiftInterface.uri();
                }
                DispatcherBean context = new DispatcherBean();
                context.target = service;
                context.method = method;
                fallbackContext.put(uri, context);
                uris.add(uri);
            }
        }
        return uris;
    }

    /**
     * 根据请求地址获取是否有降级方法<br>
     * 如：IUserService/getUserByUserId<br>
     * 
     * @param uri
     * @return
     */
    public static DispatcherBean getFallbackService(String uri) {
        return fallbackContext.get(uri);
    }

}
