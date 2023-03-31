package com.liepin.swift.framework.plugin;

public interface IScanFilter {

    /**
     * 扫描包是否包含jar包扩展的
     * <p>
     * 默认包括jar包<br>
     * 
     * @return
     */
    default boolean isContainJar() {
        return true;
    }

    /**
     * 排除扫描包路径
     * 
     * @return
     */
    default String excludePath() {
        return null;
    }

}
