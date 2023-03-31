package com.liepin.swift.framework.mvc.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.mvc.filter.ServletHolder.Holder;

/**
 * 平台服务线程内数据共享类
 * 
 * @author yuanxl
 * 
 */
public class ServletHolder extends ThreadLocal<Holder> {

    @Override
    protected Holder initialValue() {
        return new Holder();
    }

    private static final ServletHolder instance = new ServletHolder();

    private ServletHolder() {
    }

    public static class Holder {
        private HttpServletRequest request;
        private HttpServletResponse response;

        public void reset() {
            request = null;
            response = null;
        }

        public void assign(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

    }

    public static void assign(HttpServletRequest request, HttpServletResponse response) {
        instance.get().assign(request, response);
    }

    public static void reset() {
        instance.get().reset();
    }

    public static HttpServletRequest getHttpServletRequest() {
        return instance.get().getRequest();
    }

    public static HttpServletResponse getHttpServletResponse() {
        return instance.get().getResponse();
    }

}
