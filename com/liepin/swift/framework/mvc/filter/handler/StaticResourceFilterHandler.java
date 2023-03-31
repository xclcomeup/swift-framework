package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.eventInfo.NullEvent;
import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
import com.liepin.swift.framework.mvc.util.AttributeUtil;
import com.liepin.swift.framework.util.UrlUtil;

/**
 * 静态资源和框架内部接口
 * 
 * @author yuanxl
 * @date 2015-11-25 下午10:40:11
 */
public class StaticResourceFilterHandler extends AbstractFilterHandler implements FilterHandler {

    /**
     * 排除拦截的静态资源后缀集合
     */
    private final Set<String> excludeSuffixs = new HashSet<String>();

    private boolean enableDefalutServletSwiftAgreePack = SwiftConfig.enableServletDefalutSwiftAgreePack();

    public StaticResourceFilterHandler() {
        String excludeStr = SwiftConfig.getServletStaticUrlPattern();
        if (SwiftConfig.STATIC_REQUEST_SUFFIX_NONE.equals(excludeStr.toLowerCase())) {
            excludeStr = null;
        }
        Optional.ofNullable(excludeStr).ifPresent(t -> {
            for (String str : t.split(";")) {
                excludeSuffixs.add(str);
            }
        });
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        return UrlUtil.isStaticResource(request.getServletPath(), excludeSuffixs);
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        // 不输出日志
        NullEvent event = new NullEvent();
        event.begin();
        event.setType("staticResource");
        event.setName(request.getServletPath());
        return event;
    }

    @Override
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (enableDefalutServletSwiftAgreePack) {
            AttributeUtil.setViewReturn(request);
        }
        filterChain.doFilter(request, response);
        return ResultStatus.ok();
    }

    @Override
    public boolean context() {
        return false;
    }

}
