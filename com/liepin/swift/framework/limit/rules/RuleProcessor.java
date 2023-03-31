package com.liepin.swift.framework.limit.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.LimitControll;
import com.liepin.swift.framework.limit.rules.chain.ProcessorContext;
import com.liepin.swift.framework.limit.rules.processor.AuthorityRuleProcessor;
import com.liepin.swift.framework.limit.rules.processor.FlowQpsRuleProcessor;
import com.liepin.swift.framework.limit.rules.processor.ProcessorComparator;
import com.liepin.swift.framework.limit.rules.processor.fallback2s.FallbackRuleProcessor;

public interface RuleProcessor<T extends LimitControll> {

    public void process(LimitContext limitContext, Object[] args, ProcessorContext context) throws BlockException;

    public void updateRuleConfig(T t);
    
    public boolean contain(LimitContext limitContext);

    public void clear();

    /**
     * 数字越大优先级越高
     * 
     * @return
     */
    public int priority();

    @SuppressWarnings("rawtypes")
    public static List<RuleProcessor> build() {
        List<RuleProcessor> list = new ArrayList<>();
        list.add(new AuthorityRuleProcessor());
        list.add(new FlowQpsRuleProcessor());
        // FIXME
        list.add(new FallbackRuleProcessor());
        // 排序
        Collections.sort(list, new ProcessorComparator());
        return list;
    }

}
