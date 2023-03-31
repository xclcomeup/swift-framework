package com.liepin.swift.framework.log.initializer;

import com.liepin.swift.framework.log.PidLog;
import com.liepin.swift.framework.log.log4j.Log4jContext;
import com.liepin.swift.framework.monitor.cross.CallFailureCollecter;

/**
 * 雨燕框架日志初始化类
 * 
 * @author yuanxl
 *
 */
public class SwiftLogInitializer {

    private static long initTime;

    /**
     * 初始化
     */
    public static void initialize() {
        initTime = System.currentTimeMillis();
        Log4jContext.initialize();
        PidLog.logPid();
        // 初始化错误链日志目录
        CallFailureCollecter.getInstance().init();
    }

    /**
     * 启动成功标识
     * 
     * @return
     */
    public static String printSuccessMessage() {
        return "start Server startup in " + (System.currentTimeMillis() - initTime) + " ms";
    }

    /**
     * 启动失败标识
     * 
     * @return
     */
    public static String printFailMessage() {
        return "BianselongFatalError: Tomcat启动异常\n" + printSuccessMessage();
    }

}
