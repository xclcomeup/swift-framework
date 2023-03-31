package com.liepin.swift.framework.plugin.schedule;

import java.lang.reflect.Method;

import com.liepin.client.schedule.annotation.SwiftScheduler;
import com.liepin.swift.framework.plugin.IMethodFilter;

public class ScheduleMethodFilter implements IMethodFilter {

    @Override
    public String path() {
        return "com.liepin.**.**";
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

    @Override
    public boolean test(Method m) {
        return m.getAnnotation(SwiftScheduler.class) != null;
    }
    
}
