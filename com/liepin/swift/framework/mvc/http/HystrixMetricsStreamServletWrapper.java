package com.liepin.swift.framework.mvc.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

@SuppressWarnings("serial")
public class HystrixMetricsStreamServletWrapper extends HystrixMetricsStreamServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!RequestUtil.checkInnerRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        super.doGet(request, response);
    }

}
