package com.liepin.swift.framework.plugin.listener;

/**
 * 服务启动完后监听触发器
 * <p>
 * 同步执行
 * 
 * @author yuanxl
 * @date 2015-11-3 下午06:37:24
 */
public interface IAfterListener {

    /**
     * 触发方法
     */
    public void onApplicationEvent();

}
