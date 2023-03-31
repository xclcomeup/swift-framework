package com.liepin.swift.framework.security.frameOptions;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.security.ISecurityBuilder;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class FrameOptionsBuilder implements ISecurityBuilder {

    @Override
    public boolean build(HttpSecurity http) throws Exception {
        Switcher switcher = new Switcher("/common/protect/frameOptions");
        if (switcher.getEnable(ProjectId.getClientId())) {
            // 开关打开才生效
            // 读取配置
            Map<String, Object> defaultConfig = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/protect/frameOptions");
            Map<String, Object> myselfConfig = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/protect/frameOptions/" + ProjectId.getClientId());
            XFrameOptionsMode mode = getMode(defaultConfig, myselfConfig);
            if (XFrameOptionsMode.SAMEORIGIN == mode) {
                http.headers().frameOptions().sameOrigin();
            } else if (XFrameOptionsMode.DENY == mode) {
                http.headers().frameOptions().deny();
            }
            return true;
        }
        return false;
    }

    private XFrameOptionsMode getMode(Map<String, Object> defaultConfig, Map<String, Object> myselfConfig) {
        String mode = (String) Optional.ofNullable(myselfConfig).map(t -> t.get("mode")).orElseGet(() -> {
            return Optional.ofNullable(defaultConfig).map(z -> z.get("defaultMode")).orElse(null);
        });
        return XFrameOptionsMode.valueOf(mode.toUpperCase());
    }

}
