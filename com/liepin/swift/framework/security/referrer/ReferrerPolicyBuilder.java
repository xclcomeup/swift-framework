package com.liepin.swift.framework.security.referrer;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.security.ISecurityBuilder;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class ReferrerPolicyBuilder implements ISecurityBuilder {

    @Override
    public boolean build(HttpSecurity http) throws Exception {
        Switcher switcher = new Switcher("/common/protect/referrerPolicy");
        if (switcher.getEnable(ProjectId.getClientId())) {
            // 开关打开才生效
            // 读取配置
            Map<String, Object> defaultConfig = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/protect/referrerPolicy");
            Map<String, Object> myselfConfig = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/protect/referrerPolicy/" + ProjectId.getClientId());
            ReferrerPolicy policy = getPolicy(defaultConfig, myselfConfig);
            http.headers().referrerPolicy(policy);
            return true;
        }
        return false;
    }

    private ReferrerPolicy getPolicy(Map<String, Object> defaultConfig, Map<String, Object> myselfConfig) {
        String policy = (String) Optional.ofNullable(myselfConfig).map(t -> t.get("policy")).orElseGet(() -> {
            return Optional.ofNullable(defaultConfig).map(z -> z.get("defaultPolicy")).orElse(null);
        });
        return ReferrerPolicy.get(policy.toLowerCase());
    }

}
