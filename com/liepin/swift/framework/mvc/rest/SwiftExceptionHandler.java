package com.liepin.swift.framework.mvc.rest;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dianping.cat.Cat;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ThrowableUtil;
import com.liepin.swift.framework.mvc.resolver.IExceptionInterceptor;
import com.liepin.swift.framework.mvc.resolver.IExceptionResolver;
import com.liepin.swift.framework.mvc.rest.json.AjaxObjectBuilder;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.resolver.ExceptionResolverPlugin;
import com.liepin.swift.framework.util.CatHelper;
import com.liepin.swift.framework.util.LogHelper;

@RestControllerAdvice
public class SwiftExceptionHandler {

    @Autowired(required = false)
    private IExceptionResolver exceptionResolver;

    @Autowired(required = false)
    private IExceptionInterceptor exceptionInterceptor;

    private void lazyInit() {
        this.exceptionResolver = Optional.ofNullable(exceptionResolver).orElseGet(() -> {
            return PluginContext.get().getPlugin(ExceptionResolverPlugin.class).getObject();
        });
    }

    /**
     * 异常统一处理，不能再向上抛异常
     * 
     * @param throwable
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler()
    public Object resolveException(Throwable throwable, HttpServletRequest request, HttpServletResponse response) {
        // 获取真正异常
        Throwable actual = ThrowableUtil.unwrapThrowable(throwable);

        // CAT埋点
        CatHelper.logError(request, Cat.getManager().getPeekTransaction(), actual);

        // 请求参数异常，特殊处理
        if (actual instanceof TypeMismatchException || actual instanceof MissingServletRequestParameterException) {
            MonitorLogger.getInstance().log(request.getServletPath() + " | " + actual.getMessage());
            actual = new BizException(SystemEnum.PARAMETER_EXCEPTION);
        }

        // 日志输出
        LogHelper.logError(actual, request);

        // 记录错误栈
        AttributeUtil.setThrowable(request, actual);

        // 判断是否页面请求
        boolean isPageView = AttributeUtil.isViewReturn(request);
        if (isPageView) {
            lazyInit();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return exceptionResolver.handle500(request, response, actual);
        } else {
            // 降级处理
            if (exceptionInterceptor != null) {
                try {
                    Object fallbackObj = exceptionInterceptor.intercept(request.getServletPath(), actual);
                    // 降级成功，去掉异常判断
                    AttributeUtil.setThrowable(request, null);
                    return Optional.ofNullable(fallbackObj).orElseGet(() -> AjaxObjectBuilder.toSuccess(null));// 不能返回null
                } catch (Throwable temp) {
                    actual = temp;
                }
            }
            return AjaxObjectBuilder.toFail(actual);
        }
    }

}
