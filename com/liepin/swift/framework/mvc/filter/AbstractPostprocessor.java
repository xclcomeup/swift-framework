package com.liepin.swift.framework.mvc.filter;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractPostprocessor {

    /**
     * 后处理方法 各个平台可以实现自己的后处理方法，覆盖此方法. 默认是不后处理的
     * must do try catch
     * 
     * @param HttpServletRequest req
     */
    public void postprocess(HttpServletRequest req) {
        // TODO 各个平台实现自己的后处理方法

    }

}
