package com.liepin.swift.framework.boot;

import java.beans.Introspector;

import org.apache.log4j.Logger;
import org.springframework.beans.CachedIntrospectionResults;

import com.liepin.cache.redis.RedisCacheClientFactory;
import com.liepin.router.discovery.ServiceDiscovery;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.contracts.ContractContext;
import com.liepin.swift.framework.limit.SwiftLimitInitializer;
import com.liepin.swift.framework.monitor.MonitorRegister;
import com.liepin.swift.framework.monitor.cat.CatContext;
import com.liepin.swift.framework.monitor.heartbeat.HttpHeartbeatManager;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.rpc.proxy.RPCFactory;
import com.liepin.swift.framework.util.AsynProcess;
import com.liepin.swift.framework.util.AsynProcess.AsynHandle;

/**
 * 雨燕web框架 接口、配置中心
 * 
 * @author yuanxl
 * 
 */
public class SwiftApplicationContext {

    private static final Logger logger = Logger.getLogger(SwiftApplicationContext.class);

    private static SwiftApplicationContext context = new SwiftApplicationContext();

    private volatile boolean initialized = false;// 初始化标识
    private volatile boolean cancelled = false;// 注销标识

    private SwiftApplicationContext() {
    }

    public static synchronized SwiftApplicationContext initialize() {
        if (!context.initialized) {
            context.init();
        }
        return context;
    }

    public static SwiftApplicationContext getContext() {
        return context;
    }

    private void init() {
        long start = System.currentTimeMillis();
        // 约定检查
        long timeAt = System.currentTimeMillis();
        ContractContext.initialize();
        logger.info("ContractContext startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 预加载Cat
        timeAt = System.currentTimeMillis();
        CatContext.initialize();
        logger.info("CatContext startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 加载组件
        timeAt = System.currentTimeMillis();
        PluginContext.get().initialize();
        logger.info("PluginContext startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 加载RPC
        timeAt = System.currentTimeMillis();
        if (SwiftConfig.enableStartupPreload()) {
            RPCFactory.initialize();
        } else {
            new Thread(() -> {
                RPCFactory.initialize();
            }).start();
        }
        logger.info("RPCFactory startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 加载服务端限流
        timeAt = System.currentTimeMillis();
        SwiftLimitInitializer.initialize();
        logger.info("SwiftLimitInitializer startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 加载流量心跳
        timeAt = System.currentTimeMillis();
        HttpHeartbeatManager.get();
        logger.info("HttpHeartbeatManager startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // 监控
        timeAt = System.currentTimeMillis();
        MonitorRegister.initialize();
        logger.info("MonitorRegister startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        // BLOG预加载
        timeAt = System.currentTimeMillis();
        if (SwiftConfig.enableStartupPreload()) {
            com.liepin.swift.log.BLogLogger.init();
        } else {
            new Thread(() -> {
                com.liepin.swift.log.BLogLogger.init();
            }).start();
        }
        logger.info("BLogLogger startup in " + (System.currentTimeMillis() - timeAt) + " ms");

        /**
         * 防止内存泄露，配置Listener
         * 
         * @see org.springframework.web.util.IntrospectorCleanupListener
         */
        CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());

        initialized = true;

        logger.info("SwiftApplicationContext startup in " + (System.currentTimeMillis() - start) + " ms");
    }

    public static synchronized void stop() {
        if (!context.cancelled) {
            context.unInit();
        }
    }

    private void unInit() {
        long start = System.currentTimeMillis();
        // 释放组件
        PluginContext.get().cancel();

        // 释放其他
        destroyOthers();

        // 释放Cat
        CatContext.destory();

        // 释放zk资源
        // ZookeeperFactory.useDefaultZookeeperWithoutException().destroy();

        /**
         * 防止内存泄露，配置Listener
         * 
         * @see org.springframework.web.util.IntrospectorCleanupListener
         */
        CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
        Introspector.flushCaches();

        cancelled = true;

        logger.info("SwiftApplicationContext stop in " + (System.currentTimeMillis() - start) + "ms");
    }

    private void destroyOthers() {
        AsynProcess asynProcess = new AsynProcess(4);
        asynProcess.execute("释放redis缓存资源", new AsynHandle() {

            @Override
            public void process() {
                RedisCacheClientFactory.closeHeartbeatReporting();
            }

        });
        asynProcess.execute("释放dao一级缓存资源", new AsynHandle() {

            @Override
            public void process() {
                // 不主动关闭，避免停止时业务线程还有使用
                // CacheClientFactory.destroy();
            }

        });
        asynProcess.execute("释放监控资源", new AsynHandle() {

            @Override
            public void process() {
                MonitorRegister.destroy();
            }

        });
        asynProcess.execute("释放rpc资源", new AsynHandle() {

            @Override
            public void process() {
                // 不主动关闭，避免停止时业务线程还有使用
                // RouterHelper.destroyAll();
                ServiceDiscovery.getInstance().destroy();
            }

        });
        asynProcess.awaitAndfinish();
    }

    public static boolean initialized() {
        return context.initialized;
    }

}
