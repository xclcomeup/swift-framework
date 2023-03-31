package com.liepin.swift.framework.limit;

import java.util.List;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.common.datastructure.Pair;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.limit.config.LimitControllConfiger;
import com.liepin.swift.framework.limit.rules.Bridge;
import com.liepin.swift.framework.limit.rules.RuleProcessor;
import com.liepin.swift.framework.limit.rules.chain.DefaultRuleProcessorChain;
import com.liepin.swift.framework.limit.rules.processor.fallback2s.FallbackException;

/**
 * 业务使用入口
 * 
 * @author yuanxl
 *
 */
public class Hourglass {

    private static final Logger logger = Logger.getLogger(Hourglass.class);

    private static final Pair<Boolean, Object> NO_LIMIT = new Pair<Boolean, Object>(true, null);

    /**
     * 框架内部包装的通用规则限流
     * <p>
     * 覆盖：请求接口url、请求方clientId、请求来源ip
     * 
     * @param args 请求参数
     * @return 如果有降级逻辑，返回降级逻辑执行结果 <true|false, 降级结果>，true:无限流、false:有限流
     * @throws SysException 如果触发限流并且没有降级逻辑则抛出限流异常，或者降级逻辑执行异常
     */
    @SuppressWarnings("rawtypes")
    public static Pair<Boolean, Object> entry(Object[] args) throws SysException {
        // 判断总开关
        if (!LimitControllConfiger.get().isEnable()) {
            return NO_LIMIT;
        }
        // 创建请求上下文
        LimitContext limitContext = LimitContextUtil.create();

        // lookProcessChain 查找处理链，没有就返回
        List<RuleProcessor> ruleProcessors = Bridge.lookup(limitContext);
        if (ruleProcessors == null || ruleProcessors.isEmpty()) {
            return NO_LIMIT;
        }

        Transaction t = Cat.newTransaction("ServerLimit", limitContext.getUrl());
        t.addData("context", limitContext);
        // 链式处理
        try {
            DefaultRuleProcessorChain processorChain = DefaultRuleProcessorChain.newProcessorChain(ruleProcessors);
            processorChain.next(limitContext, args);
            processorChain.releaseBlockException();
            Object fallbackObject = processorChain.getFallbackObject();
            Cat.logEvent("ServerLimit.status", ((fallbackObject != null) ? "YES_LIMIT&FALLBACK_SUCCESS" : "NO_LIMIT"));
            t.setStatus(Message.SUCCESS);
            return new Pair<Boolean, Object>(fallbackObject == null, fallbackObject);
        } catch (BlockException be) {
            Cat.logEvent("ServerLimit.status", "YES_LIMIT&NO_FALLBACK");
            t.setStatus(be);
            Cat.logError(be);
            throw new SysException(SystemEnum.SERVICE_LIMIT, be);
        } catch (FallbackException fe) {
            Cat.logEvent("ServerLimit.status", "YES_LIMIT&FALLBACK_FAIL");
            t.setStatus(fe);
            Cat.logError(fe);
            throw new SysException(SystemEnum.SERVICE_LIMIT_FALLBACK_FAIL, fe);
        } catch (Throwable throwable) {
            // this should not happen
            Cat.logEvent("ServerLimit.status", "LIMIT_FAIL");
            t.setStatus(throwable);
            Cat.logError(throwable);
            logger.error("服务端限流模块异常: " + limitContext, throwable);
            throw throwable;
        } finally {
            t.complete();
        }
    }

    /**
     * 业务自定义的规则限流
     * 
     * @param name
     * @return
     */
    // public static boolean entry(String name) {
    //
    // return true;
    // }

    /**
     * 业务自定义的规则限流
     * <p>
     * 热点数据限流
     * 
     * @param name
     * @param args
     * @return
     */
    // public static boolean entry(String name, Object... args) {
    //
    // return true;
    // }

}
