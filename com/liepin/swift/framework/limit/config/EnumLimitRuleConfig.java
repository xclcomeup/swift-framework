package com.liepin.swift.framework.limit.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.liepin.swift.framework.limit.config.controll.FlowClientIdQpsControll;
import com.liepin.swift.framework.limit.config.controll.FlowCurrentUserIdQpsControll;
import com.liepin.swift.framework.limit.config.controll.FlowOriginalIpQpsControll;
import com.liepin.swift.framework.limit.config.controll.FlowQpsControll;
import com.liepin.swift.framework.limit.config.controll.LimitControll;
import com.liepin.swift.framework.limit.config.controll.OriginalIpControll;

public enum EnumLimitRuleConfig implements IRuleConfig {
    /**
     * 针对接口访问限流
     */
    URL_QPS() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            return new FlowQpsControll(data, URL_QPS);
        }

    },
    /**
     * 针对接口+某个调用方访问限流
     */
    URL_CLIENTID_QPS() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            return new FlowClientIdQpsControll(data, URL_CLIENTID_QPS);
        }

    },
    /**
     * 针对接口+请求来源ip访问限流
     */
    URL_ORIGINALIP_QPS() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            return new FlowOriginalIpQpsControll(data, URL_ORIGINALIP_QPS);
        }

    },
    /**
     * 针对接口+请求用户id（currentUserId）访问限流
     */
    URL_CURRENTUSERID_QPS() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            return new FlowCurrentUserIdQpsControll(data, URL_CURRENTUSERID_QPS);
        }

    },
    /**
     * 针对接口+某个调用方白名单访问限制
     */
    URL_CLIENTID_WHITELIST() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            // FIXME
            return new FlowClientIdQpsControll(data, URL_CLIENTID_WHITELIST);
        }

    },
    /**
     * 针对接口+某个调用方黑名单访问限制
     */
    URL_CLIENTID_BLACKLIST() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            // FIXME
            return new FlowClientIdQpsControll(data, URL_CLIENTID_BLACKLIST);
        }

    },
    /**
     * 针对接口+请求来源ip访问限制
     */
    URL_ORIGINALIP_BLACKLIST() {

        @Override
        public LimitControll read(Map<String, Object> data) {
            return new OriginalIpControll(data, URL_ORIGINALIP_BLACKLIST);
        }

    };
    // FIXME 针对某个调用方黑白名单访问限制

    public static EnumLimitRuleConfig of(String name) {
        Optional<EnumLimitRuleConfig> optional = Arrays.stream(EnumLimitRuleConfig.values())
                .filter(t -> t.name().equals(name)).findFirst();
        return optional.get();
    }

}
