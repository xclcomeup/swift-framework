package com.liepin.swift.framework.plugin.gateway.interceptor;

import com.liepin.gateway.api.spi.GwApiInterceptor;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class GwApiInterceptorObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof GwApiInterceptor;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

}
