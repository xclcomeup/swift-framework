package com.liepin.swift.framework.mvc.resolver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

/**
 * Do Nothing
 * 
 * @author yuanxl
 * 
 */
public class DefaultExceptionResolver implements IExceptionResolver {

    private static final Logger logger = Logger.getLogger(DefaultExceptionResolver.class);

    @Override
    public void handle404(HttpServletRequest request, HttpServletResponse response) {
        logger.warn("No mapping found for HTTP request with URI [" + request.getRequestURI()
                + "] in SwiftDispatcherServlet with name 'dispatcherServlet'");
    }

    @Override
    public ModelAndView handle500(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        ModelAndView modelAndView = new ModelAndView("/error/500.jsp");
        request.setAttribute("swift_timestamp", new Date());
        request.setAttribute("swift_status", response.getStatus());
        request.setAttribute("swift_url", request.getRequestURL().toString());
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        stackTrace.flush();
        request.setAttribute("swfit_trace", stackTrace.toString());
        return modelAndView;
    }

}
