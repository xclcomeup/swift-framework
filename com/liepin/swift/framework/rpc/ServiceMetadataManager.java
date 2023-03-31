package com.liepin.swift.framework.rpc;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import com.liepin.swift.core.annotation.SwiftInterface;
import com.liepin.swift.core.util.ConfUtil;
import com.liepin.swift.framework.rpc.cache.RpcCache;
import com.liepin.swift.framework.rpc.config.InterfaceConfig;
import com.liepin.swift.framework.rpc.config.InterfaceConfigReader;
import com.liepin.swift.framework.rpc.limit.HystrixConfigurationCurator;
import com.liepin.swift.framework.rpc.proxy.RPCFactory;
import com.liepin.swift.framework.rpc.router.RouterRPCHandle;
import com.liepin.swift.framework.util.DependencyUtil;

public class ServiceMetadataManager {

    private static final Logger logger = Logger.getLogger(ServiceMetadataManager.class);

    private static final ServiceMetadataManager instance = new ServiceMetadataManager();

    /**
     * service class => IRPCHandle
     */
    private final Map<Class<?>, IRPCHandle> serviceRPCHandleMap = new HashMap<Class<?>, IRPCHandle>();

    /**
     * service class => ServiceMetadata
     */
    private final Map<Class<?>, ServiceMetadata> serviceMetadataMap = new HashMap<Class<?>, ServiceMetadata>();

    /**
     * service class => ClientCache
     */
    private final Map<Class<?>, RpcCache> serviceClientCacheMap = new HashMap<>();

    private ServiceMetadataManager() {
    }

    public static ServiceMetadataManager getInstance() {
        return instance;
    }

    /**
     * 加载lib包下的所有客户端jar
     */
    public void load() {
        long s = System.currentTimeMillis();
        if (loadLib() == 0) {
            loadClassPath();// TODO待测试，单元测试模式
        }

        // 预生成service代理
        prepareServiceProxy();
        logger.warn("RPC Factory: initialization completed in " + (System.currentTimeMillis() - s) + " ms");
    }

    private void loadClassPath() {
        String paths = System.getProperty("java.class.path");
        String[] array = paths.split("\\" + File.pathSeparator);
        for (String path : array) {
            File jarFile = new File(path);
            if (path.endsWith(".jar") && DependencyUtil.isInnerClient(jarFile.getName())) {
                boolean include = include(jarFile);
                if (include) {
                    logger.info("ServiceMetadataManager include jar: " + jarFile.getName());
                } else {
                    logger.info("ServiceMetadataManager try inclue jar: " + jarFile.getName()
                            + ", because did not find the ServiceMetadata and ingore");
                }
            }
        }
    }

    private int loadLib() {
        AtomicInteger loadCnt = new AtomicInteger(0);
        URL location = ServiceMetadataManager.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader classLoader = ServiceMetadataManager.class.getClassLoader();
        try {
            File file = ResourceUtils.getFile(location.getFile());
            File parentFile = file.getParentFile();
            // 获取lib下面的jar包
            String libPath = parentFile.getPath();// file:..../lib
            int pos = libPath.indexOf("!/BOOT-INF/lib");// jar
            pos = (pos == -1) ? libPath.indexOf("!/WEB-INF/lib") : pos;// war
            if (pos != -1) {
                String jarPath = libPath.substring(0, pos);
                List<String> clientNames = new ArrayList<>();
                logger.info("ServiceMetadataManager scan path: " + libPath);
                JarFile jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String name = jarEntry.getName();
                    boolean isJar = false;
                    if (isJar = name.startsWith("BOOT-INF/lib")) {
                        name = name.substring("BOOT-INF/lib".length() + 1);
                    } else if (isJar = name.startsWith("WEB-INF/lib")) {
                        name = name.substring("WEB-INF/lib".length() + 1);
                    }
                    // 过滤并找出客户端jar
                    if (isJar && name.endsWith(".jar") && DependencyUtil.isInnerClient(name)) {
                        clientNames.add(name);
                    }
                }
                jarFile.close();

                if (clientNames.size() > 0) {
                    clientNames.forEach(t -> {
                        String urlString = "jar:file:" + libPath + "/" + t + "!/" + InterfaceConfig.FILENAME;
                        try {
                            InputStream openStream = classLoader.getResourceAsStream(urlString);
                            if (openStream != null && include(openStream, t)) {
                                openStream.close();
                                logger.info("ServiceMetadataManager include jar: " + t);
                            } else {
                                logger.info("ServiceMetadataManager try inclue jar: " + t
                                        + ", because did not find the ServiceMetadata and ingore");
                            }
                            loadCnt.incrementAndGet();
                        } catch (Exception e) {
                            logger.error("RPC元数据文件URL=" + urlString + "解析失败: " + e.getMessage(), e);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            logger.error("RPC加载" + location + "解析失败: " + e.getMessage(), e);
        }
        return loadCnt.get();
    }

    /**
     * 外部扫描service客户端接口
     * 
     * @param inputStream
     * @param jarFileName
     * @return
     */
    public boolean include(InputStream inputStream, String jarFileName) {
        return include(new InterfaceConfigReader(inputStream), jarFileName);
    }

    /**
     * 外部扫描service客户端接口
     * 
     * @param jarFile
     */
    public boolean include(File jarFile) {
        return include(new InterfaceConfigReader(jarFile), jarFile.getName());
    }

    /**
     * 
     * @param reader
     * @param jarFileName
     * @return
     */
    private boolean include(InterfaceConfigReader reader, String jarFileName) {
        // 获取客户端名称
        String projectName = reader.readProjectName();
        Map<Class<?>, Map<String, String[]>> data = reader.read();
        reader.close();

        if (data == null) {
            return false;
        }

        // 获取客户端名称
        if (projectName == null) {
            int pos = jarFileName.lastIndexOf(".jar");
            String clientName = jarFileName.substring(0, pos);
            projectName = ConfUtil.clientName2ProjectName(clientName);
        }

        // 获取客户端版本
        String clientVersion = getVersion(jarFileName);

        // 加载引用客户端限流熔断策略
        HystrixConfigurationCurator.getInstance().loadLimitCustom(projectName);

        IRPCHandle rpcHandle = new RouterRPCHandle(projectName, clientVersion);
        logger.info("ServiceMetadataManager new IRPCHandle for " + projectName + " version=" + clientVersion);

        StringBuilder log = new StringBuilder();
        for (Map.Entry<Class<?>, Map<String, String[]>> entry : data.entrySet()) {
            Class<?> serviceClass = entry.getKey();
            Map<String, String[]> methods = entry.getValue();
            if (serviceMetadataMap.containsKey(serviceClass)) {
                throw new RuntimeException("RPC加载接口类class=" + serviceClass.getName() + " of " + jarFileName
                        + "失败: 接口类出现在多个jar包中, 请先检查jar包中是否有重复的包名和类名!");
            }
            try {
                ServiceMetadata serviceMetadata = new ServiceMetadata(projectName, serviceClass, methods);
                serviceMetadataMap.put(serviceClass, serviceMetadata);
                serviceRPCHandleMap.put(serviceClass, rpcHandle);
                RpcCache rpcCache = createRpcCache(serviceMetadata);
                if (rpcCache != null) {
                    serviceClientCacheMap.put(serviceClass, rpcCache);
                }
                log.append("Added {" + serviceClass.getName()).append("} to ServiceMetadataManager\n");
            } catch (Throwable e) {
                logger.error("file=" + jarFileName + ", projectName=" + projectName + ", class=" + serviceClass
                        + " RPC加载类失败: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        logger.info(log.toString());
        return true;
    }

    private void prepareServiceProxy() {
        for (Map.Entry<Class<?>, ServiceMetadata> entry : serviceMetadataMap.entrySet()) {
            Class<?> clazz = entry.getKey();
            try {
                RPCFactory.getService(clazz);
            } catch (Throwable e) {
                logger.error("projectName=" + entry.getValue().getProjectName() + ", serviceName="
                        + entry.getValue().getServiceName() + ", class=" + entry.getValue().getClazz()
                        + " RPC加载接口代理失败: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
            logger.info("prepare " + clazz.getName() + " service proxy");
        }
    }

    /**
     * 获取类的RPC实现
     * 
     * @param clazz
     * @return
     */
    public IRPCHandle getRPCHandle(Class<?> clazz) {
        return serviceRPCHandleMap.get(clazz);
    }

    /**
     * 获取service层元数据
     * 
     * @param clazz
     * @return
     */
    public ServiceMetadata getServiceMetadata(Class<?> clazz) {
        return serviceMetadataMap.get(clazz);
    }

    /**
     * 获取类的接口缓存
     * 
     * @param clazz
     * @return
     */
    public RpcCache getClentCache(Class<?> clazz) {
        return serviceClientCacheMap.get(clazz);
    }

    /**
     * 获取客户端版本号
     * <p>
     * 如：erp-demo-client-1.0.5.jar => 1.0.5<br>
     * 如：ins-demo-client-1.0.4-SNAPSHOT.jar => 1.0.4-SNAPSHOT<br>
     * 
     */
    private String getVersion(String filename) {
        int pos = filename.indexOf("-client-");
        return filename.substring(pos + "-client-".length(), filename.indexOf(".jar"));
    }

    private RpcCache createRpcCache(ServiceMetadata serviceMetadata) {
        RpcCache rpcCache = null;
        Map<Method, SwiftInterface> methodAnnotationMap = serviceMetadata.getMethodAnnotationMap();
        for (Entry<Method, SwiftInterface> entry : methodAnnotationMap.entrySet()) {
            if (entry.getValue().cache()) {
                if (rpcCache == null) {
                    rpcCache = new RpcCache(serviceMetadata.getServiceName());
                }
                rpcCache.build(entry.getKey());
            }
        }
        return rpcCache;
    }

}
