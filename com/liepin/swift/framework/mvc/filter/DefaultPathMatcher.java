package com.liepin.swift.framework.mvc.filter;

/**
 * 路径匹配器
 * 
 * @author yuanxl
 * 
 */
public class DefaultPathMatcher implements PathMatcher {

    /**
     * 暂时只支持匹配格式：<br>
     * /<br>
     * /*<br>
     * *.jpg，这样的后缀结尾的<br>
     * equals，全匹配<br>
     * /abc/*，路径匹配<br>
     * 多个规则以逗号隔开<br>
     * 
     */
    @Override
    public boolean match(String urlPattern, String path) {
        if (urlPattern == null || urlPattern.trim().length() == 0) {
            return true;
        }
        String[] array = urlPattern.split(",");
        for (String pattern : array) {
            if (matchSingle(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchSingle(String urlPattern, String path) {
        if (urlPattern == null || urlPattern.trim().length() == 0) {
            return true;
        }
        if ("/*".equals(urlPattern) || "/".equals(urlPattern)) {
            return true;
        }
        if (urlPattern.equals(path)) {
            return true;
        }
        if (urlPattern.startsWith("*.")) {
            if (path.endsWith(urlPattern.substring(1))) {
                return true;
            }
        }
        if (urlPattern.endsWith("/*")) {
            if (path.startsWith(urlPattern.substring(0, urlPattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

}
