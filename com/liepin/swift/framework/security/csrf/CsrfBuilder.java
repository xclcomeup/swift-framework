package com.liepin.swift.framework.security.csrf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.liepin.common.conf.PropUtil;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;
import com.liepin.swift.framework.security.ISecurityBuilder;

public class CsrfBuilder implements ISecurityBuilder, IPluginListener {

    private static boolean enable = PropUtil.getInstance().getBoolean("security.csrf.enable", false);

    private final HashSet<String> allowedMethods = new HashSet<>(Arrays.asList("HEAD", "TRACE", "OPTIONS"));
    private Set<String> csrfUrls = new HashSet<>();

    public static boolean isEnable() {
        return enable;
    }

    @Override
    public boolean build(HttpSecurity http) throws Exception {
        // 开关
        if (!isEnable()) {
            return false;
        }

        http.csrf().csrfTokenRepository(new CookieCsrfTokenRepository())
                .requireCsrfProtectionMatcher(new RequestMatcher() {

                    /**
                     * return false: 不走csrf检查<br>
                     * return true: 走csrf检查<br>
                     */
                    @Override
                    public boolean matches(HttpServletRequest request) {
                        // 过滤method
                        if (allowedMethods.contains(request.getMethod())) {
                            return false;
                        }
                        // 过滤url
                        return csrfUrls.contains(request.getServletPath());
                    }

                });

        ControllerPlugin.listen(this);

        return false;
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        Set<String> csrfUrls = ((ControllerPlugin) plugin).getNeedCsrfUrls();
        if (csrfUrls.size() > 0) {
            this.csrfUrls = csrfUrls;
        }
    }

}
