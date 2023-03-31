package com.liepin.swift.framework.mvc.filter;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 代理Filter职责链
 * 
 * @author yuanxl
 * 
 */
public class ProxyFilterChain implements FilterChain {

    private final List<Filter> additionalFilters;
    private int currentPosition = 0;
    private final FilterChain filterChain;

    public ProxyFilterChain(List<Filter> additionalFilters, FilterChain filterChain) {
        this.additionalFilters = additionalFilters;
        this.filterChain = filterChain;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException,
            ServletException {
        if (currentPosition == additionalFilters.size()) {
            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }
        } else {
            currentPosition++;
            Filter nextFilter = additionalFilters.get(currentPosition - 1);
            nextFilter.doFilter(request, response, this);
        }
    }

}
