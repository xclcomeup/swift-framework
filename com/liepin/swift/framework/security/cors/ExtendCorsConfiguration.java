package com.liepin.swift.framework.security.cors;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

public class ExtendCorsConfiguration extends CorsConfiguration {

    @Override
    public String checkOrigin(String requestOrigin) {
        if (!StringUtils.hasText(requestOrigin)) {
            return null;
        }
        if (ObjectUtils.isEmpty(getAllowedOrigins())) {
            return null;
        }

        if (getAllowedOrigins().contains(ALL)) {
            if (getAllowCredentials() != Boolean.TRUE) {
                return ALL;
            } else {
                return requestOrigin;
            }
        }
        for (String allowedOrigin : getAllowedOrigins()) {
            if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
                return requestOrigin;
            }
            // 支持前缀*.匹配
            if (allowedOrigin.startsWith(ALL)) {
                String suffix = allowedOrigin.substring(ALL.length());
                if (requestOrigin.toLowerCase().endsWith(suffix)) {
                    return requestOrigin;
                }
            }
            // 支持*.匹配
        }

        return null;
    }

}
