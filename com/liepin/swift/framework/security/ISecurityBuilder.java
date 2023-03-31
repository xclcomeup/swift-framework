package com.liepin.swift.framework.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface ISecurityBuilder {

    /**
     * @param http
     * @return 开启http.headers()返回true
     * @throws Exception
     */
    public boolean build(final HttpSecurity http) throws Exception;

}
