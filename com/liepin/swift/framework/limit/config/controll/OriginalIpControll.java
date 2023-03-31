package com.liepin.swift.framework.limit.config.controll;

import java.util.Map;
import java.util.Set;

import com.liepin.swift.framework.limit.config.EnumLimitRuleConfig;

public class OriginalIpControll extends LimitControll {

    private Set<String> originalIpSet;;

    public OriginalIpControll(Map<String, Object> data, EnumLimitRuleConfig enumLimitRuleConfig) {
        super(data, enumLimitRuleConfig);
        this.originalIpSet = getOriginalIps(data);
    }
    
    @Override
    public String toString() {
        return "OriginalIpControll [originalIpSet=" + originalIpSet + ", enable=" + isEnable() + ", url=" + getUrl()
                + ", enumLimitRuleConfig=" + getEnumLimitRuleConfig() + "]";
    }

}
