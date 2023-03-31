package com.liepin.swift.framework.limit.rules.chain;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;

public interface RuleProcessorChain {

    public void next(LimitContext limitContext, Object[] args) throws BlockException;

}
