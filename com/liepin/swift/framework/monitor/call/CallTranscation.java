package com.liepin.swift.framework.monitor.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.common.other.DateUtil;

public final class CallTranscation {

    private String url;
    private String time;

    private static final int PROTECT_LIMIT = 1000;

    // 项目名->接口名->堆栈快照
    private final Map<String, Map<String, List<String>>> simple = new HashMap<String, Map<String, List<String>>>();
    // 项目名->接口名->请求参数->堆栈快照
    private final Map<String, Map<String, Map<String, List<String>>>> complex = new HashMap<String, Map<String, Map<String, List<String>>>>();

    public void start(String url) {
        this.url = url;
        this.time = DateUtil.getCurrentDateTime();
    }

    public void log(String appName, String interfaceName, int numberLines) {
        Map<String, List<String>> interfaceMap = simple.get(appName);
        if (interfaceMap == null) {
            simple.put(appName, interfaceMap = new HashMap<String, List<String>>());
        }
        List<String> threadDumpList = interfaceMap.get(interfaceName);
        if (threadDumpList == null) {
            interfaceMap.put(interfaceName, threadDumpList = new ArrayList<String>());
        }
        // 防大循环内存占用过多，忽略超过阀值的数据
        if (threadDumpList.size() >= PROTECT_LIMIT) {
            return;
        }
        String trace = printStackTraceMgr(numberLines);
        threadDumpList.add(trace);
    }

    public void log(String appName, String interfaceName, int numberLines, LinkedHashMap<String, Object> data) {
        if (data == null) {
            return;
        }
        String paramString = data.toString();

        Map<String, Map<String, List<String>>> interfaceMap = complex.get(appName);
        if (interfaceMap == null) {
            complex.put(appName, interfaceMap = new HashMap<String, Map<String, List<String>>>());
        }
        Map<String, List<String>> paramMap = interfaceMap.get(interfaceName);
        if (paramMap == null) {
            interfaceMap.put(interfaceName, paramMap = new HashMap<String, List<String>>());
        }
        // 防大循环内存占用过多，忽略超过阀值的数据
        if (paramMap.size() >= PROTECT_LIMIT) {
            return;
        }
        List<String> threadDumpList = paramMap.get(paramString);
        if (threadDumpList == null) {
            paramMap.put(paramString, threadDumpList = new ArrayList<String>());
        }
        // 防大循环内存占用过多，忽略超过阀值的数据
        if (threadDumpList.size() >= PROTECT_LIMIT) {
            return;
        }
        String trace = printStackTraceMgr(numberLines);
        threadDumpList.add(trace);
    }

    private String printStackTraceMgr(int lineNum) {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        StackTraceElement ste;
        StringBuilder sb = new StringBuilder();
        for (int i = 53, j = 0; i < stes.length && j < lineNum; i++) {// 6
            ste = stes[i];
            if (ste.getLineNumber() <= 0) {
                continue;
            }
            if (!ste.getClassName().startsWith("com.liepin")) {
                continue;
            }
            if (ste.getClassName().startsWith("com.liepin.swift.")
                    || ste.getClassName().startsWith("com.liepin.cache.")
                    || ste.getClassName().startsWith("com.liepin.dao.")
                    || ste.getClassName().startsWith("com.liepin.router.")) {
                continue;
            }
            sb.append("at ");
            sb.append(ste.getClassName());
            sb.append(".");
            sb.append(ste.getMethodName());
            sb.append("(");
            sb.append(ste.getFileName());
            sb.append(":");
            sb.append(ste.getLineNumber());
            sb.append(")");
            sb.append("\r\n");
            j++;
        }
        return sb.toString();
    }

    public List<RepeatCallBean> end(boolean advanced) {
        if (simple.isEmpty() && complex.isEmpty()) {
            this.url = null;
            this.time = null;
            return null;
        }
        List<RepeatCallBean> data = new ArrayList<RepeatCallBean>();
        if (advanced) {
            for (Map.Entry<String, Map<String, Map<String, List<String>>>> entry : complex.entrySet()) {
                String appName = entry.getKey();
                for (Map.Entry<String, Map<String, List<String>>> entry1 : entry.getValue().entrySet()) {
                    String interfaceName = entry1.getKey();
                    for (Map.Entry<String, List<String>> entry2 : entry1.getValue().entrySet()) {
                        String paramString = entry2.getKey();
                        List<String> stack = entry2.getValue();
                        if (stack.size() > 1) {
                            RepeatCallBean bean = new RepeatCallBean();
                            bean.setRemoteProject(appName);
                            bean.setUrl(interfaceName);
                            bean.setTrack(stack);
                            bean.setParamString(paramString);
                            data.add(bean);
                        }
                    }
                }
            }
        } else {
            for (Map.Entry<String, Map<String, List<String>>> entry : simple.entrySet()) {
                String appName = entry.getKey();
                for (Map.Entry<String, List<String>> entry1 : entry.getValue().entrySet()) {
                    String interfaceName = entry1.getKey();
                    List<String> stack = entry1.getValue();
                    if (stack.size() > 1) {
                        RepeatCallBean bean = new RepeatCallBean();
                        bean.setRemoteProject(appName);
                        bean.setUrl(interfaceName);
                        bean.setTrack(stack);
                        data.add(bean);
                    }
                }
            }
        }
        this.url = null;
        this.time = null;
        this.simple.clear();
        this.complex.clear();
        return data;
    }

    public String getUrl() {
        return url;
    }

    public String getTime() {
        return time;
    }

}
