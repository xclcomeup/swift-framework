package com.liepin.swift.framework.plugin.gateway.interceptor.chain;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

import com.liepin.gateway.api.spi.GwApiInterceptor;

public class GwApiInterceptorProxy {

    private final GwApiInterceptor gwApiInterceptor;

    public GwApiInterceptorProxy(GwApiInterceptor gwApiInterceptor) {
        this.gwApiInterceptor = gwApiInterceptor;
    }

    public Object process(final GwApiInterceptorChain chain) throws Exception {
        GwApiContext context = chain.getContext();
        try {
            if (Objects.isNull(chain.getBeforeResult())) {
                Optional.ofNullable(
                        gwApiInterceptor.before(context.getMethod(), context.getParams(), context.getTarget()))
                        .ifPresent(chain::setBeforeResult);
            }
            Object result = chain.call();
            gwApiInterceptor.afterReturning(result, context.getMethod(), context.getParams(), context.getTarget());
            return result;
        } catch (Exception e) {
            Throwable realThrowable = e;
            if (e instanceof InvocationTargetException) {
                realThrowable = ((InvocationTargetException)e).getTargetException();
            }
            gwApiInterceptor.afterThrowing(context.getMethod(), context.getParams(), context.getTarget(), realThrowable);
            throw e;
        }
    }

    @Override
    public String toString() {
        return gwApiInterceptor.getClass().getSimpleName();
    }

    public int getOrder() {
        return gwApiInterceptor.getOrder();
    }

}
