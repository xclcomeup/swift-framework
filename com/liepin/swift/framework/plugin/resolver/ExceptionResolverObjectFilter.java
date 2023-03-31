package com.liepin.swift.framework.plugin.resolver;

import com.liepin.swift.framework.mvc.resolver.IExceptionResolver;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class ExceptionResolverObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof IExceptionResolver;
    }

}
