package com.liepin.swift.framework.plugin.gateway;

import com.liepin.swift.framework.plugin.IClassFilter;

/**
 * Gateway层接口扫描器
 * 
 * @author yuanxl
 * 
 */
public class GatewayClassFilter implements IClassFilter {

    @Override
    public String path() {
        return "com.liepin.**.gateway.*";
    }

    @Override
    public boolean test(Class<?> clazz) {
        return clazz.isInterface() ? true : false;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

}
