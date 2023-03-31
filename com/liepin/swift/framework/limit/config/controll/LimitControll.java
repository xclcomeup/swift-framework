package com.liepin.swift.framework.limit.config.controll;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.framework.limit.config.EnumLimitRuleConfig;

public class LimitControll {

    private boolean enable;

    private String url;

    private EnumLimitRuleConfig enumLimitRuleConfig;

    public LimitControll(Map<String, Object> data, EnumLimitRuleConfig enumLimitRuleConfig) {
        Boolean enableObj = (Boolean) data.get("enable");
        this.enable = (enableObj != null) ? enableObj.booleanValue() : true;
        this.url = (String) data.get("url");
        this.enumLimitRuleConfig = enumLimitRuleConfig;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public EnumLimitRuleConfig getEnumLimitRuleConfig() {
        return enumLimitRuleConfig;
    }

    public void setEnumLimitRuleConfig(EnumLimitRuleConfig enumLimitRuleConfig) {
        this.enumLimitRuleConfig = enumLimitRuleConfig;
    }

    @SuppressWarnings("unchecked")
    public int getQps(Map<String, Object> data) {
        Map<String, Object> limitRule = (Map<String, Object>) data.get("limitRule");
        return (int) limitRule.get(enumLimitRuleConfig.name());
    }

    @SuppressWarnings("unchecked")
    public Pair<Set<String>, Set<String>> getClientIds(Map<String, Object> data) {
        Map<String, Object> valueMap = (Map<String, Object>) data.get("clientId");
        Set<String> initClientIdSet = null;
        Set<String> lastClientIdSet = null;
        List<String> initClientIdList = (List<String>) valueMap.get("initClientId");
        if (initClientIdList != null && !initClientIdList.isEmpty()) {
            initClientIdSet = new HashSet<>(initClientIdList);
        }
        List<String> lastClientIdList = (List<String>) valueMap.get("lastClientId");
        if (lastClientIdList != null && !lastClientIdList.isEmpty()) {
            lastClientIdSet = new HashSet<>(lastClientIdList);
        }
        return new Pair<Set<String>, Set<String>>(initClientIdSet, lastClientIdSet);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getOriginalIps(Map<String, Object> data) {
        Set<String> originalIpSet = null;
        List<String> originalIpList = (List<String>) data.get("originalIp");
        if (originalIpList != null && !originalIpList.isEmpty()) {
            originalIpSet = new HashSet<>(originalIpList);
        } else {
            originalIpSet = new HashSet<>();
        }
        return originalIpSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((enumLimitRuleConfig == null) ? 0 : enumLimitRuleConfig.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LimitControll other = (LimitControll) obj;
        if (enumLimitRuleConfig != other.enumLimitRuleConfig)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

}
