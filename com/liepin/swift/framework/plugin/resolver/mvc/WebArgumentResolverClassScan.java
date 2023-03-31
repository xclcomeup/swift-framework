package com.liepin.swift.framework.plugin.resolver.mvc;

import org.springframework.web.bind.support.WebArgumentResolver;

import com.liepin.swift.framework.plugin.AbstractClassScan;

@Deprecated
public class WebArgumentResolverClassScan extends AbstractClassScan {

    @Override
    public String path() {
        return "com.liepin.**.resolver.**";
    }

    @Override
    public boolean conventional(Class<?> clazz) {
        if (!WebArgumentResolver.class.isAssignableFrom(clazz)) {
            return false;
        }
        checkBean(clazz);
        return true;
    }

}
