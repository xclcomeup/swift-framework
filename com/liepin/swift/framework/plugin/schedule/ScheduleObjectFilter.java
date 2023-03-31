package com.liepin.swift.framework.plugin.schedule;

import com.liepin.client.schedule.binding.Scheduler;
import com.liepin.swift.framework.plugin.IObjectFilter;

public class ScheduleObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        return o instanceof Scheduler;
    }

}
