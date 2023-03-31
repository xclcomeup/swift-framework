package com.liepin.swift.framework.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.monitor.JVMMonitor;
import com.liepin.common.monitor.MemorySnapshot;
import com.liepin.common.monitor.SystemSnapshot;
import com.liepin.common.monitor.ThreadSnapshot;
import com.liepin.common.other.ThreadUtil;
import com.liepin.swift.framework.monitor.dependency.JarDependency;
import com.liepin.swift.framework.monitor.heartbeat.HttpHeartbeatManager;
import com.liepin.swift.framework.monitor.search.WorkspaceSearch;
import com.liepin.swift.framework.mvc.eventInfo.AbstractIOLogger;
import com.liepin.swift.framework.util.Pair;

public class AbstractMonitor implements IMonitor {

    @Override
    public String jvm() {
        String cpu = JVMMonitor.cpuSnapshot();
        SystemSnapshot systemSnapshot = JVMMonitor.systemSnapshot();
        MemorySnapshot memorySnapshot = JVMMonitor.memorySnapshot();
        ThreadSnapshot threadSnapshot = JVMMonitor.threadSnapshot();

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("CpuSnapshot", cpu);
        map.put("MemorySnapshot", memorySnapshot);
        map.put("ThreadSnapshot", threadSnapshot);
        map.put("SystemSnapshot", systemSnapshot);
        map.put("ClientId", ProjectId.getClientId());

        return JsonUtil.toJson(map);
    }

    @Override
    public boolean http() {
        return HttpHeartbeatManager.get().http();
    }

    @Override
    public void eventinfoManage(String actionPath, String fullPrint) {
        if (actionPath == null || actionPath.trim().length() == 0) {
            return;
        }
        AbstractIOLogger.configFullLog(actionPath, Boolean.parseBoolean(fullPrint));
    }

    @Override
    public String topThread(float threshold, int count) {
        return JVMMonitor.topCpuThreadSnapshot(threshold, count);
    }

    @Override
    public String thread(String lineBreak) {
        StringBuilder snapshot = ThreadUtil.snapshot(lineBreak);
        return snapshot.toString();
    }

    @Override
    public String env() {
        return JVMMonitor.getSystemEnv();
    }

    /**
     * 获取项目jar包依赖关系
     * <p>
     * 返回的数据格式如下：<br>
     * {<br>
     * &nbsp;&nbsp; "parent": "ins-liepin-parent:0.10.0",<br>
     * &nbsp;&nbsp; "jars": [<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "name": "poi-3.9.jar",<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "md5": ""<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; }<br>
     * &nbsp;&nbsp; ],<br>
     * &nbsp;&nbsp; "error": ""<br>
     * }<br>
     */
    @Override
    public String dependency() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        JarDependency jarDependency = new JarDependency();
        try {
            jarDependency.collect();
        } catch (Exception e) {
            data.put("error", e.getMessage());
        }
        data.put("parent", jarDependency.getParentArtifactId() + ":" + jarDependency.getParentVersion());
        List<Map<String, Object>> jars = new ArrayList<Map<String, Object>>();
        for (Pair<String, String> pair : jarDependency.getJars()) {
            Map<String, Object> jar = new LinkedHashMap<String, Object>();
            jar.put("name", pair.getFirst());
            jar.put("md5", pair.getSecond());
            jars.add(jar);
        }
        data.put("jars", jars);
        return JsonUtil.toJson(data);
    }

    @Override
    public String search(String jarName, boolean containJar, String[] texts) {
        WorkspaceSearch workspaceSearch = new WorkspaceSearch();
        workspaceSearch.setJarName(jarName);
        workspaceSearch.setContainJarPackage(containJar);
        workspaceSearch.setSearchText(texts);
        workspaceSearch.search();

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Map<String, List<String>> classResult = workspaceSearch.analysisClassResult();
        Map<String, List<String>> staticResult = workspaceSearch.analysisStaticResult();
        data.put("class", classResult);
        data.put("static", staticResult);
        if (containJar) {
            Map<String, Map<String, List<String>>> jarResult = workspaceSearch.analysisJarResult();
            data.put("jar", jarResult);
        }
        return JsonUtil.toJson(data);
    }

}
