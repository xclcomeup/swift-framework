package com.liepin.swift.framework.security.xxp;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.security.ISecurityBuilder;

public class XxpBuilder implements ISecurityBuilder {

    @Override
    public boolean build(final HttpSecurity http) throws Exception {
        Switcher switcher = new Switcher("/common/protect/xss/X-XSS-Protection");
        if (switcher.getEnable(ProjectId.getClientId())) {
            // 开关打开才生效
            http.headers().xssProtection().xssProtectionEnabled(true).block(true);
            return true;
        }
        return false;
    }

}
