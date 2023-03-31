package com.liepin.swift.framework.plugin.listener;

import com.liepin.swift.framework.plugin.IObjectFilter;

public class ShutdownListenerObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof IShutdownListener;
    }

}
