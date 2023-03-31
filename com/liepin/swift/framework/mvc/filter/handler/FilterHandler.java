package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;

public interface FilterHandler {

    /**
     * 当前处理器是否支持
     * 
     * @param request
     * @return
     */
    public boolean supports(HttpServletRequest request);

    /**
     * 创建一个事件
     * 
     * @param request
     * @return
     */
    public Event newEvent(HttpServletRequest request);

    /**
     * 处理方法
     * 
     * @param request
     * @param response
     * @param filterChain
     * @return
     * @throws ServletException
     * @throws IOException
     */
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException;

    /**
     * 异常处理
     * 
     * @param request
     * @param response
     * @param e
     * @return
     */
    default ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        return ResultStatus.ok();
    }

    /**
     * 输出
     * 
     * @param request
     * @param response
     * @param outputStr
     * @throws Exception
     */
    default String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
        return null;
    }

    /**
     * 有状态的
     * 
     * @return
     */
    default boolean context() {
        return true;
    }

}
