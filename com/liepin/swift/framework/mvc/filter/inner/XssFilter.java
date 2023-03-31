package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.http.XssHttpServletRequestWrapper;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;

public class XssFilter extends GenericFilter implements IPluginListener {

    private ControllerPlugin controllerPlugin;
    private Switcher switcher;
    private boolean ignoreDoubleQuotes = false;// 忽略双引号过滤

    public XssFilter() {
        this(false);
    }

    public XssFilter(boolean ignoreDoubleQuotes) {
        this.ignoreDoubleQuotes = ignoreDoubleQuotes;
        this.switcher = new Switcher("/common/protect/xss");
        ControllerPlugin.listen(this);
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        this.controllerPlugin = (ControllerPlugin) plugin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // url的请求参数白名单
        Pair<Set<String>, Set<String>> pair = controllerPlugin.getUnEscapeHtmlControllerPair();
        Set<String> paramNames = controllerPlugin.getUnEscapeHtmlControllerParam(request.getServletPath());
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(request, paramNames, pair, switcher, ignoreDoubleQuotes);
        filterChain.doFilter(xssRequest, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
