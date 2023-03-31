package com.liepin.swift.framework.mvc.filter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.core.exception.BizException;

public abstract class AbstractPreprocessor {

    /**
     * 预处理方法 各个业务可以实现自己的预处理方法，覆盖此方法. 默认是不预处理的
     * 
     * @param Map<String, Object> inputMap 输入参数
     * @param HttpServletRequest req
     */
    public void preprocess(Map<String, Object> inputMap, HttpServletRequest req) throws BizException {

    }

}
