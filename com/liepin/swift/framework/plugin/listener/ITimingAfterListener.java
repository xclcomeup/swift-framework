package com.liepin.swift.framework.plugin.listener;

import java.util.concurrent.TimeUnit;

/**
 * 服务启动完后监听触发器
 * <p>
 * 定期执行
 * 
 * @author yuanxl
 * @date 2015-11-3 下午06:37:24
 */
public interface ITimingAfterListener extends IAfterListener {

    /**
     * 间隔的时间周期
     * 
     * @return
     */
    public long period();

    /**
     * 时间单位
     * 
     * @return
     */
    public TimeUnit timeUnit();

}
