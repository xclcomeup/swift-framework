package com.liepin.swift.framework.security.csp;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.ContentSecurityPolicyConfig;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.security.ISecurityBuilder;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class CspBuilder implements ISecurityBuilder {

    @Override
    public boolean build(final HttpSecurity http) throws Exception {
        Switcher cspSwitcher = new Switcher("/common/protect/xss/Content-Security-Policy");
        if (cspSwitcher.getEnable(ProjectId.getClientId())) {
            // 开关打开才生效
            // 读取配置
            Map<String, Object> defaultConfig = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/protect/xss/Content-Security-Policy");
            Map<String, Object> myselfConfig = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(
                    EnumNamespace.PUBLIC, "/common/protect/xss/Content-Security-Policy/" + ProjectId.getClientId());
            String policyDirectives = getPolicyDirectives(defaultConfig, myselfConfig);
            if (Objects.nonNull(policyDirectives) && !"".equals(policyDirectives)) {
                String reportUri = (String) defaultConfig.get("reportUri");
                policyDirectives = (Objects.nonNull(reportUri) && !"".equals(reportUri))
                        ? (policyDirectives + " report-uri " + reportUri) : policyDirectives;
                @SuppressWarnings("rawtypes")
                ContentSecurityPolicyConfig contentSecurityPolicy = http.headers()
                        .contentSecurityPolicy(policyDirectives);
                if (getReportOnly(defaultConfig, myselfConfig)) {
                    contentSecurityPolicy.reportOnly();
                }
                return true;
            }
        }
        return false;
    }

    private String getPolicyDirectives(Map<String, Object> defaultConfig, Map<String, Object> myselfConfig) {
        return (String) Optional.ofNullable(myselfConfig).map(t -> t.get("policyDirectives")).orElseGet(() -> {
            return Optional.ofNullable(defaultConfig).map(z -> z.get("defaultPolicyDirectives")).orElse(null);
        });
    }

    private boolean getReportOnly(Map<String, Object> defaultConfig, Map<String, Object> myselfConfig) {
        return (Boolean) Optional.ofNullable(myselfConfig).map(t -> t.get("reportOnly")).orElseGet(() -> {
            return Optional.ofNullable(defaultConfig).map(z -> z.get("reportOnly")).orElse(true);
        });
    }

}
