//package com.liepin.swift.framework.mvc.filter.handler;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.liepin.common.json.JsonUtil;
//import com.liepin.swift.core.enums.SystemEnum;
//import com.liepin.swift.core.exception.IMessageCode;
//import com.liepin.swift.core.exception.SysException;
//import com.liepin.swift.core.log.MonitorLogger;
//import com.liepin.swift.core.util.TraceIdUtil;
//import com.liepin.swift.framework.form.CompatibleJsonForm;
//import com.liepin.swift.framework.form.JsonForm;
//import com.liepin.swift.framework.mvc.ResultStatus;
//import com.liepin.swift.framework.mvc.eventInfo.Event;
//import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
//import com.liepin.swift.framework.mvc.filter.GenericFilter;
//import com.liepin.swift.framework.mvc.filter.inner.AppFilter;
//import com.liepin.swift.framework.mvc.filter.inner.InputParamFilter;
//import com.liepin.swift.framework.mvc.util.AttributeUtil;
//import com.liepin.swift.framework.mvc.util.RequestUtil;
//import com.liepin.swift.framework.util.IPUtil;
//
//public class WxaFilterHandler extends AbstractFilterHandler implements FilterHandler {
//
//    public WxaFilterHandler(List<GenericFilter> externalFilterChains) {
//        setExternalFilterChains(appendFilter(externalFilterChains));
//        // setPreprocessor(new AppPreprocessor());
//    }
//
//    private List<GenericFilter> appendFilter(List<GenericFilter> externalFilterChains) {
//        List<GenericFilter> list = new ArrayList<GenericFilter>();
//        for (int i = 0; i < externalFilterChains.size(); i++) {
//            GenericFilter filter = externalFilterChains.get(i);
//            list.add(filter);
//            if (filter instanceof InputParamFilter) {
//                list.add(new AppFilter());
//            }
//        }
//        return list;
//    }
//
//    @Override
//    public boolean supports(HttpServletRequest request) {
//        return RequestUtil.isJsonRequest(request) && RequestUtil.isWxaRequest(request);
//    }
//
//    @Override
//    public Event newEvent(HttpServletRequest request) {
//        Event event = super.newEvent(request);
//        event.setType("wxa");
//        event.setName(request.getServletPath());
//        event.setClientIP(IPUtil.getIpAddr(request));
//        return event;
//    }
//
//    @Override
//    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        // 执行拦截器
//        doFilterProxy(request, response, getFilters(request.getServletPath()), null);
//        // 预处理
//        // preprocessor.preprocess(RequestUtil.getInput(request), request);
//
//        filterChain.doFilter(request, response);
//
//        ResultStatus ok = ResultStatus.ok();
//        ok.setData(RequestUtil.getOutput(request));
//        return ok;
//    }
//
//    @Override
//    public ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
//        ResultStatus rs = toResultStatus(e);
//        rs.setData(jsonRequestErrorHandle(request, e, new CompatibleJsonForm()));
//        return rs;
//    }
//
//    @Override
//    public String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
//        JsonForm jsonForm = null;
//        if (!(rs.getData() instanceof JsonForm)) {
//            jsonForm = new CompatibleJsonForm();
//            jsonForm.setData((rs.getData() != null) ? rs.getData() : Collections.emptyMap());
//        } else {
//            jsonForm = (JsonForm) rs.getData();
//        }
//        String outputStr = JsonUtil.toJson(jsonForm);
//
//        postprocessor.postprocess(request);
//
//        export(request, response, outputStr, false);
//        return outputStr;
//    }
//
//    @Override
//    protected JsonForm jsonRequestErrorHandle(HttpServletRequest request, Throwable throwable, JsonForm form) {
//        form.setFlag(JsonForm.FLAG_FAIL);
//        if (throwable instanceof IMessageCode) {
//            if (SysException.class != throwable.getClass()) {
//                IMessageCode messageCode = (IMessageCode) throwable;
//                setErr(form, messageCode);
//                MonitorLogger.getInstance().log(throwable.getMessage(), throwable);
//            } else {
//                setErr(form, SystemEnum.UNKNOWN);
//                catalinaLog.error(
//                        "traceId=" + TraceIdUtil.getTraceId() + " " + throwable.getMessage() + ignoreTag(request),
//                        throwable);
//            }
//        } else {
//            setErr(form, SystemEnum.UNKNOWN);
//            catalinaLog.error("traceId=" + TraceIdUtil.getTraceId() + " " + throwable.getMessage() + ignoreTag(request),
//                    throwable);
//        }
//
//        // 处理当异常情况时JsonForm的data数据填充
//        Object jsonFormData = AttributeUtil.getJsonFormData(request);
//        if (jsonFormData != null) {
//            form.setData(jsonFormData);
//        }
//        return form;
//    }
//
//}
