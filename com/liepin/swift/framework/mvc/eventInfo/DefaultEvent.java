package com.liepin.swift.framework.mvc.eventInfo;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.liepin.common.json.JsonUtil;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.util.ThreadLocalUtil;

public class DefaultEvent implements Event {

    private static final Set<String> PARAMKEY_ORDER = new LinkedHashSet<String>();

    static {
        PARAMKEY_ORDER.add(Const.CLIENT_IDS);
        PARAMKEY_ORDER.add(Const.CURRENT_USER_ID);
        PARAMKEY_ORDER.add(Const.ORIGINAL_IP);
        PARAMKEY_ORDER.add(Const.TRACEID);
        PARAMKEY_ORDER.add(Const.FLOW_GRAY_ID);
        PARAMKEY_ORDER.add(Const.INITIATE_URL);
        PARAMKEY_ORDER.add(Const.AREA);
        PARAMKEY_ORDER.add(Const.ROOT_DOMAIN);
        PARAMKEY_ORDER.add(Const.TRANSMIT_EXTEND);
        PARAMKEY_ORDER.add(Const.VERSION);
        PARAMKEY_ORDER.add(Const.TIME_RIVER);
        PARAMKEY_ORDER.add(Const.GW_CLIENT_INFO);
    }

    private String type;
    private String name;
    private String status;
    private long eclipse;
    private String actionPath;
    private String clientIP;
    private Date start;
    private String input = "{}";
    private String output = "{}";

    @Override
    public long getEclipse() {
        return eclipse;
    }

    @Override
    public void begin() {
        this.start = new Date();
    }

    @Override
    public Date getStart() {
        return start;
    }

    @Override
    public void submit() {
        this.eclipse = System.currentTimeMillis() - this.start.getTime();
    }

    @Override
    public String getActionPath() {
        return actionPath;
    }

    @Override
    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    @Override
    public String getClientIP() {
        return clientIP;
    }

    @Override
    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getInput() {
        return input;
    }

    @Override
    public void setInput(Map<String, Object> input) {
        Optional.ofNullable(input).ifPresent(t -> {
            // 不打印biInfo和cat数据
            t.remove(Const.BI_INFO);
            t.remove(Const.CAT);

            // 业务过程中输出的，需要添加进去。输出currentUserId、transmitExtend
            if (!t.containsKey(Const.CURRENT_USER_ID)) {
                t.put(Const.CURRENT_USER_ID, ThreadLocalUtil.getInstance().getCurrentUserId());
            }
            if (!t.containsKey(Const.TRANSMIT_EXTEND)) {
                t.put(Const.TRANSMIT_EXTEND, ThreadLocalUtil.getInstance().getExtend());
            }

            // 参数排序
            if (t.size() > 1) {
                t = sortInputParams(t);
            }

            this.input = JsonUtil.toJson(t);
        });
    }

    @Override
    public void setInput(String value) {
        if (Objects.nonNull(value)) {
            this.input = value;
        }
    }

    @Override
    public String getOutput() {
        return output;
    }

    @Override
    public void setOutput(String output) {
        if (Objects.nonNull(output)) {
            this.output = output;
        }
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setType(String value) {
        this.type = value;
    }

    @Override
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String toString() {
        return "EventInfo [type=" + type + ", name=" + name + ", status=" + status + ", eclipse=" + eclipse
                + ", actionPath=" + actionPath + ", clientIP=" + clientIP + ", start=" + start + ", input=" + input
                + ", output=" + output + "]";
    }

    private static Map<String, Object> sortInputParams(Map<String, Object> map) {
        Map<String, Object> sortMap = new LinkedHashMap<String, Object>();
        if (map != null && map.size() > 0) {
            for (String key : PARAMKEY_ORDER) {
                Object value = map.get(key);
                if (value != null) {
                    sortMap.put(key, value);
                }
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (PARAMKEY_ORDER.contains(entry.getKey())) {
                    continue;
                }
                sortMap.put(entry.getKey(), entry.getValue());
            }
        }
        return sortMap;
    }

}
