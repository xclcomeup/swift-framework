package com.liepin.swift.framework.limit.rules.processor;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.AuthorityControll;
import com.liepin.swift.framework.limit.rules.RuleProcessor;
import com.liepin.swift.framework.limit.rules.chain.ProcessorContext;

/**
 * 
 * @author yuanxl
 *
 */
public class AuthorityRuleProcessor implements RuleProcessor<AuthorityControll> {

    @Override
    public void process(LimitContext limitContext, Object[] args, ProcessorContext context) throws BlockException {
        // FIXME

        context.next(limitContext, args);
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void updateRuleConfig(AuthorityControll authorityControll) {
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
