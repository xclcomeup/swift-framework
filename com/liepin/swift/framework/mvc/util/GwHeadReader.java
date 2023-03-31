package com.liepin.swift.framework.mvc.util;

import javax.servlet.http.HttpServletRequest;

@Deprecated
public class GwHeadReader {

    /**
     * 用户流量灰度标记
     * 
     * @param request
     * @return
     */
    public static String getGcId(final HttpServletRequest request) {
        return request.getHeader("X-Gw-Gc-Id");
    }

}
