package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.mvc.filter.GenericFilter;

/**
 * 字符集过滤器
 * <p>
 * 默认UTF-8
 * 
 * @author yuanxl
 * 
 */
public class CharacterEncodingFilter extends GenericFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setCharacterEncoding(CHARSET_UTF_8);
        response.setCharacterEncoding(CHARSET_UTF_8);
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
