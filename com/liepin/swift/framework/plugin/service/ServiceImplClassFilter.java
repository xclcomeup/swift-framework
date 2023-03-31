package com.liepin.swift.framework.plugin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liepin.swift.framework.plugin.IClassFilter;

public class ServiceImplClassFilter implements IClassFilter {

    /**
     * 接口class => 接口 impl class
     */
    private Map<Class<?>, List<Class<?>>> interfaceClassImplMap = new HashMap<Class<?>, List<Class<?>>>();

    public ServiceImplClassFilter(List<Class<?>> list) {
        for (Class<?> interfaceClass : list) {
            interfaceClassImplMap.put(interfaceClass, new ArrayList<Class<?>>());
        }
    }

    @Override
    public String path() {
        return "com.liepin.**.service.impl.*";
    }

    @Override
    public boolean test(Class<?> clazz) {
        if (!clazz.isInterface()) {
            for (Map.Entry<Class<?>, List<Class<?>>> entry : interfaceClassImplMap.entrySet()) {
                Class<?> interfaceClass = entry.getKey();
                if (interfaceClass.isAssignableFrom(clazz)) {
                    entry.getValue().add(clazz);
                }
            }
            // 多个接口有同一个实现类
            return true;
        }
        return false;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

    /**
     * 返回接口类与实现类对应关系
     * 
     * @return
     */
    public Map<Class<?>, List<Class<?>>> getImpl() {
        // super.scan();
        return interfaceClassImplMap;
    }

}
