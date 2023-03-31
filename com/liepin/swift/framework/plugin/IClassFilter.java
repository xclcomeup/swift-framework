package com.liepin.swift.framework.plugin;

public interface IClassFilter extends IScanFilter {

    /**
     * 检查是否符合
     * 
     * @param clazz
     * @return
     */
    boolean test(Class<?> clazz);

    /**
     * 扫描包路径
     * 
     * @return
     */
    String path();

}
