package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.common.json.JsonUtil;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.framework.form.JsonForm;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.rest.json.AjaxObjectBuilder;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.JsonBodyPathFinder;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;
import com.liepin.swift.framework.util.IPUtil;

public class JsonFilterHandler extends AbstractFilterHandler implements FilterHandler, IPluginListener {

    private ControllerPlugin controllerPlugin;

    public JsonFilterHandler(List<GenericFilter> externalFilterChains) {
        setExternalFilterChains(externalFilterChains);
        ControllerPlugin.listen(this);
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        this.controllerPlugin = (ControllerPlugin) plugin;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        // Content-Type: application/json 或者 @RestController 或者 @ResponseBody 或者
        // Accept: application/json
        return RequestUtil.isJSONRequest(request) || JsonBodyPathFinder.getResponse().match(request.getServletPath())
                || RequestUtil.isAccept4JsonRequest(request);
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        Event event = super.newEvent(request);
        // event.setType("ajax");
        event.setType(Optional.ofNullable(HeadReader.Gw.getClientType(request)).orElse("json"));
        event.setName(request.getServletPath());
        event.setClientIP(IPUtil.getIpAddr(request));
        return event;
    }

    @Override
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        List<Filter> filters = getFilters(request.getServletPath());
        doFilterProxy(request, response, filters, filterChain);

        ResultStatus rs = ResultStatus.ok().setData(RequestUtil.getOutput(request));

        // 处理404
        if (AttributeUtil.isNotFound(request)) {
            exceptionResolver.handle404(request, response);
            rs = new ResultStatus(SystemEnum.API_NOTEXIST).setData(AjaxObjectBuilder.toFail(SystemEnum.API_NOTEXIST));
        } else {
            Throwable throwable = AttributeUtil.getThrowable(request);
            if (Objects.nonNull(throwable)) {
                rs = toResultStatus(throwable).setData(AjaxObjectBuilder.toFail(throwable));
            }
        }
        return rs;
    }

    @Override
    public ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        AttributeUtil.setFilterError(request);
        Throwable temp = e;
        if (exceptionInterceptor != null) {
            // 降级处理
            try {
                Object fallbackObj = exceptionInterceptor.intercept(request.getServletPath(), temp);
                return ResultStatus.ok().setData(fallbackObj);
            } catch (Throwable throwable) {
                temp = throwable;
            }
        }
        return toResultStatus(temp).setData(AjaxObjectBuilder.toFail(temp));
    }

    @Override
    public String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
        Object data = rs.getData();
        String outputStr = null;
        // 无需包装的请求
        if (controllerPlugin.isNoPack(request.getServletPath()) && data instanceof String) { // string
            outputStr = Optional.ofNullable(data).map(t -> t.toString()).orElse("");
        } else {
            JsonForm jsonForm = AjaxObjectBuilder.toSuccess(data);
            outputStr = JsonUtil.toJson(jsonForm);
        }

        // 如果是因为response返回中断，不应该再输出了
        Throwable throwable = AttributeUtil.getThrowable(request);
        if (!(Objects.nonNull(throwable) && throwable instanceof org.apache.catalina.connector.ClientAbortException)) {
            if (AttributeUtil.isNotFound(request)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);// 与页面不存在返回404对齐
                export(request, response, outputStr);
            }
            if (AttributeUtil.hasFilterError(request)) {
                export(request, response, outputStr);
            }
        }

        return outputStr;
    }

}
