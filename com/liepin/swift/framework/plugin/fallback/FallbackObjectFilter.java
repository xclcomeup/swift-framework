package com.liepin.swift.framework.plugin.fallback;

import com.liepin.swift.framework.plugin.IObjectFilter;

public class FallbackObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o.getClass().getSimpleName().endsWith("Fallback");
    }

}
