package com.liepin.swift.framework.mvc.resolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

/**
 * 异常分解器：用于对异常进行统一切面处理
 * <p>
 * 只适用于：PAGE页面请求，针对404页面不存在、500内部异常情况进行降级处理<br>
 *
 */
public interface IExceptionResolver {

    static final Logger logger = Logger.getLogger(IExceptionResolver.class);

    /**
     * URL不存在 异常
     * <p>
     * 备注：不需要单独forward 404页面，springboot默认会根据status查找资源目录404文件
     * 
     * @param request
     * @param response
     */
    public void handle404(HttpServletRequest request, HttpServletResponse response);

    /**
     * Controller 异常
     * 
     * @param request
     * @param response
     * @param throwable
     * @return
     */
    public ModelAndView handle500(HttpServletRequest request, HttpServletResponse response, Throwable throwable);

    /**
     * Filter 异常
     * 
     * @param request
     * @param response
     * @param throwable
     */
    default void handleFilter500(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {
        logger.warn("URI [" + request.getRequestURI() + "] filter 500, need add filter exception page handling");
    }

    /**
     * 请求内部跳转
     * 
     * @param request
     * @param response
     * @param path
     */
    default void forward(HttpServletRequest request, HttpServletResponse response, String path) {
        try {
            request.getRequestDispatcher(path).forward(request, response);
        } catch (Exception e) {
            logger.error("forward path=" + path + " fail", e);
        }
    }

}
