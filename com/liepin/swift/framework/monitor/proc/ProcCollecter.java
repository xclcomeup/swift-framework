package com.liepin.swift.framework.monitor.proc;

import java.util.LinkedHashMap;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.monitor.JVMMonitor;
import com.liepin.common.other.DateUtil;
import com.liepin.swift.framework.monitor.AbstractLogPlugin;
import com.liepin.swift.framework.monitor.proc.fd.ProcForFd;
import com.liepin.swift.framework.monitor.proc.io.IoBean;
import com.liepin.swift.framework.monitor.proc.io.ProcForIo;

public class ProcCollecter extends AbstractLogPlugin {

    private static ProcCollecter instance = new ProcCollecter();

    private ProcCollecter() {
        super();
    }

    public static ProcCollecter getInstance() {
        return instance;
    }

    @Override
    public String category() {
        return "proc";
    }

    @Override
    public String zkListenPath() {
        return "/common/monitor/proc";
    }

    @Override
    public boolean timer() {
        return (isMac()) ? false : true;
    }

    @Override
    protected boolean getEnable(Map<String, Object> map) {
        return SystemUtil.isLinux() && super.getEnable(map);
    }

    @Override
    public void onEvent() {
        String pid = JVMMonitor.getProcessId();
        int fdNum = ProcForFd.print(pid);
        IoBean iobean = ProcForIo.print(pid);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("ClientId", ProjectId.getClientId());
        map.put("Pod", SystemUtil.getPod());
        map.put("Time", DateUtil.getCurrentDateTime());
        map.put("fd", fdNum);
        map.put("io", iobean);
        String json = JsonUtil.toJson(map);
        log(json);
    }

}
