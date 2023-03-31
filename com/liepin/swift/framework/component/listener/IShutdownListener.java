package com.liepin.swift.framework.component.listener;

/**
 * 服务停止前监听触发器
 * <p>
 * 同步执行<br>
 * 执行时机：在接收到停服务时立即执行，在服务注销、对象销毁之前<br>
 * <p>
 * 类已过时，迁移到 {@link com.liepin.swift.framework.plugin.listener.IShutdownListener}
 * 
 * @author yuanxl
 * @date 2016-12-21 下午03:05:55
 */
@Deprecated
public interface IShutdownListener extends com.liepin.swift.framework.plugin.listener.IShutdownListener {

}
