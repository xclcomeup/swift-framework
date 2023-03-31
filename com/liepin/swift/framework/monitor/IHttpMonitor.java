package com.liepin.swift.framework.monitor;

import com.liepin.swift.core.annotation.SwiftInterface;

public interface IHttpMonitor extends IMonitor {

    /**
     * 判断tomcat流量接口
     * <p>
     * 有流量：true<br>
     * 无流量：false<br>
     * 
     * @return
     */
    @SwiftInterface
    public boolean isFlow();

}
