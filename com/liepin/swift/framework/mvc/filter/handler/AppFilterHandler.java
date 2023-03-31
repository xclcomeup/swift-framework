//package com.liepin.swift.framework.mvc.filter.handler;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//
//import javax.servlet.Filter;
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.liepin.common.json.JsonUtil;
//import com.liepin.swift.core.enums.SystemEnum;
//import com.liepin.swift.framework.form.JsonForm;
//import com.liepin.swift.framework.mvc.ResultStatus;
//import com.liepin.swift.framework.mvc.eventInfo.Event;
//import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
//import com.liepin.swift.framework.mvc.filter.GenericFilter;
//import com.liepin.swift.framework.mvc.rest.json.AjaxObjectBuilder;
//import com.liepin.swift.framework.mvc.util.AttributeUtil;
//import com.liepin.swift.framework.mvc.util.RequestUtil;
//import com.liepin.swift.framework.util.IPUtil;
//
//public class AppFilterHandler extends AbstractFilterHandler implements FilterHandler {
//
//    public AppFilterHandler(List<GenericFilter> externalFilterChains) {
//        setExternalFilterChains(externalFilterChains);
//    }
//
//    @Override
//    public boolean supports(HttpServletRequest request) {
//        return RequestUtil.isAppRequest(request);
//    }
//
//    @Override
//    public Event newEvent(HttpServletRequest request) {
//        Event event = super.newEvent(request);
//        event.setType("app");
//        event.setName(request.getServletPath());
//        event.setClientIP(IPUtil.getIpAddr(request));
//        return event;
//    }
//
//    @Override
//    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        List<Filter> filters = getFilters(request.getServletPath());
//        doFilterProxy(request, response, filters, filterChain);
//
//        ResultStatus rs = ResultStatus.ok().setData(RequestUtil.getOutput(request));
//
//        // 处理404
//        if (AttributeUtil.isNotFound(request)) {
//            rs = new ResultStatus(SystemEnum.API_NOTEXIST).setData(AjaxObjectBuilder.toFail(SystemEnum.API_NOTEXIST));
//        }
//        return rs;
//    }
//
//    @Override
//    public ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
//        Throwable temp = e;
//        if (exceptionInterceptor != null) {
//            // 降级处理
//            try {
//                Object fallbackObj = exceptionInterceptor.intercept(request.getServletPath(), temp);
//                return ResultStatus.ok().setData(fallbackObj);
//            } catch (Throwable throwable) {
//                temp = throwable;
//            }
//        }
//        return toResultStatus(temp).setData(AjaxObjectBuilder.toFail(temp));
//    }
//
//    @Override
//    public String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
//        JsonForm jsonForm = null;
//        if (!(rs.getData() instanceof JsonForm)) {
//            jsonForm = new JsonForm();
//            jsonForm.setData((rs.getData() != null) ? rs.getData() : Collections.emptyMap());
//        } else {
//            jsonForm = (JsonForm) rs.getData();
//        }
//        String outputStr = JsonUtil.toJson(jsonForm);
//
//        if (AttributeUtil.isNotFound(request)) {
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        }
//        export(request, response, outputStr, false);
//
//        return outputStr;
//    }
//
//}
