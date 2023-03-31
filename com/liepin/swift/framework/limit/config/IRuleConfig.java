package com.liepin.swift.framework.limit.config;

import java.util.Map;

import com.liepin.swift.framework.limit.config.controll.LimitControll;

public interface IRuleConfig {

    public String name();

    public LimitControll read(Map<String, Object> data);

}
