package com.liepin.swift.framework.plugin.jsp;

import java.net.URL;

import com.liepin.swift.framework.plugin.IURLFilter;

public class JspURLFilter implements IURLFilter {

    @Override
    public boolean test(URL url) {
        return true;
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

    @Override
    public String path() {
        return "/**/*";
    }

    @Override
    public String suffix() {
        return ".jsp";
    }

}
