package com.liepin.swift.framework.plugin.filter;

import com.liepin.swift.framework.mvc.filter.external.ExternalFilter;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class ExternalFilterObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof ExternalFilter;
    }

}
