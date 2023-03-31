package com.liepin.swift.framework.rpc.router;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectIdMap;
import com.liepin.router.Router;
import com.liepin.router.cluster.ConfigCluster;
import com.liepin.router.cluster.HttpHealthChecker;
import com.liepin.router.cluster.TcpHealthChecker;
import com.liepin.router.cluster.UdpHealthChecker;
import com.liepin.router.dispatch.ConfigDispatch;
import com.liepin.router.loadbalance.AbstractLoadBalance;
import com.liepin.router.loadbalance.LoadBalanceHelper;
import com.liepin.router.rule.AbstractModelRule;
import com.liepin.router.rule.ConfigRule;
import com.liepin.router.rule.MasterSlaveModeRule;
import com.liepin.router.util.RouterConfigCurator;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 路由器构造工具类
 * 
 * @author yuanxl
 * 
 */
public final class RouterHelper {

    private static final Logger logger = Logger.getLogger(RouterHelper.class);

    // 启动加载依赖的所有客户端，不考虑线程安全. 但也有场景(网关系统、部分项目自己打包的rpc客户端)是赖加载，可能有线程安全问题
    private static final ConcurrentMap<String, Router> CONTAINER = new ConcurrentHashMap<>();

    /**
     * 从容器里取应用对应的路由器
     * 
     * @param projectName
     * @return
     */
    public static Router getInstance(String projectName) {
        return getInstance(projectName, null, null);
    }

    /**
     * 从容器里取应用对应的路由器
     * 
     * @param projectName
     * @param clientVersion
     * @return
     */
    public static Router getInstance(String projectName, String clientVersion) {
        return getInstance(projectName, clientVersion, null);
    }

    /**
     * 从容器里去应用对应的路由器
     * <p>
     * 自定义服务器部署模式:如，可以实现主从模式 {@link MasterSlaveModeRule}
     * 
     * @param appName
     * @param modelRule
     * @return
     */
    public static Router getInstance(String projectName, String clientVersion, AbstractModelRule modelRule) {
        Router current = CONTAINER.get(projectName);
        if (Objects.isNull(current)) {
            current = CONTAINER.computeIfAbsent(projectName, k -> {

                Router router = new Router(projectName);
                router.setExceptionHandler(new CommonExceptionHandler());
                // 如果没提供客户端版本，就根据调用链获取
                router.setPreprocessor(
                        new CommonPreprocessor(projectName, Optional.ofNullable(clientVersion).orElse("")));
                router.setPostprocessor(new CommonPostprocessor());
                router.setCluster(new ConfigCluster(projectName));
                router.setDispatch(ConfigDispatch.checkLoad(projectName));
                router.setModelRule(ConfigRule.wrap(modelRule, projectName));
                AbstractLoadBalance loadBalance = getLoadBalance(projectName);
                if (loadBalance != null) {
                    router.setLoadBalance(loadBalance);
                }

                // 从全局配置获取心跳设置
                String healthCheckMode = RouterConfigCurator.get().getHealthCheckMode();
                int healthCheckInterval = RouterConfigCurator.get().getHealthCheckInterval();

                router.setInterval(healthCheckInterval);// 客户端心跳间隔
                if (healthCheckMode == null || healthCheckMode.equals("UDP")) {
                    int threshold = RouterConfigCurator.get().getUdpThreshold();
                    long t = threshold * 1000;// 秒 => 毫秒
                    router.setHealthChecker(new UdpHealthChecker(t));
                } else if (healthCheckMode.equals("TCP")) {
                    int threshold = RouterConfigCurator.get().getTcpThreshold();
                    router.setHealthChecker(new TcpHealthChecker(threshold));
                } else if (healthCheckMode.equals("HTTP")) {
                    String httpUrl = RouterConfigCurator.get().getHttpUrl();
                    int httpThreshold = RouterConfigCurator.get().getHttpThreshold();
                    router.setHealthChecker(new HttpHealthChecker(httpUrl, httpThreshold));
                }

                boolean newway = router.init();

                StringBuilder log = new StringBuilder("ins-router config: ");
                log.append(RouterConfigCurator.get().toString()).append(" @").append(projectName);
                if (newway) {
                    log.append(" new way!");
                }
                logger.info(log.toString());
                return router;
            });

        }
        return current;
    }

    static {
        // 交给框架控制 registerShutdownHook();
    }

    /**
     * 释放指定项目路由器
     * 
     * @param projectName
     */
    public static void destroy(String projectName) {
        Optional.ofNullable(CONTAINER.remove(projectName)).ifPresent(t -> t.destroy());
    }

    /**
     * 释放已加载的项目路由器
     */
    public static synchronized void destroyAll() {
        for (Router router : CONTAINER.values()) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    router.destroy();
                }

            });
            t.setDaemon(true);
            t.start();
        }
        CONTAINER.clear();
    }

    // private static void registerShutdownHook() {
    // Thread shutdownHook = new Thread() {
    // public void run() {
    // destroyAll();
    // }
    // };
    // Runtime.getRuntime().addShutdownHook(shutdownHook);
    // }

    /**
     * 获取客户端版本号
     * 
     * @return
     */
    @SuppressWarnings("unused")
    private static String getVersion() {
        // 取版本号
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        Class<?> clazz = getClass(stes[3].getClassName());
        if (clazz == RouterHelper.class) {
            clazz = getClass(stes[4].getClassName());
        }
        if (clazz == null) {
            return "";
        }
        String jarFilePath = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
        File jarFile = new File(jarFilePath);
        if (jarFile.isDirectory()) {
            return "";
        }
        String filename = jarFile.getName();
        int pos = -1;
        if ((pos = filename.indexOf(".jar")) == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("-") + 1, pos);
    }

    private static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    private static AbstractLoadBalance getLoadBalance(String projectName) {
        String clientId = ProjectIdMap.clientId(projectName);
        Properties properties = ZookeeperFactory.useServerZookeeperWithoutException()
                .getProperties(EnumNamespace.PUBLIC, "/rpc/client/loadBalance");
        if (properties != null && properties.size() > 0) {
            String strategyName = properties.getProperty(clientId);
            if (strategyName != null) {
                return LoadBalanceHelper.getLoadBalance(strategyName);
            }
        }
        return null;
    }

}
