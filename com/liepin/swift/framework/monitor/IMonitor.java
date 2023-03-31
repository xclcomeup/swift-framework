package com.liepin.swift.framework.monitor;

import com.liepin.swift.core.annotation.SwiftInterface;

/**
 * 服务内部控制管理接口
 * 
 * @author yuanxl
 * 
 */
public interface IMonitor {

    /**
     * 取JVM信息
     * 
     * @return
     */
    @SwiftInterface
    public String jvm();

    @SwiftInterface
    public boolean http();

    /**
     * eventinfo日志管理
     * <p>
     * 动态控制接口日志大小缩放
     * 
     * @param actionPath
     * @param fullPrint
     */
    @SwiftInterface
    public void eventinfoManage(String actionPath, String fullPrint);

    /**
     * 
     * @param threshold
     * @param count
     */
    @SwiftInterface
    public String topThread(float threshold, int count);

    /**
     * 
     * @param lineBreak
     * @return
     */
    @SwiftInterface
    public String thread(String lineBreak);

    @SwiftInterface
    public String env();

    @SwiftInterface
    public String dependency();

    @SwiftInterface
    public String search(String jarName, boolean containJar, String[] texts);

}
