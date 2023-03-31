package com.liepin.swift.framework.plugin.service;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.router.discovery.ServicePortUtil;
import com.liepin.swift.core.annotation.SwiftInterface;
import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.describe.DescribeRegisterContext;
import com.liepin.swift.framework.describe.IDescribeHook;
import com.liepin.swift.framework.limit.BlockException;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherBean;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean.ParamBean;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.rpc.config.InterfaceConfigClassReader;
import com.liepin.swift.framework.util.DependencyUtil;
import com.liepin.swift.framework.util.LockUtil;
import com.liepin.swift.framework.util.LockUtil.Function;
import com.liepin.swift.framework.util.TypeUtil;
import com.liepin.swift.framework.util.UrlUtil;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.unusual.IZookeeperClient;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class ServicePlugin implements IPlugin<Map<String, Map<String, DispatcherMethodBean>>>, IDescribeHook {

    private static final Logger logger = Logger.getLogger(ServicePlugin.class);

    private final Map<String, Map<String, DispatcherMethodBean>> serviceMethodBeanMap = new HashMap<String, Map<String, DispatcherMethodBean>>();

    // private final Map<String, Object> serviceInstanceMap = new
    // HashMap<String, Object>();

    private final Map<String, DispatcherBean> fallbackHandlerMap = new HashMap<>();

    private InterfaceConfigClassReader interfaceConfigClassReader = new InterfaceConfigClassReader();

    public ServicePlugin() {
        // 注册rpc定义上报
        DescribeRegisterContext.addRegisterHook(this);
    }

    public DispatcherMethodBean getServiceMethod(String serviceName, String methodUri) {
        Map<String, DispatcherMethodBean> map = serviceMethodBeanMap.get(serviceName);
        if (map == null) {
            logger.warn("RPC接口serviceName=" + serviceName + "不存在!");
            return null;
        }
        DispatcherMethodBean methodBean = map.get(methodUri);
        if (methodBean == null) {
            logger.warn("RPC接口serviceName=" + serviceName + ", methodUri=" + methodUri + "不存在!");
        }
        return methodBean;
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ServicePlugin init.");
        StringBuilder log = new StringBuilder();
        // 获取接口类
        List<Class<?>> interfaceClazzes = new PluginScan<Class<?>>().scanClazzes(new ServiceClassFilter());
        ServiceImplClassFilter serviceImplClassFilter = new ServiceImplClassFilter(interfaceClazzes);
        // 获取接口实现类
        new PluginScan<Class<?>>().scanClazzes(serviceImplClassFilter);
        Map<Class<?>, List<Class<?>>> scanImplMap = serviceImplClassFilter.getImpl();

        // // 获取接口类
        // List<Class<?>> interfaceClazzes = serviceClassScan.scan();
        // ServiceImplClassScan serviceImplClassScan = new
        // ServiceImplClassScan(interfaceClazzes);
        // // 获取接口实现类
        // Map<Class<?>, List<Class<?>>> scanImplMap =
        // serviceImplClassScan.scanImpl();

        for (Map.Entry<Class<?>, List<Class<?>>> entry : scanImplMap.entrySet()) {
            // 接口类
            Class<?> interfaceClass = entry.getKey();
            List<Class<?>> interfaceImplClassList = entry.getValue();
            // 接口唯一实现类
            Class<?> implClass = interfaceConfigClassReader.getOneImplClass(interfaceClass, interfaceImplClassList);
            // 接口方法型参名列表
            Map<String, String[]> methodParamNames = interfaceConfigClassReader.getMethodParamNames(interfaceClass,
                    implClass, null);

            // 获取接口serviceName
            String serviceName = AnnotationUtil.getServiceName(interfaceClass);
            Object implInstance = SpringContextUtil.getBean(AnnotationUtil.getBeanName(implClass), implClass);
            // serviceInstanceMap.put(serviceName, implInstance);

            Map<String, DispatcherMethodBean> map = new HashMap<String, DispatcherMethodBean>();
            Map<String, DispatcherMethodBean> oldServiceNameMap = serviceMethodBeanMap.put(serviceName, map);
            if (Objects.nonNull(oldServiceNameMap)) {
                throw new RuntimeException("RPC定义接口类有重名的, 类名: " + interfaceClass.getName() + "!");
            }

            // Method[] declaredMethods = interfaceClass.getDeclaredMethods();
            // 支持父类
            Method[] declaredMethods = interfaceClass.getMethods();
            for (Method method : declaredMethods) {
                if (method.getAnnotation(SwiftInterface.class) == null) {
                    continue;
                }
                String methodUri = AnnotationUtil.getMethodUri(method);
                String[] paramNames = methodParamNames.get(methodUri);
                DispatcherMethodBean methodBean = getMethodBean(interfaceClass, method, paramNames);
                methodBean.target = implInstance;
                map.put(methodUri, methodBean);

                // 获取降级信息
                String fallbackHandlerName = AnnotationUtil.getFallbackHandler(method);
                if (!"".equals(fallbackHandlerName)) {
                    DispatcherBean fallbackHandlerBean = getFallbackHandlerBean(implClass, implInstance, method,
                            fallbackHandlerName);
                    String url = UrlUtil.getUrl(UrlUtil.getNamespace4API(), serviceName, methodUri);
                    fallbackHandlerMap.put(url, fallbackHandlerBean);
                }
            }
            log.append("Added {" + interfaceClass.getName()).append("} to Servie\n");
        }

        if (SwiftConfig.enableZookeeper()) {
            register();
        }

        logger.info("ServicePlugin init.");
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        // serviceInstanceMap.clear();
        serviceMethodBeanMap.clear();
        logger.info("ServicePlugin destroy.");
    }

    /**
     * 单个导入接口请求数据
     * 
     * @param serviceName
     * @param serviceInstance
     * @param interfaceClass
     * @param interfaceImplClass
     */
    public void putService(String serviceName, Object serviceInstance, Class<?> interfaceClass,
            Class<?> interfaceImplClass) {
        // serviceInstanceMap.put(serviceName, serviceInstance);
        Map<String, String[]> methodParamNames = interfaceConfigClassReader.getMethodParamNames(interfaceClass,
                interfaceImplClass, null);

        Map<String, DispatcherMethodBean> map = new HashMap<String, DispatcherMethodBean>();
        serviceMethodBeanMap.put(serviceName, map);

        // Method[] declaredMethods = interfaceClass.getDeclaredMethods();
        // 也需要读父类接口
        Method[] declaredMethods = interfaceClass.getMethods();
        for (Method method : declaredMethods) {
            if (method.getAnnotation(SwiftInterface.class) == null) {
                continue;
            }
            String methodUri = AnnotationUtil.getMethodUri(method);
            String[] paramNames = methodParamNames.get(methodUri);
            DispatcherMethodBean methodBean = getMethodBean(interfaceClass, method, paramNames);
            methodBean.target = serviceInstance;
            map.put(methodUri, methodBean);
        }
    }

    /**
     * 
     * @param method
     * @param paramNames
     * @return
     */
    private DispatcherMethodBean getMethodBean(final Class<?> interfaceClass, final Method method,
            final String[] paramNames) {
        DispatcherMethodBean methodBean = new DispatcherMethodBean();
        methodBean.method = method;
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (paramNames.length != genericParameterTypes.length) {
            throw new RuntimeException("RPC定义接口类有重载方法, 导致URI一样无法区分: " + interfaceClass.getName() + ":"
                    + method.getName() + ". 解决方案: 在重载方法的注解@SwiftInterface上添加uri描述，定义接口名.");
        }
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            ParamBean paramBean = new ParamBean();
            List<Class<?>> list = new ArrayList<Class<?>>();
            TypeUtil.recursiveParamClasses(type, list);
            paramBean.parametrized = list.get(0);
            if (list.size() > 1) {
                List<Class<?>> subList = list.subList(1, list.size());
                paramBean.parameterClasses = subList.toArray(new Class<?>[] {});
            }
            methodBean.setParamBean(paramNames[i], paramBean);
        }
        return methodBean;
    }

    private DispatcherBean getFallbackHandlerBean(Class<?> implClass, Object implInstance, Method method,
            String methodName) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?>[] newParameterTypes = new Class<?>[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, newParameterTypes, 0, parameterTypes.length);
        newParameterTypes[newParameterTypes.length - 1] = BlockException.class;
        try {
            Method fallbackMethod = implClass.getDeclaredMethod(methodName, newParameterTypes);
            fallbackMethod.setAccessible(true);
            DispatcherBean bean = new DispatcherBean();
            bean.target = implInstance;
            bean.method = fallbackMethod;
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("获取服务端限流降级方法失败: implClass=" + implClass + ", fallbackHandler=" + methodName, e);
        }
    }

    private void register() {
        boolean flag = serviceMethodBeanMap.size() > 0 && DependencyUtil.hadSwiftPlugin4Rpc();
        try {
            LockUtil.doAndRetry(ProjectId.getProjectName(), new Function() {

                @Override
                public void run() {
                    // zk注册接口标示
                    IZookeeperClient zookeeperClient = ZookeeperFactory.useServerZookeeperWithoutException();
                    String path = "/rpc/server/isInterface/" + ProjectId.getProjectName();
                    Map<String, Object> map = zookeeperClient.getMap(EnumNamespace.PUBLIC, path);
                    map = Optional.ofNullable(map).orElseGet(HashMap::new);
                    map.put("rpc", flag);
                    zookeeperClient.setNode4Map(EnumNamespace.PUBLIC, path, map);
                }

            });
        } catch (Exception e) {
        }
    }

    @Override
    public Map<String, Map<String, DispatcherMethodBean>> getObject() {
        return Collections.unmodifiableMap(serviceMethodBeanMap);
    }

    public Map<String, DispatcherBean> getFallbackHandlerMap() {
        return Collections.unmodifiableMap(fallbackHandlerMap);
    }

    @Override
    public void describe() {
        if (serviceMethodBeanMap.isEmpty()) {
            return;
        }

        // zk注册节点准备
        IZookeeperClient zookeeperClient = ZookeeperFactory.useServerZookeeperWithoutException();
        String path1 = "/rpc/server/interfaceDefinition/service/" + ProjectId.getProjectName();
        String path2 = path1 + "/" + SystemUtil.getInNetworkIp() + ":" + ServicePortUtil.getServerPort();
        zookeeperClient.addNode(EnumNamespace.PUBLIC, path1);

        // 准备数据
        Map<String, Object> definitions = new LinkedHashMap<>();
        serviceMethodBeanMap.entrySet().stream().filter(entry -> entry.getValue().size() > 0).forEach((entry) -> {
            String serviceName = entry.getKey();
            Map<String, DispatcherMethodBean> interfacesMap = entry.getValue();
            // serviceMethodBeanMap.forEach((serviceName, interfacesMap) -> {

            Map<String, Object> definition = new LinkedHashMap<>();
            interfacesMap.forEach((interfaceName, dispatcherMethodBean) -> {

                Map<String, Object> prMap = new LinkedHashMap<>();

                Map<String, String> paramMap = new LinkedHashMap<>();
                dispatcherMethodBean.getParamMap().forEach((param, paramBean) -> {
                    paramMap.put(param, paramBean.toString());
                });

                prMap.put("params", paramMap);
                prMap.put("return", Collections.EMPTY_MAP);// FIXME 添加返回结构
                definition.put("/" + interfaceName, prMap);
            });
            definitions.put("/RPC/" + serviceName, definition);

        });

        // 写zk
        zookeeperClient.setTempNode4Map(EnumNamespace.PUBLIC, path2, definitions);
    }

    @Override
    public String name() {
        return "服务端RPC加载";
    }

}
