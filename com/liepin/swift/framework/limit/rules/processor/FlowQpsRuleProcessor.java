package com.liepin.swift.framework.limit.rules.processor;

import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.util.internal.ConcurrentHashMap;

import com.google.common.util.concurrent.RateLimiter;
import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.FlowQpsControll;
import com.liepin.swift.framework.limit.rules.RuleProcessor;
import com.liepin.swift.framework.limit.rules.chain.ProcessorContext;

/**
 * 针对接口访问限流
 * 
 * @author yuanxl
 *
 */
public class FlowQpsRuleProcessor implements RuleProcessor<FlowQpsControll> {

    private final ConcurrentMap<String, RateLimiter> qpsRateLimiters = new ConcurrentHashMap<>();

    @Override
    public void process(LimitContext limitContext, Object[] args, ProcessorContext context) throws BlockException {
        RateLimiter rateLimiter = qpsRateLimiters.get(limitContext.getUrl());
        if (rateLimiter != null) {
            if (!rateLimiter.tryAcquire()) {
                // 到达限流
                context.setBlockException(
                        new BlockException("服务端接口限流: URL=" + limitContext.getUrl() + ", QPS=" + rateLimiter.getRate()));
            }
        }
        context.next(limitContext, args);
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public void updateRuleConfig(FlowQpsControll flowQpsControll) {
        RateLimiter oldRateLimiter = qpsRateLimiters.get(flowQpsControll.getUrl());
        if (oldRateLimiter != null) {
            // 更新
            if (flowQpsControll.isEnable()) {
                if (oldRateLimiter.getRate() != flowQpsControll.getQps()) {
                    RateLimiter newRateLimiter = RateLimiter.create(flowQpsControll.getQps());
                    qpsRateLimiters.put(flowQpsControll.getUrl(), newRateLimiter);
                }
            } else {
                qpsRateLimiters.remove(flowQpsControll.getUrl());
            }
        } else {
            // 新增
            if (flowQpsControll.isEnable()) {
                RateLimiter newRateLimiter = RateLimiter.create(flowQpsControll.getQps());
                qpsRateLimiters.put(flowQpsControll.getUrl(), newRateLimiter);
            }
        }
    }

    @Override
    public boolean contain(LimitContext limitContext) {
        String url = limitContext.getUrl();
        return (url != null) ? qpsRateLimiters.containsKey(url) : false;
    }

    @Override
    public void clear() {
        qpsRateLimiters.clear();
    }

}
