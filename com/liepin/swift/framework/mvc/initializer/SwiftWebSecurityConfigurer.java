package com.liepin.swift.framework.mvc.initializer;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.liepin.swift.framework.security.ISecurityBuilder;
import com.liepin.swift.framework.security.cors.CorsBuilder;
import com.liepin.swift.framework.security.csp.CspBuilder;
import com.liepin.swift.framework.security.csrf.CsrfBuilder;
import com.liepin.swift.framework.security.firewall.StatusCodeStrictHttpFirewall;
import com.liepin.swift.framework.security.frameOptions.FrameOptionsBuilder;
import com.liepin.swift.framework.security.referrer.ReferrerPolicyBuilder;
import com.liepin.swift.framework.security.xxp.XxpBuilder;

@EnableWebSecurity
public class SwiftWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    public SwiftWebSecurityConfigurer() {
        super(true);// 去掉默认配置
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // // war包项目才生效
        // if (DeployType.WAR != SystemUtil.getDeployType()) {
        // return;
        // }

        // 去掉默认配置
        http.headers().defaultsDisabled();

        List<ISecurityBuilder> list = Arrays.asList(new XxpBuilder(), new CspBuilder(), new CorsBuilder(),
                new FrameOptionsBuilder(), new ReferrerPolicyBuilder(), new CsrfBuilder());
        boolean disable = true;
        for (ISecurityBuilder securityBuilder : list) {
            if (securityBuilder.build(http)) {
                disable = false;
            }
        }

        if (disable) {
            // Headers security is enabled, but no headers will be added. Either
            // add headers or disable headers security
            http.headers().disable();
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.httpFirewall(new StatusCodeStrictHttpFirewall());// 定制返回码
    }

}
