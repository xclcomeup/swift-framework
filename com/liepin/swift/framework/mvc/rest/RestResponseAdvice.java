package com.liepin.swift.framework.mvc.rest;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.liepin.common.json.JsonUtil;
import com.liepin.swift.framework.form.JsonForm;
import com.liepin.swift.framework.mvc.rest.json.AjaxObjectBuilder;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;

/**
 * 针对@RestController的json返回请求统一返回值拦截
 * 
 * @author yuanxl
 *
 */
@RestControllerAdvice
public class RestResponseAdvice implements ResponseBodyAdvice<Object>, IPluginListener {

    private ControllerPlugin controllerPlugin;

    public RestResponseAdvice() {
        ControllerPlugin.listen(this);
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        this.controllerPlugin = (ControllerPlugin) plugin;
    }

//    private void lazyInit() {
//        this.controllerPlugin = Optional.ofNullable(controllerPlugin).orElseGet(() -> {
//            return PluginContext.get().getPlugin(ControllerPlugin.class);
//        });
//    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest)) {
            throw new RuntimeException("only supported ServletServerHttpRequest");
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        RequestUtil.setOutput(servletRequest, body);
        boolean isPageView = AttributeUtil.isViewReturn(servletRequest);
        if (isPageView) {
            return body;
        } else {
            // 无需包装的请求
            // lazyInit();
            if (controllerPlugin.isNoPack(servletRequest.getServletPath())) {
                return body;
            }

            // 对body进行封装处理
            JsonForm jsonForm = AjaxObjectBuilder.toSuccess(body);
            // 因为在controller层中返回的是String类型，这边如果换成JsonForm的话，会导致StringMessageConverter方法类型转换异常，所以这边将对象转成字符串
            if ((body instanceof String)
                    || (String.class == returnType.getGenericParameterType() && Objects.isNull(body))) {
                return JsonUtil.toJson(jsonForm);
            }
            return jsonForm;
        }
    }

}
