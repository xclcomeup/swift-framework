package com.liepin.swift.framework.security.cors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.common.base.Splitter;
import com.liepin.common.conf.PropUtil;
import com.liepin.swift.framework.security.ISecurityBuilder;

public class CorsBuilder implements ISecurityBuilder {

    private static final Logger logger = Logger.getLogger(CorsBuilder.class);

    private static boolean enable = PropUtil.getInstance().getBoolean("security.cors.enable", false);

    public static final String ALL_DOT = "*.";

    public static boolean isEnable() {
        return enable;
    }

    @Override
    public boolean build(final HttpSecurity http) throws Exception {
        // 开关
        if (!isEnable()) {
            return false;
        }

        // 配置
        // By default, all origins, all headers, credentials and GET, HEAD, and
        // POST methods are allowed, and the max age is set to 30 minutes.
        // 允许跨域的源
        String allowedOrigins = PropUtil.getInstance().getString("security.cors.AllowedOrigins",
                "*.liepin.com,*.liepin.cn");
        List<String> allowedOriginsList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedOrigins);
        checkAllowedOriginsConf(allowedOriginsList);
        // 允许哪些方法可以跨域请求
        String allowedMethods = PropUtil.getInstance().getString("security.cors.AllowedMethods", "GET,HEAD,POST");
        List<String> allowedMethodsList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedMethods);
        allowedMethodsList = allowedMethodsList.stream().map(t -> t.toUpperCase()).collect(Collectors.toList());
        // List<HttpMethod> allowedMethodsList =
        // allowedMethodsStringList.stream().map(t->
        // t.toUpperCase()).map(HttpMethod::resolve).collect(Collectors.toList());
        // 允许跨域携带的header
        String allowedHeaders = PropUtil.getInstance().getString("security.cors.AllowedHeaders", "X-Requested-With");
        List<String> allowedHeadersList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedHeaders);
        // 配合CSRF拦截增加默认携带：X-XSRF-TOKEN
        allowedHeadersList = new ArrayList<>(allowedHeadersList);
        allowedHeadersList.add("X-XSRF-TOKEN");
        // 是否允许跨域携带认证信息，例如Cookie
        boolean allowCredentials = PropUtil.getInstance().getBoolean("security.cors.AllowCredentials", true);
        // 预检请求的有效期，单位为秒，默认60分钟
        long maxAge = PropUtil.getInstance().getLong("security.cors.MaxAge", 3600L);
        // 允许跨域访问的header
        String exposedHeaders = PropUtil.getInstance().getString("security.cors.ExposedHeaders", "");
        List<String> exposedHeadersList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(exposedHeaders);

        ExtendCorsConfiguration configuration = new ExtendCorsConfiguration();
        configuration.setAllowedOrigins(allowedOriginsList);
        configuration.setAllowedMethods(allowedMethodsList);
        configuration.setAllowedHeaders(allowedHeadersList);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        configuration.setExposedHeaders(exposedHeadersList);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/RPC/**", null);// 排除来自从网关带Origin的请求
        source.registerCorsConfiguration("/GW/**", null);
        source.registerCorsConfiguration("/**", configuration);
        http.cors().configurationSource(source);
        return false;
    }

    private void checkAllowedOriginsConf(List<String> allowedOriginsList) {
        for (String origin : allowedOriginsList) {
            if (CorsConfiguration.ALL.equals(origin)) {
                RuntimeException exception = new RuntimeException(
                        "CORS配置config.properties的security.cors.AllowedOrigins不合理：不应该单独配置\"" + origin + "\"");
                logger.error("CORS的AllowedOrigins配置校验失败", exception);
                throw exception;
            }
            if (origin.startsWith(CorsConfiguration.ALL) && !origin.startsWith(ALL_DOT)) {
                RuntimeException exception = new RuntimeException(
                        "CORS配置config.properties的security.cors.AllowedOrigins不合理：错误配置=\"" + origin + "\", 正确配置=\""
                                + ALL_DOT + origin.substring(CorsConfiguration.ALL.length()) + "\"");
                logger.error("CORS的AllowedOrigins配置校验失败", exception);
                throw exception;
            }
        }
    }

}
