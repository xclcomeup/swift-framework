package com.liepin.swift.framework.limit.rules.processor.fallback2s;

import java.lang.reflect.InvocationTargetException;

import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.LimitControll;
import com.liepin.swift.framework.limit.rules.RuleProcessor;
import com.liepin.swift.framework.limit.rules.chain.ProcessorContext;

public class FallbackRuleProcessor implements RuleProcessor<LimitControll> {

    @Override
    public void process(LimitContext limitContext, Object[] args, ProcessorContext context)
            throws BlockException, FallbackException {
        BlockException blockException = context.getBlockException();
        if (blockException != null) {
            // 尝试降级逻辑处理
            FallbackHandler fallbackHandler = FallbackProvider.get().getFallbackHandler(limitContext.getUrl());
            if (fallbackHandler != null) {
                try {
                    Object fallbackResult = fallbackHandler.handle(blockException, args);
                    context.setObject(fallbackResult);
                } catch (Throwable e) {
                    Throwable actual = e;
                    if (e instanceof InvocationTargetException) {
                        actual = ((InvocationTargetException) e).getTargetException();
                    }
                    throw new FallbackException("服务端接口降级失败: " + fallbackHandler.toString(), actual);
                }
            }
        }
        context.next(limitContext, args);
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public void updateRuleConfig(LimitControll limitControll) {
        // nothing，没有动态更新
    }

    @Override
    public void clear() {
        // nothing，没有动态更新
    }

    @Override
    public boolean contain(LimitContext limitContext) {
        return false;
    }

}
