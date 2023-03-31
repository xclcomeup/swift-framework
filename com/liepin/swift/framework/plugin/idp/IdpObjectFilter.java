package com.liepin.swift.framework.plugin.idp;

import com.liepin.dispatcher.handler.IDPHandler;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class IdpObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof IDPHandler;
    }

}
