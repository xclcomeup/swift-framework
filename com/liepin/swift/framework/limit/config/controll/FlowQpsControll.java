package com.liepin.swift.framework.limit.config.controll;

import java.util.Map;

import com.liepin.swift.framework.limit.config.EnumLimitRuleConfig;

/**
 * 针对接口访问限流配置
 * 
 * @author yuanxl
 *
 */
public class FlowQpsControll extends LimitControll {

    private int qps;

    public FlowQpsControll(Map<String, Object> data, EnumLimitRuleConfig enumLimitRuleConfig) {
        super(data, enumLimitRuleConfig);
        this.qps = getQps(data);
    }

    public int getQps() {
        return qps;
    }

    public void setQps(int qps) {
        this.qps = qps;
    }

    @Override
    public String toString() {
        return "FlowQpsControll [qps=" + qps + ", enable=" + isEnable() + ", url=" + getUrl() + ", enumLimitRuleConfg="
                + getEnumLimitRuleConfig() + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
