package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.mvc.filter.GenericFilter;

/**
 * 签名过滤器<br>
 * FIXME 通用接口鉴权
 * 
 * @author yuanxl
 * 
 */
public class SignFilter extends GenericFilter {

    // private APISign apiSign;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
