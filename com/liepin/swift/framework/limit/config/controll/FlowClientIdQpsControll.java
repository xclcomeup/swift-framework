package com.liepin.swift.framework.limit.config.controll;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.framework.limit.config.EnumLimitRuleConfig;

public class FlowClientIdQpsControll extends FlowQpsControll {

    private Set<String> initClientIdSet;
    private Set<String> lastClientIdSet;

    public FlowClientIdQpsControll(Map<String, Object> data, EnumLimitRuleConfig enumLimitRuleConfig) {
        super(data, enumLimitRuleConfig);
        Pair<Set<String>, Set<String>> pair = getClientIds(data);
        this.initClientIdSet = (pair.getFirst() != null) ? pair.getFirst() : new HashSet<>();
        this.lastClientIdSet = (pair.getSecond() != null) ? pair.getSecond() : new HashSet<>();
    }

    public Set<String> getInitClientIdSet() {
        return initClientIdSet;
    }

    public void setInitClientIdSet(Set<String> initClientIdSet) {
        this.initClientIdSet = initClientIdSet;
    }

    public Set<String> getLastClientIdSet() {
        return lastClientIdSet;
    }

    public void setLastClientIdSet(Set<String> lastClientIdSet) {
        this.lastClientIdSet = lastClientIdSet;
    }

    @Override
    public String toString() {
        return "FlowClientIdQpsControll [initClientIdSet=" + initClientIdSet + ", lastClientIdSet=" + lastClientIdSet
                + ", enable=" + isEnable() + ", url=" + getUrl() + ", enumLimitRuleConfig=" + getEnumLimitRuleConfig()
                + "]";
    }

}
