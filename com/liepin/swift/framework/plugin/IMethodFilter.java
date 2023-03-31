package com.liepin.swift.framework.plugin;

import java.lang.reflect.Method;

public interface IMethodFilter extends IScanFilter {

    /**
     * 检查是否符合
     * 
     * @param m
     * @return
     */
    boolean test(Method m);

    /**
     * 扫描包路径
     * 
     * @return
     */
    String path();

}
