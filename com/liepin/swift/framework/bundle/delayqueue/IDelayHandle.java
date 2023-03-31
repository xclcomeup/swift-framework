package com.liepin.swift.framework.bundle.delayqueue;

/**
 * 延迟事件处理器
 * 
 * @author yuanxl
 *
 */
public interface IDelayHandle {

    /**
     * 事件回调方法
     * 
     * @param element
     */
    public void entry(String element);

}
