package com.liepin.swift.framework.limit.rules.processor;

import java.util.Comparator;

import com.liepin.swift.framework.limit.rules.RuleProcessor;

@SuppressWarnings("rawtypes")
public class ProcessorComparator implements Comparator<RuleProcessor> {

    @Override
    public int compare(RuleProcessor o1, RuleProcessor o2) {
        if (o1.priority() == o2.priority()) {
            return 0;
        }
        return (o1.priority() > o2.priority()) ? -1 : 1;
    }

}
