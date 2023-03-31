package com.liepin.swift.framework.monitor.cat;

import com.dianping.cat.Cat;

/**
 * cat上下文管理
 * 
 * @author yuanxl
 *
 */
public final class CatContext {

    /**
     * 创造资源
     */
    public static void initialize() {
        // 预加载Cat
        Cat.getManager();
    }

    /**
     * 回收资源
     */
    public static void destory() {
        // 释放Cat
        Cat.destroy();
    }

}
