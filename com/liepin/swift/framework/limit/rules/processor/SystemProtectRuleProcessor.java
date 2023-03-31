package com.liepin.swift.framework.limit.rules.processor;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.LimitControll;
import com.liepin.swift.framework.limit.rules.RuleProcessor;
import com.liepin.swift.framework.limit.rules.chain.ProcessorContext;

@SuppressWarnings("rawtypes")
public class SystemProtectRuleProcessor implements RuleProcessor {

    @Override
    public void process(LimitContext limitContext, Object[] args, ProcessorContext context)
            throws BlockException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void updateRuleConfig(LimitControll limitControll) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean contain(LimitContext limitContext) {
        // TODO Auto-generated method stub
        return false;
    }

}
