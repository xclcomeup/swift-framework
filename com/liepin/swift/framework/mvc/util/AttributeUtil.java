package com.liepin.swift.framework.mvc.util;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AttributeUtil {

    public static final String ATTRIBUTE_THROWABLE = "javax.servlet.throwable";
    public static final String ATTRIBUTE_ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    public static final String ATTRIBUTE_JSONFORM_DATA = "javax.servlet.jsonform.data";
    public static final String ATTRIBUTE_APP_DECRYPT_FAILED = "javax.servlet.app.decrypt.failed";
    public static final String ATTRIBUTE_AJAX_METHOD = "javax.servlet.ajax.method";
    public static final String ATTRIBUTE_VIEW_RETURN = "javax.servlet.view.return";
    public static final String ATTRIBUTE_ERROR_IGNORE = "javax.servlet.error.ignore";
    public static final String ATTRIBUTE_FILTER_ERROR = "javax.servlet.filter.error";

    public static final void setErrorStatusCode(HttpServletRequest request, int code) {
        request.setAttribute(ATTRIBUTE_ERROR_STATUS_CODE, code);
    }

    public static final Integer getErrorStatusCode(HttpServletRequest request) {
        return (Integer) request.getAttribute(ATTRIBUTE_ERROR_STATUS_CODE);
    }

    /**
     * 判断请求的接口是否不存在
     * 
     * @param request
     * @return true:不存在 | false:存在
     */
    public static final boolean isNotFound(final HttpServletRequest request) {
        Integer status = getErrorStatusCode(request);
        return Objects.nonNull(status) && status.intValue() == HttpServletResponse.SC_NOT_FOUND;
    }

    @Deprecated
    public static final void setJsonFormData(HttpServletRequest request, Object data) {
        request.setAttribute(ATTRIBUTE_JSONFORM_DATA, data);
    }

    @Deprecated
    public static final Object getJsonFormData(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE_JSONFORM_DATA);
    }

    @Deprecated
    public static final void setAppDecryptFailed(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE_APP_DECRYPT_FAILED, 1);
    }

    @Deprecated
    public static final boolean hasAppDecryptFailed(HttpServletRequest request) {
        Integer code = (Integer) request.getAttribute(ATTRIBUTE_APP_DECRYPT_FAILED);
        return code != null && 1 == code.intValue();
    }

    public static final Method getAjaxMethod(HttpServletRequest request) {
        return (Method) request.getAttribute(ATTRIBUTE_AJAX_METHOD);
    }

    /**
     * for ajax、app、rpc、gw 请求
     * 
     * @param request
     * @param method
     */
    public static final void setAjaxMethod(HttpServletRequest request, Method method) {
        request.setAttribute(ATTRIBUTE_AJAX_METHOD, method);
    }

    public static final void setViewReturn(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE_VIEW_RETURN, true);
    }

    public static final boolean isViewReturn(HttpServletRequest request) {
        Boolean viewObj = (Boolean) request.getAttribute(ATTRIBUTE_VIEW_RETURN);
        return viewObj != null && true == viewObj.booleanValue();
    }

    public static final void setThrowable(HttpServletRequest request, Throwable throwable) {
        request.setAttribute(ATTRIBUTE_THROWABLE, throwable);
    }

    public static final Throwable getThrowable(HttpServletRequest request) {
        return (Throwable) request.getAttribute(ATTRIBUTE_THROWABLE);
    }

    public static final void setErrorIgnore(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE_ERROR_IGNORE, 1);
    }

    public static final boolean hasErrorIgnore(HttpServletRequest request) {
        Integer code = (Integer) request.getAttribute(ATTRIBUTE_ERROR_IGNORE);
        return code != null && 1 == code.intValue();
    }

    public static final void setFilterError(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE_FILTER_ERROR, 1);
    }

    public static final boolean hasFilterError(HttpServletRequest request) {
        Integer code = (Integer) request.getAttribute(ATTRIBUTE_FILTER_ERROR);
        return code != null && 1 == code.intValue();
    }

    public static final void clean(HttpServletRequest request) {
        request.removeAttribute(ATTRIBUTE_ERROR_STATUS_CODE);
        request.removeAttribute(ATTRIBUTE_JSONFORM_DATA);
        request.removeAttribute(ATTRIBUTE_APP_DECRYPT_FAILED);
        request.removeAttribute(ATTRIBUTE_AJAX_METHOD);
        request.removeAttribute(ATTRIBUTE_VIEW_RETURN);
        request.removeAttribute(ATTRIBUTE_THROWABLE);
        request.removeAttribute(ATTRIBUTE_ERROR_IGNORE);
        request.removeAttribute(ATTRIBUTE_FILTER_ERROR);
    }

}
