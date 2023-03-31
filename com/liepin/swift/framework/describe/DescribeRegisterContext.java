package com.liepin.swift.framework.describe;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * 服务内部功能描述信息注册上报管理
 * 
 * @author yuanxl
 *
 */
public class DescribeRegisterContext {

    private static final Logger logger = Logger.getLogger(DescribeRegisterContext.class);

    private static final Set<IDescribeHook> REGISTER_HOOKS = new HashSet<>();

    public static synchronized void initialize() {
        REGISTER_HOOKS.forEach(hook -> {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        hook.describe();
                    } catch (Exception e) {
                        logger.warn(hook.getClass() + "的API信息注册失败, " + e.getMessage(), e);
                    }
                }

            });
            t.setDaemon(true);
            t.start();
        });
    }

    public static void addRegisterHook(IDescribeHook hook) {
        REGISTER_HOOKS.add(hook);
    }

}
