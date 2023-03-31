package com.liepin.swift.framework.mvc.http;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.PropUtil;
import com.liepin.common.datastructure.Pair;
import com.liepin.common.magic.EscapeFilter;
import com.liepin.swift.framework.bundle.choice.Switcher;
import com.liepin.swift.framework.mvc.util.JsonBodyPathFinder;
import com.liepin.swift.framework.mvc.util.RequestUtil;

/**
 * 防XSS攻击
 * 
 * @author yuanxl
 * @date 2016-5-8 下午04:52:54
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final Set<String> paramNamesWhitelist;// 参数UnEscapeHtml白名单
    private boolean ignoreEscapeQuot = false;// 忽略双引号过滤
    private static boolean trimEnable = PropUtil.getInstance().getBoolean("web.request.trim.enable", false);
    private Switcher switcher;
    private boolean ignoreAll = false;// 忽略所有过滤（双引号、尖括号）

    public XssHttpServletRequestWrapper(HttpServletRequest request, Set<String> paramNames,
            Pair<Set<String>, Set<String>> pair, Switcher switcher, boolean ignoreDoubleQuotes) {
        super(request);
        this.paramNamesWhitelist = paramNames;
        this.switcher = switcher;
        if (ignoreDoubleQuotes) {
            this.ignoreEscapeQuot = true;
        } else {
            if (Objects.nonNull(pair)) {
                if (pair.getFirst().contains(request.getServletPath())) {
                    ignoreEscapeQuot = true;
                } else {
                    for (String prefix : pair.getSecond()) {
                        if (request.getServletPath().startsWith(prefix)) {
                            ignoreEscapeQuot = true;
                            break;
                        }
                    }
                }
            }
        }
        // 整个url都忽略
        if (JsonBodyPathFinder.getUnEscapeHtml().match(request.getServletPath())) {
            this.ignoreAll = true;
        }
    }

    /**
     * 判断白名单
     * 
     * @param name
     * @return
     */
    private boolean isWhitelist(String name) {
        return (paramNamesWhitelist != null) ? paramNamesWhitelist.contains(name) : false;
    }

    /**
     * 判断请求是否需要转义双引号
     * 
     * @return
     */
    private boolean needDqmRequest() {
        HttpServletRequest request = (HttpServletRequest) getRequest();
        return !RequestUtil.isJSONRequest(request);
    }

    private String escape(String value) {
        // return (String) EscapeFilter.escapeHtml(value);
        if (Objects.isNull(value) || ignoreAll) {
            return value;
        }
        if ((!ignoreEscapeQuot && Objects.nonNull(switcher) && needDqmRequest()
                && switcher.getEnable(ProjectId.getClientId()))) {
            return (String) EscapeFilter.escapeHtmlMore(value);
        }
        return (String) EscapeFilter.escapeHtml(value);
    }

    private String trim(String value) {
        return (value != null) ? ((trimEnable) ? value.trim() : value) : value;
    }

    private String[] trim(String[] value) {
        if (value == null || value.length == 0 || !trimEnable) {
            return value;
        }
        String[] array = new String[value.length];
        for (int i = 0; i < value.length; i++) {
            array[i] = (value[i] != null) ? value[i].trim() : value[i];
        }
        return array;
    }

    @Override
    public String getHeader(String name) {
        return escape(super.getHeader(name));
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Enumeration<String> headers = super.getHeaders(name);
        if (headers != null) {
            Vector<String> vector = new Vector<String>();
            while (headers.hasMoreElements()) {
                vector.add(escape(headers.nextElement()));
            }
            return vector.elements();
        }
        return headers;
    }

    @Override
    public String getQueryString() {
        return escape(trim(super.getQueryString()));
    }

    @Override
    public String getParameter(String name) {
        return (isWhitelist(name)) ? trim(super.getParameter(name)) : escape(trim(super.getParameter(name)));
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = super.getParameterMap();
        if (parameterMap != null) {
            Map<String, String[]> escapeMap = new HashMap<String, String[]>();
            for (Entry<String, String[]> entry : parameterMap.entrySet()) {
                String name = entry.getKey();
                String[] value = entry.getValue();
                value = trim(value);
                if (isWhitelist(name)) {
                    escapeMap.put(name, value);
                    continue;
                }
                String[] escapeArray = null;
                if (value != null && value.length != 0) {
                    if (value.length == 1) {
                        escapeArray = new String[1];
                        escapeArray[0] = escape(value[0]);
                    } else {
                        escapeArray = new String[value.length];
                        for (int i = 0; i < value.length; i++) {
                            escapeArray[i] = escape(value[i]);
                        }
                    }
                }
                escapeMap.put(name, escapeArray);
            }
            return escapeMap;
        }
        return parameterMap;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] parameterValues = super.getParameterValues(name);
        parameterValues = trim(parameterValues);
        if (!isWhitelist(name) && parameterValues != null) {
            String[] escapeValues = new String[parameterValues.length];
            for (int p = 0; p < parameterValues.length; p++) {
                escapeValues[p] = escape(parameterValues[p]);
            }
            return escapeValues;
        }
        return parameterValues;
    }

}
