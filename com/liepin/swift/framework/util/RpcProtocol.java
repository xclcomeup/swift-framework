package com.liepin.swift.framework.util;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

public class RpcProtocol {

    public static enum Header {
        /**
         * biz业务异常
         */
        biz_code,
        /**
         * 异常类型
         */
        error_type;
    }

    public static enum ErrorType {
        biz, sys
    }

    public static void addBizCodeHeader(final HttpServletResponse response, String status) {
        addErrorTypeHeader(response, ErrorType.biz);
        response.addHeader(Header.biz_code.name(), status);
    }

    public static void addErrorTypeHeader(final HttpServletResponse response, ErrorType errorType) {
        response.addHeader(Header.error_type.name(), errorType.name());
    }

    public static ErrorType getErrorTypeHeader(Map<String, String> headers) {
        return Optional.ofNullable(headers).map(t -> {
            return Optional.ofNullable(t.get(Header.error_type.name())).map(k -> {
                try {
                    return ErrorType.valueOf(k);
                } catch (Exception e) {
                    return null;
                }
            }).orElse(null);
        }).orElse(null);
    }

    public static String getBizCodeHeader(Map<String, String> headers) {
        return Optional.ofNullable(headers).map(t -> {
            return t.get(Header.biz_code.name());
        }).orElse(null);
    }

}
