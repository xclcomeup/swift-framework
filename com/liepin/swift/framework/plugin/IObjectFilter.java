package com.liepin.swift.framework.plugin;

import com.liepin.swift.framework.util.DependencyUtil;
import com.liepin.swift.framework.util.ObjectUtil;

public interface IObjectFilter extends IScanFilter {

    /**
     * 检查是否符合
     * 
     * @param o
     * @return
     */
    boolean test(Object o);

    /**
     * 是否只扫描com.liepin的包
     * <p>
     * 默认：是
     * 
     * @return
     */
    default boolean isOnlyComPinPackage() {
        return true;
    }

    /**
     * 包路径是否在com.liepin下
     * <p>
     * 默认：是
     * 
     * @param object
     * @return
     */
    default boolean isOnlyComPinPackage(Object object) {
        return isOnlyComPinPackage() ? object.getClass().getName().startsWith("com.liepin.") : true;
    }

    /**
     * 扫描包是否包含jar包扩展的
     * <p>
     * 默认包括jar包<br>
     * 
     * @param object
     * @return
     */
    default boolean isContainJar(Object object) {
        Object actual = ObjectUtil.getActual(object);
        return isContainJar() ? true : (DependencyUtil.isInJar(actual.getClass()) ? false : true);
    }

}
