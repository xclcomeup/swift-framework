package com.liepin.swift.framework.mvc.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.swift.framework.mvc.util.RequestUtil;

/**
 * 基础拦截器
 * 
 * @author yuanxl
 * 
 */
public abstract class GenericFilter implements Filter {

    private static final Logger logger = Logger.getLogger(GenericFilter.class);

    public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    public static final String CHARSET_UTF_8 = "UTF-8";
    public static final String CHARSET_GB2312 = "GB2312";

    protected static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();

    protected final Map<String, String> initParams = new HashMap<String, String>();

    private ServletContext servletContext;

    public final void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    protected final ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Initializing filter '" + getFilterName() + "'");
        Enumeration<String> initParameterNames = filterConfig.getInitParameterNames();
        while (initParameterNames.hasMoreElements()) {
            String name = initParameterNames.nextElement();
            initParams.put(name, filterConfig.getInitParameter(name));
        }
        initFilterBean();

        logger.info("Filter '" + getFilterName() + "' initParams=" + initParams + " configured successfully");
    }

    @Override
    public void destroy() {
    }

    /**
     * 真实执行方法
     * 
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    protected abstract void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException;

    /**
     * 拦截url路径
     * 
     * @return
     */
    protected abstract String urlPattern();

    /**
     * 预留Filter对象初始行为扩展方法
     * 
     * @throws ServletException
     */
    protected void initFilterBean() throws ServletException {
    }

    @Override
    public final void doFilter(final ServletRequest request, final ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("GenericFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // 包装response
        // StatusHttpServletResponseWrapper responseWrapper = new
        // StatusHttpServletResponseWrapper(httpResponse);

        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
        if ((request.getAttribute(alreadyFilteredAttributeName) != null && onceFilter())
                || shouldNotFilter(httpRequest)) {
            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        } else {
            // Do invoke this filter...
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            try {
                doFilterInternal(httpRequest, httpResponse, filterChain);
            } finally {
                // Remove the "already filtered" request attribute for this
                // request.
                request.removeAttribute(alreadyFilteredAttributeName);
            }
        }

    }

    protected String getAlreadyFilteredAttributeName() {
        String name = getFilterName();
        if (name == null) {
            name = getClass().getName();
        }
        return name + ALREADY_FILTERED_SUFFIX;
    }

    /**
     * 是否跳过过滤器
     * <p>
     * 默认 否
     * 
     * @param request
     * @return
     * @throws ServletException
     */
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return false;
    }

    protected final String getFilterName() {
        return getClass().getName();
    }

    /**
     * 是否全局拦截一次，默认执行一次，如需修改请覆盖
     * 
     * @return
     */
    protected boolean onceFilter() {
        return true;
    }

    /**
     * 判断压缩数据是否无效的
     * 
     * @param req
     * @return
     */
    @Deprecated
    protected boolean isInvalid(HttpServletRequest req) {
        String value = req.getHeader("content-length");
        if (value == null || "0".equals(value.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 获取请求的逻辑区标示
     * 
     * @param req
     * @return
     */
    @Deprecated
    protected String getArea(HttpServletRequest req) {
        return req.getHeader("area");
    }

    /**
     * 获取内网远程地址真实ip地址
     * 
     * @param request
     * @return
     */
    protected String getClientIp(HttpServletRequest request) {
        return RequestUtil.getClientIp(request);
    }

    protected String printHeader(final HttpServletRequest request) {
        StringBuilder log = new StringBuilder();
        log.append("url=").append(request.getServletPath()).append(" | headers= ");
        Enumeration<?> headerNames = request.getHeaderNames();
        int i = 0;
        while (headerNames.hasMoreElements()) {
            String name = (String) headerNames.nextElement();
            if (i++ != 0) {
                log.append(", ");
            }
            log.append(name).append("=").append(request.getHeader(name));
        }
        return log.toString();
    }

}
