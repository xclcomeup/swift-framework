package com.liepin.swift.framework.monitor;

import com.liepin.common.monitor.JVMMonitor;

public class HttpMonitorService extends AbstractMonitor implements IHttpMonitor {

    @Override
    public boolean isFlow() {
        int cnt = JVMMonitor.tomcatRunningThreadCnt();
        return ((cnt == 0) ? Boolean.FALSE : Boolean.TRUE);
    }

}
