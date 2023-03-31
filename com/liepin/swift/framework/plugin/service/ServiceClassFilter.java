package com.liepin.swift.framework.plugin.service;

import com.liepin.swift.core.annotation.SwiftService;
import com.liepin.swift.framework.plugin.IClassFilter;

/**
 * Service层接口扫描器
 * 
 * @author yuanxl
 * 
 */
public class ServiceClassFilter implements IClassFilter {

    @Override
    public String path() {
        return "com.liepin.**.service.*";
    }

    @Override
    public boolean test(Class<?> clazz) {
        return clazz.getAnnotation(SwiftService.class) != null;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

}
