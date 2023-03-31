package com.liepin.swift.framework.mvc.resolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

/**
 * MVC 异常处理器
 * 
 * @author yuanxl
 * 
 */
@Deprecated
public abstract class AbstractExceptionResolver implements IExceptionResolver {

    private static final Logger logger = Logger.getLogger(AbstractExceptionResolver.class);

    @Override
    public void handle404(HttpServletRequest request, HttpServletResponse response) {
        try {
            // response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resolve404(request, response, request.getRequestURI());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public ModelAndView handle500(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            return resolve500(request, response, throwable);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Url Not Found 处理
     * 
     * @param request
     * @param response
     * @param requestUri
     * @throws Exception
     */
    public abstract void resolve404(HttpServletRequest request, HttpServletResponse response, String requestUri)
            throws Exception;

    /**
     * Internal Server Error 处理
     * 
     * @param request
     * @param response
     * @param e
     * @param errMessages
     * @throws Exception
     */
    public abstract ModelAndView resolve500(HttpServletRequest request, HttpServletResponse response, Throwable e)
            throws Exception;

    /**
     * 请求内部跳转
     * <p>
     * 注意：404请求会自动跳转404页面
     * 
     * @param request
     * @param response
     * @param path
     */
    public void forward(HttpServletRequest request, HttpServletResponse response, String path) {
        try {
            request.getRequestDispatcher(path).forward(request, response);
        } catch (Exception e) {
            logger.error("forward path=" + path + " fail", e);
        }
    }

}
