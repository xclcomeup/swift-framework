package com.liepin.swift.framework.monitor.call;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.json.JsonUtil;
import com.liepin.swift.framework.monitor.AbstractLogPlugin;

/**
 * 重复调用监控
 * 
 * @author yuanxl
 * @date 2015-8-24 下午04:06:21
 */
public class RepeatCallCollecter extends AbstractLogPlugin {

    private static final boolean DEFAULT_ADVANCED = false;
    private static final int DEFAULT_PRINT_NUMBER_LINES = 1;
    /**
     * false：粒度到接口<br>
     * true: 粒度到接口参数<br>
     */
    private volatile boolean advanced = DEFAULT_ADVANCED;

    private volatile int numberLines = DEFAULT_PRINT_NUMBER_LINES;

    private static final ThreadLocal<CallTranscation> callTranscationThreadLocal = new ThreadLocal<CallTranscation>() {

        protected CallTranscation initialValue() {
            return new CallTranscation();
        };

    };

    private RepeatCallCollecter() {
        super();
    }

    private static RepeatCallCollecter instance = new RepeatCallCollecter();

    public static RepeatCallCollecter getInstance() {
        return instance;
    }

    private CallTranscation get() {
        return callTranscationThreadLocal.get();
    }

    public void beginTransaction(String name) {
        if (!isEnable()) {
            return;
        }
        get().start(name);
    }

    public void collect(String appName, String methodName, LinkedHashMap<String, Object> data) {
        if (!isEnable()) {
            return;
        }

        Thread t = Thread.currentThread();
        // 只监控tomcat线程
        if (!t.getName().startsWith("catalina-exec-")) {
            return;
        }

        CallTranscation transcation = get();
        if (transcation.getUrl() == null) {
            return;
        }
        if (isAdvanced()) {
            transcation.log(appName, methodName, numberLines, data);
        } else {
            transcation.log(appName, methodName, numberLines);
        }
    }

    public void endTransaction() {
        if (!isEnable()) {
            return;
        }
        CallTranscation transcation = get();
        String url = transcation.getUrl();
        String time = transcation.getTime();
        List<RepeatCallBean> list = transcation.end(isAdvanced());
        if (list != null && list.size() > 0) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("ClientId", ProjectId.getClientId());
            map.put("Time", time);
            map.put("Url", url);
            map.put("Stack", list);
            String json = JsonUtil.toJson(map);
            log(json);
        }
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public int getNumberLines() {
        return numberLines;
    }

    @Override
    public String category() {
        return "repeatcall";
    }

    @Override
    public String zkListenPath() {
        return "/common/monitor/repeatCall";
    }

    @Override
    public boolean timer() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handle(Map<String, Object> map) {
        super.handle(map);
        if (isEnable()) {
            boolean advanced = getAdvanced(map, DEFAULT_ADVANCED);
            int numberLines = getNumberLines(map, DEFAULT_PRINT_NUMBER_LINES);
            Map<String, Object> custom = (Map<String, Object>) map.get(ProjectId.getProjectName());
            if (custom != null) {
                advanced = getAdvanced(custom, advanced);
                numberLines = getNumberLines(custom, numberLines);
            }
            this.advanced = advanced;
            this.numberLines = numberLines;
        }
    }

    private boolean getAdvanced(Map<String, Object> map, boolean defaultValue) {
        Boolean advanced = (Boolean) map.get("advanced");
        return (advanced != null) ? advanced.booleanValue() : defaultValue;
    }

    private int getNumberLines(Map<String, Object> map, int defaultValue) {
        Integer numberLines = (Integer) map.get("numberLines");
        return (numberLines != null) ? numberLines.intValue() : defaultValue;
    }

}
