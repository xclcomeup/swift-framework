package com.liepin.swift.framework.mvc.filter;

/**
 * 路径适配器
 * 
 * @author yuanxl
 * 
 */
public interface PathMatcher {

    /**
     * 请求路径和规则路径匹配
     * 
     * @param urlPattern 规则路径
     * @param servletPath 请求路径
     * @return
     */
    public boolean match(String urlPattern, String servletPath);

}
