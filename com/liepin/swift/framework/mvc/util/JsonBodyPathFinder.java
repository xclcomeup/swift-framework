package com.liepin.swift.framework.mvc.util;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.AntPathMatcher;

public class JsonBodyPathFinder {

    private static AntPathMatcher matcher = new AntPathMatcher();

    private final Set<String> NO_PATTERN_PATHS = new HashSet<String>();
    private final Set<String> PATTERN_PATHS = new HashSet<>();

    // @RequestBody
    private static final JsonBodyPathFinder request = new JsonBodyPathFinder();
    // @RestController || @ResponseBody
    private static final JsonBodyPathFinder response = new JsonBodyPathFinder();

    // @SwiftNoPack
    private static final JsonBodyPathFinder noPackPathFinder = new JsonBodyPathFinder();
    // @SwiftNoPack4Input
    private static final JsonBodyPathFinder noPack4InputPathFinder = new JsonBodyPathFinder();

    // @UnEscapeHtml
    private static final JsonBodyPathFinder unEscapeHtmlPathFinder = new JsonBodyPathFinder();

    public static JsonBodyPathFinder getRequest() {
        return request;
    }

    public static JsonBodyPathFinder getResponse() {
        return response;
    }

    public static JsonBodyPathFinder getNopack() {
        return noPackPathFinder;
    }

    public static JsonBodyPathFinder getNopack4input() {
        return noPack4InputPathFinder;
    }

    public static JsonBodyPathFinder getUnEscapeHtml() {
        return unEscapeHtmlPathFinder;
    }

    public void append(String servletPath) {
        if (isPattern(servletPath)) {
            PATTERN_PATHS.add(servletPath);
        } else {
            NO_PATTERN_PATHS.add(servletPath);
        }
    }

    public boolean match(String servletPath) {
        if (NO_PATTERN_PATHS.isEmpty() && PATTERN_PATHS.isEmpty()) {
            return false;
        }
        // 有精确
        if (NO_PATTERN_PATHS.size() > 0) {
            if (NO_PATTERN_PATHS.contains(servletPath)) {
                return true;
            }
        }
        // 有pattern
        if (PATTERN_PATHS.size() > 0) {
            for (String pattern : PATTERN_PATHS) {
                if (matcher.match(pattern, servletPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPattern(String path) {
        return path.indexOf('*') != -1 || path.indexOf('?') != -1
                || (path.indexOf("{") != -1 && path.indexOf("}") != -1);
    }

}
