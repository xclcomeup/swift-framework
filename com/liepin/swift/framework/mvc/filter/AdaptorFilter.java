package com.liepin.swift.framework.mvc.filter;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.ThrowableUtil;
import com.liepin.swift.framework.monitor.call.RepeatCallCollecter;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.AbstractIOLogger;
import com.liepin.swift.framework.mvc.eventInfo.BaseIOLogger;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.filter.handler.FilterHandler;
import com.liepin.swift.framework.mvc.filter.handler.FilterHandlerManager;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.CatHelper;

/**
 * 请求拦截适配器
 * 
 * @author yuanxl
 * 
 */
public abstract class AdaptorFilter extends GenericFilter {

    private static final Logger logger = Logger.getLogger(AdaptorFilter.class);

    private FilterHandlerManager filterHandlerManager = new FilterHandlerManager();
    private AbstractIOLogger ioLogger = new BaseIOLogger();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 保管request、response
        ServletHolder.assign(request, response);
        // 选择处理器
        FilterHandler filterHandler = filterHandlerManager.getFilterHandler(request);
        // 请求是否有上下文：框架内部请求、tomcat静态请求无状态，走快速模式
        boolean context = filterHandler.context();
        // 请求事件
        Event eventInfo = filterHandler.newEvent(request);
        // 结果状态
        ResultStatus rs = ResultStatus.ok();
        // 有上下文的
        if (context) {
            preprocessor(request);
        }

        // cat
        Transaction t = Cat.newTransaction(eventInfo.getType(), eventInfo.getName());
        try {
            // 业务执行
            rs = filterHandler.handle(request, response, filterChain);
            if (Objects.isNull(t.getStatus())) {
                t.setStatus(Message.SUCCESS);
            }
        } catch (Throwable e) {
            // 异常处理
            Throwable actual = ThrowableUtil.unwrapThrowable(e);
            CatHelper.logError(request, t, actual);
            rs = filterHandler.resolveException(request, response, actual);
        } finally {
            try {
                // 输出结果
                String outputStr = filterHandler.output(request, response, rs);
                eventInfo.setOutput(outputStr);
            } catch (Throwable e) {
                rs = ResultStatus.unknown(e.getMessage());
                logger.warn(e.getMessage(), e);
            }

            try {
                // 记录日志
                eventInfo.setStatus(rs.getStatus());
                eventInfo.setInput(RequestUtil.getInput(request));
                eventInfo.submit();
                ioLogger.log(eventInfo);
            } finally {
                // 回收
                if (context) {
                    postprocessor(request);
                }
                ServletHolder.reset();
                t.complete();
            }
        }
    }

    @Override
    public String urlPattern() {
        return null;
    }

    @Override
    protected void initFilterBean() throws ServletException {
        filterHandlerManager.initFilterHandler();
        logger.info("AdaptorFilter '" + getFilterName() + "' configured successfully");
    }

    /**
     * 处理前 do something
     * 
     * @param request
     */
    protected void preprocessor(final HttpServletRequest request) {
        // 记录上下文
        ThreadLocalUtil.getInstance().setCurrentUrl(request.getServletPath());
        ThreadLocalUtil.getInstance().setClientIp(RequestUtil.getClientIp(request));

        RepeatCallCollecter.getInstance().beginTransaction(request.getServletPath());
    }

    /**
     * 处理后 do something
     * 
     * @param request
     */
    protected void postprocessor(final HttpServletRequest request) {
        // 清理上下文
        ThreadLocalUtil.getInstance().remove();
        RequestUtil.clean(request);

        RepeatCallCollecter.getInstance().endTransaction();
    }

}
