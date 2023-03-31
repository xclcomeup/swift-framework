package com.liepin.swift.framework.limit.config.controll;

import java.util.Map;

import com.liepin.swift.framework.limit.config.EnumLimitRuleConfig;

public class FlowCurrentUserIdQpsControll extends FlowQpsControll {

    // TODO currentUserId 
    // 保存多少userId的限流信息

    public FlowCurrentUserIdQpsControll(Map<String, Object> data, EnumLimitRuleConfig enumLimitRuleConfig) {
        super(data, enumLimitRuleConfig);
    }

    // toString

}
