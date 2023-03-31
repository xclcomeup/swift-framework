package com.liepin.swift.framework.monitor.jvm;

import java.util.LinkedHashMap;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.monitor.BufferPoolMemorySnapshot;
import com.liepin.common.monitor.JVMMonitor;
import com.liepin.common.monitor.MemorySnapshot;
import com.liepin.common.monitor.RuntimeSnapshot;
import com.liepin.common.monitor.SystemSnapshot;
import com.liepin.common.monitor.ThreadSnapshot;
import com.liepin.common.other.DateUtil;
import com.liepin.common.other.ThreadUtil;
import com.liepin.swift.framework.monitor.AbstractLogPlugin;

/**
 * 规范线程名 swift-monitor-<br>
 * 
 * @author yuanxl
 * @date 2016-10-31 下午03:38:46
 */
public class JvmCollecter extends AbstractLogPlugin {

    private static final int DEFAULT_CPU_USAGE_RATE = 200;
    private volatile int cpuUsageRate = DEFAULT_CPU_USAGE_RATE;

    private JvmCollecter() {
        super();
    }

    private static JvmCollecter instance = new JvmCollecter();

    public static JvmCollecter getInstance() {
        return instance;
    }

    @Override
    public void onEvent() {
        String cpu = JVMMonitor.cpuSnapshot();
        SystemSnapshot systemSnapshot = JVMMonitor.systemSnapshot();
        MemorySnapshot memorySnapshot = JVMMonitor.memorySnapshot();
        ThreadSnapshot threadSnapshot = JVMMonitor.threadSnapshot();
        RuntimeSnapshot runtimeSnapshot = JVMMonitor.getRuntimeSnapshot();
        BufferPoolMemorySnapshot bufferPoolMemorySnapshot = JVMMonitor.bufferPoolMemorySnapshot();

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("CpuSnapshot", cpu);
        map.put("MemorySnapshot", memorySnapshot);
        map.put("BufferPoolMemorySnapshot", bufferPoolMemorySnapshot);
        map.put("ThreadSnapshot", threadSnapshot);
        map.put("SystemSnapshot", systemSnapshot);
        map.put("RuntimeSnapshot", runtimeSnapshot);
        map.put("ClientId", ProjectId.getClientId());
        map.put("Pod", SystemUtil.getPod());
        map.put("Time", DateUtil.getCurrentDateTime());

        float cpuRate = 0;
        if (cpu.endsWith("%")) {
            cpuRate = Float.parseFloat(cpu.substring(0, cpu.length() - 1));
        }
        if (cpuRate > cpuUsageRate) {
            String topCpuThreadSnapshot = JVMMonitor.topCpuThreadSnapshot(cpuUsageRate * 0.6f, 20);
            map.put("TopCpuThreadSnapshot", topCpuThreadSnapshot);
        }

        StringBuilder out = new StringBuilder();
        Map<String, Integer> classify = ThreadUtil.snapshotWithClassification("\n", out);
        map.put("ThreadClassification", classify);
        String json = JsonUtil.toJson(map);
        log(json);
    }

    @Override
    public String category() {
        return "jvm";
    }

    @Override
    public String zkListenPath() {
        return "/common/monitor/jvm";
    }

    @Override
    public boolean timer() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handle(Map<String, Object> map) {
        super.handle(map);
        if (isEnable()) {
            int cpuUsageRate = getCpuUsageRate(map, DEFAULT_CPU_USAGE_RATE);
            Map<String, Object> custom = (Map<String, Object>) map.get(ProjectId.getProjectName());
            if (custom != null) {
                cpuUsageRate = getCpuUsageRate(custom, cpuUsageRate);
            }
            this.cpuUsageRate = cpuUsageRate;
        }
    }

    private int getCpuUsageRate(Map<String, Object> map, int defaultValue) {
        Integer customCpuUsageRate = (Integer) map.get("cpuUsageRate");
        return (customCpuUsageRate != null) ? customCpuUsageRate.intValue() : defaultValue;
    }

}
