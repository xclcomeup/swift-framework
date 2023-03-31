package com.liepin.swift.framework.mvc.resolver;

import java.lang.reflect.Method;

/**
 * Ajax请求异常拦截器
 * <p>
 * recommended use {@link IExceptionInterceptor}
 * 
 * @since 2.2.0版本开始已不兼容，使用请迁移到 {@link IExceptionInterceptor}
 * 
 * @author yuanxl
 *
 */
@Deprecated
public interface IAjaxExceptionInterceptor {

    /**
     * 异常处理方法<br>
     * 1. 可以对异常降级，返回降低结果<br>
     * 2. 可以不对异常降级处理，继续将异常返回，继续抛原异常即可<br>
     * 
     * @param servletPath 请求url
     * @param method 调用的方法，可空（filter异常）
     * @param throwable 抛的异常
     * @return
     * @throws Throwable 原异常返回或者未捕获的异常
     */
    public Object intercept(String servletPath, Method method, Throwable throwable) throws Throwable;

}
