package com.liepin.swift.framework.limit.rules.chain;

import java.util.List;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.rules.RuleProcessor;

@SuppressWarnings("rawtypes")
public class DefaultRuleProcessorChain extends ProcessorContext {

    private final List<RuleProcessor> ruleProcessors;
    private int currentPosition = 0;

    private DefaultRuleProcessorChain(List<RuleProcessor> ruleProcessors) {
        this.ruleProcessors = ruleProcessors;
    }

    public static DefaultRuleProcessorChain newProcessorChain(List<RuleProcessor> ruleProcessors) {
        return new DefaultRuleProcessorChain(ruleProcessors);
    }

    @Override
    public void next(LimitContext limitContext, Object[] args) throws BlockException {
        if (currentPosition != ruleProcessors.size()) {
            currentPosition++;
            RuleProcessor rulProcessor = ruleProcessors.get(currentPosition - 1);
            rulProcessor.process(limitContext, args, this);
        }
    }

}
