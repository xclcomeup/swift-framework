package com.liepin.swift.framework.mvc.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class HeadReader {

    /**
     * 判断是否app请求
     * <p>
     * header标识 X-Client-Type: app
     * 
     * @param request
     * @return
     */
    public static boolean isAppRequest(final HttpServletRequest request) {
        String header = request.getHeader("X-Client-Type");
        return header != null && header.toLowerCase().equals("app");
    }

    /**
     * 判断是否可以处理压缩响应
     * <p>
     * header标识 accept-encoding: gzip
     * 
     * @param request
     * @return
     */
    public static boolean isAcceptEncodingRequest(final HttpServletRequest request) {
        String header = request.getHeader("accept-encoding");
        if (header != null && (header.toLowerCase().indexOf("gzip") != -1)) {
            return true;
        }
        return false;
    }

    /**
     * 判断压缩数据是否无效的
     * <p>
     * header标识 content-length:
     * 
     * @param request
     * @return
     */
    public static boolean noContentLength(final HttpServletRequest request) {
        String value = request.getHeader("content-length");
        if (value == null || "0".equals(value.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 获取请求的逻辑区标示
     * 
     * @param req
     * @return
     */
    public static String getArea(final HttpServletRequest request) {
        return request.getHeader("area");
    }

    /**
     * 获取客户端希望接收的body数据类型
     * 
     * @param request
     * @return
     */
    public static String getAccept(final HttpServletRequest request) {
        return request.getHeader("accept");
    }

    /**
     * 获取所有head信息
     * 
     * @param request
     * @return
     */
    public static Map<String, String> getAll(final HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = (String) headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /**
     * 网关相关header信息
     * 
     * @author yuanxl
     *
     */
    public static class Gw {

        /**
         * 用户流量灰度标记
         * 
         * @param request
         * @return
         */
        public static String getGcId(final HttpServletRequest request) {
            return request.getHeader("X-Gw-Gc-Id");
        }

        /**
         * 请求来自什么网关
         * 
         * @param request
         * @return
         */
        public static String getAltGw(final HttpServletRequest request) {
            return request.getHeader("X-Alt-Gw");
        }

        /**
         * 网关鉴权信息传递协议
         * 
         * @param request
         * @return
         */
        public static String getAltPrincipal(final HttpServletRequest request) {
            return request.getHeader("X-Alt-Principal");
        }

        /**
         * 获取请求程序类型
         * 
         * @param request
         * @return
         */
        public static String getClientType(final HttpServletRequest request) {
            return request.getHeader("X-Client-Type");
        }

    }

}
