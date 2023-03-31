package com.liepin.swift.framework.plugin.listener;

/**
 * 服务停止前监听触发器
 * <p>
 * 同步执行<br>
 * 执行时机：在接收到停服务时立即执行，在服务注销、对象销毁之前<br>
 * 
 * @author yuanxl
 * @date 2016-12-21 下午03:05:55
 */
public interface IShutdownListener {

    /**
     * 触发方法
     */
    public void onApplicationEvent();

    /**
     * 服务停止前监听触发器执行优先级
     * <p>
     * 数字越大优先级越高
     * <p>
     * 默认同级、无序，排在最后执行
     * 
     * @return
     */
    default int priority() {
        return 0;
    }

    /**
     * 停止处理耗时最大等待时间，单位秒，默认5秒
     * 
     * @return
     */
    default int awaitSecond() {
        return -1;
    }

}
