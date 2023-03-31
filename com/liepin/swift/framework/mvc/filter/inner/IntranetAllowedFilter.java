package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.util.IPUtil;

/**
 * 只允许内网访问，也就是私网ip访问<br>
 * 
 * @author yuanxl
 * @date 2015-7-17 下午12:05:26
 */
public class IntranetAllowedFilter extends GenericFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = getClientIp(request);
        if ("127.0.0.1".equals(ip)) {
            return;
        }
        if (!IPUtil.isPrivateIp(ip)) {
            throw new SysException(SystemEnum.IP_NOT_VALID);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
