package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.IPUtil;
import com.liepin.swift.framework.util.LogHelper;

public class PageFilterHandler extends AbstractFilterHandler implements FilterHandler {

    public PageFilterHandler(List<GenericFilter> externalFilterChains) {
        setExternalFilterChains(externalFilterChains);
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        return true;
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        Event event = super.newEvent(request);
        event.setType("page");
        event.setName(request.getServletPath());
        event.setClientIP(IPUtil.getIpAddr(request));
        return event;
    }

    @Override
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AttributeUtil.setViewReturn(request);
        // 执行拦截器
        List<Filter> filters = getFilters(request.getServletPath());
        doFilterProxy(request, response, filters, filterChain);

        ResultStatus rs = ResultStatus.ok();
        // 处理404
        if (response.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            exceptionResolver.handle404(request, response);
            rs = new ResultStatus(SystemEnum.API_NOTEXIST);
        } else {
            Throwable throwable = AttributeUtil.getThrowable(request);
            if (Objects.nonNull(throwable)) {
                rs = toResultStatus(throwable);
            }
        }
        return rs;
    }

    @Override
    public ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        // 处理500，filter产生的
        LogHelper.logError(e, request);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        exceptionResolver.handleFilter500(request, response, e);
        return toResultStatus(e);
    }

    @Override
    public String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
        // 普通页面请求交由spring管理，这里不做输出
        Object object = RequestUtil.getOutput(request);
        return (Objects.nonNull(object)) ? "{\"" + object.toString() + "\"}" : "{}";
    }

}
