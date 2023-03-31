package com.liepin.swift.framework.plugin.resolver.ajax;

import com.liepin.swift.framework.mvc.resolver.IExceptionInterceptor;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class ExceptionInterceptorObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof IExceptionInterceptor;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }
}
