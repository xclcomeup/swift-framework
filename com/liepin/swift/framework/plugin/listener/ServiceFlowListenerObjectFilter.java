package com.liepin.swift.framework.plugin.listener;

import com.liepin.router.discovery.IServiceFlowListener;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class ServiceFlowListenerObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof IServiceFlowListener;
    }

}
