package com.liepin.swift.framework.rpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.liepin.swift.framework.rpc.cache.RpcCache;

public class CacheServiceInterceptor implements InvocationHandler {

    private ServiceInterceptor serviceInterceptor;
    private RpcCache rpcCache;

    public CacheServiceInterceptor(ServiceInterceptor serviceInterceptor, RpcCache rpcCache) {
        this.serviceInterceptor = serviceInterceptor;
        this.rpcCache = rpcCache;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        if (rpcCache != null && rpcCache.useCache(method)) {
            result = rpcCache.getCache(method, args);
            if (result == null) {
                result = serviceInterceptor.invoke(proxy, method, args);
                if (result != null) {
                    rpcCache.setCache(method, args, result);
                }
            }
        } else {
            result = serviceInterceptor.invoke(proxy, method, args);
        }
        return result;
    }

    @Override
    public String toString() {
        return "clientCache|" + serviceInterceptor.toString();
    }

}
