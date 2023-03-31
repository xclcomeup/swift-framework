package com.liepin.swift.framework.boot.tomcat.initializer;

import com.liepin.swift.framework.conf.SwiftConfig;

/**
 * tomcat Executor配置
 * 
 * @author yuanxl
 *
 */
public class ExecutorConfig extends SwiftConfig {

    private String name;
    private String namePrefix;
    private int maxThreads;
    private int minSpareThreads;
    private int maxIdleTime;

    public ExecutorConfig() {
        this.name = getValue("tomcat.executor.name", "tomcatThreadPool");
        this.namePrefix = getValue("tomcat.executor.namePrefix", "catalina-exec-");
        this.maxThreads = getIntValue("tomcat.executor.maxThreads", 3000);
        this.minSpareThreads = (SwiftConfig.enableStartupPreload())
                ? getIntValue("tomcat.executor.minSpareThreads", 100)
                : getIntValue("tomcat.executor.minSpareThreads", 5);// 为了启动提速，线下环境初始化少点线程
        this.maxIdleTime = getIntValue("tomcat.executor.maxIdleTime", 60000);
    }

    public String getName() {
        return name;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

}
