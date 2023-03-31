package com.liepin.swift.framework.mvc.dispatcher;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.log.StartLog;
import com.liepin.swift.framework.log.initializer.SwiftLogInitializer;
import com.liepin.swift.framework.mvc.WebApplicationContextHolder;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.plugin.resource.ResourcePlugin;
import com.liepin.swift.framework.util.UrlUtil;

public abstract class AbstractAdaptorDispatcherServlet extends DispatcherServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(AbstractAdaptorDispatcherServlet.class);

    @Override
    protected void initFrameworkServlet() throws ServletException {
        logger.info("Initializing servlet '" + getServletName() + "'");
        super.initFrameworkServlet();
        WebApplicationContextHolder.setApplicationContext(getWebApplicationContext());
        initDispatchBean();
        ResourcePlugin.setServlet(this);
        logger.info("Servlet '" + getServletName() + "' configured successfully");
    }

    @Override
    protected final WebApplicationContext initWebApplicationContext() {
        try {
            return super.initWebApplicationContext();
        } catch (Throwable e) {
            StartLog.err(logger, SwiftLogInitializer.printFailMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        doSomethingBeforeDestroy();
        super.destroy();
        destroyDispatchBean();
    }

    /**
     * 初始安装
     */
    protected void initDispatchBean() {
        // 提高性能
        setPublishContext(false);
        setPublishEvents(false);
    }

    /**
     * 结束卸载
     */
    protected void destroyDispatchBean() {

    }

    /**
     * 在停止前处理事情预留接口
     */
    protected void doSomethingBeforeDestroy() {

    }

    /**
     * 请求地址映射到对象和方法
     * 
     * @param servletPath
     * @return
     */
    protected abstract <T extends DispatcherBean> T mappingRPC(String servletPath);

    protected abstract <T extends DispatcherBean> T mappingGW(String servletPath);

    /**
     * 外部API方法
     * 
     * @param bean
     * @param req
     * @param resp
     * @throws Exception
     */
    protected abstract void serviceApi(DispatcherBean bean, HttpServletRequest req, HttpServletResponse resp)
            throws Exception;

    protected abstract void gwApi(DispatcherBean bean, HttpServletRequest req, HttpServletResponse resp)
            throws Exception;

    /**
     * 重载实现自己的404控制
     */
    @Override
    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.warn("No mapping found for HTTP request with URI [" + request.getRequestURI()
                + "] in SwiftDispatcherServlet with name '" + getServletName() + "'");
        AttributeUtil.setErrorStatusCode(request, HttpServletResponse.SC_NOT_FOUND);
    }


    @Override
    protected final void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String servletPath = request.getServletPath();
        if (UrlUtil.isRPC(servletPath)) {
            // RPC
            dispatchRPC(request, response);
        } else if (UrlUtil.isGW(servletPath)) {
            // GW
            dispatchGW(request, response);
        } else {
            // MVC
            super.doService(request, response);
        }
    }

    /**
     * 重载解决：json请求接口不存在逻辑处理<br>
     * 默认接口不存在的处理方法：返回404，然后forward到/error页面，BasicErrorController类
     */
    @Nullable
    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        HandlerExecutionChain handler = super.getHandler(request);
        if (handler != null) {
            if (handler.getHandler() instanceof ResourceHttpRequestHandler) {
                // 后续改造请求json判断标准
                // if (RequestUtil.isJsonRequest(request) &&
                // RequestUtil.isAjaxRequest(request)) {
                // 请求是json类型的 或者 接收返回是json类型的
                if (RequestUtil.isJSONRequest(request) || RequestUtil.isAccept4JsonRequest(request)) {
                    return null;
                }
            }
        }
        return handler;
    }

    /**
     * 处理请求
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private final void dispatchRPC(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String servletPath = request.getServletPath();
        String path = UrlUtil.compile(servletPath, UrlUtil.getNamespace4API());

        DispatcherBean bean = mappingRPC(path);
        if (bean == null) {
            logger.warn("No mapping found for RPC request with URI [" + servletPath + "] in SwiftDispatcherServlet");
            throw new SysException(SystemEnum.API_NOTEXIST.code(),
                    SystemEnum.API_NOTEXIST.message() + " " + servletPath);
        }
        AttributeUtil.setAjaxMethod(request, bean.method);
        serviceApi(bean, request, response);
    }

    private final void dispatchGW(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String servletPath = request.getServletPath();
        String path = UrlUtil.removeNamespace(servletPath, UrlUtil.getNamespace4GWAPI());

        DispatcherBean bean = mappingGW(path);
        if (bean == null) {
            logger.warn("No mapping found for GW request with URI [" + servletPath + "] in SwiftDispatcherServlet");
            throw new SysException(SystemEnum.API_NOTEXIST.code(),
                    SystemEnum.API_NOTEXIST.message() + " " + servletPath);
        }
        AttributeUtil.setAjaxMethod(request, bean.method);
        gwApi(bean, request, response);
    }

}
