package com.liepin.swift.framework.mvc.resolver;

/**
 * 异常拦截器：用于对异常进行统一切面处理
 * <p>
 * 只适用于：RPC、GW、JSON请求<br>
 *
 */
public interface IExceptionInterceptor {

    /**
     * 异常处理方法<br>
     * 1. 可以对异常降级，返回降低结果<br>
     * 2. 可以不对异常降级处理，继续将异常返回，继续抛原异常即可<br>
     * 
     * @param servletPath 请求url
     * @param throwable 抛的异常
     * @return
     * @throws Throwable 原异常返回或者未捕获的异常
     */
    public Object intercept(String servletPath, Throwable throwable) throws Throwable;

}
