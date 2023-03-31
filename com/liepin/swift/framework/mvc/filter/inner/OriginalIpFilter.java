package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.util.IPUtil;

/**
 * 获取用户真实ip拦截器
 * 
 * @author yuanxl
 * @date 2015-5-15 下午03:28:08
 */
public class OriginalIpFilter extends GenericFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String originalIP = IPUtil.getIpAddr(request);
        ThreadLocalUtil.getInstance().setOriginalIP(originalIP);
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
