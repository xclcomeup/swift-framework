package com.liepin.swift.framework.mvc.dispatcher;

import java.util.LinkedHashMap;

public class DispatcherMethodBean extends DispatcherBean {

    public LinkedHashMap<String, ParamBean> paramMap = new LinkedHashMap<String, ParamBean>();

    public ParamBean getParamBean(String paramName) {
        return paramMap.get(paramName);
    }

    public void setParamBean(String paramName, ParamBean paramBean) {
        paramMap.put(paramName, paramBean);
    }

    public LinkedHashMap<String, ParamBean> getParamMap() {
        return paramMap;
    }

    public static class ParamBean {
        public Class<?> parametrized;
        public Class<?>[] parameterClasses = new Class<?>[] {};// 默认无参数化类型

        public String toString() {
            // Map<Long, List<Long>>
            StringBuilder describe = new StringBuilder();
            describe.append(parametrized.getSimpleName());
            for (int i = 0; i < parameterClasses.length; i++) {
                if (i == 0) {
                    describe.append("<");
                }
                if (i > 0) {
                    describe.append(", ");
                }
                describe.append(parameterClasses[i].getSimpleName());
                if (i == parameterClasses.length - 1) {
                    describe.append(">");
                }
            }
            return describe.toString();
        }

    }

}
