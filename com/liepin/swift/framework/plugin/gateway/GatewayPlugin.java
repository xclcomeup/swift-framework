package com.liepin.swift.framework.plugin.gateway;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.gateway.api.spi.GwApi;
import com.liepin.gateway.api.util.GwUtil;
import com.liepin.router.discovery.ServicePortUtil;
import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.describe.DescribeRegisterContext;
import com.liepin.swift.framework.describe.IDescribeHook;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean.ParamBean;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.rpc.config.InterfaceConfigClassReader;
import com.liepin.swift.framework.util.LockUtil;
import com.liepin.swift.framework.util.LockUtil.Function;
import com.liepin.swift.framework.util.TypeUtil;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.unusual.IZookeeperClient;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class GatewayPlugin implements IPlugin<Map<String, DispatcherMethodBean>>, IDescribeHook {

    private static final Logger logger = Logger.getLogger(GatewayPlugin.class);

    private final Map<String, DispatcherMethodBean> gatewayMethodBeanMap = new HashMap<String, DispatcherMethodBean>();

    private InterfaceConfigClassReader interfaceConfigClassReader = new InterfaceConfigClassReader();

    public GatewayPlugin() {
        // 注册gateway定义上报
        DescribeRegisterContext.addRegisterHook(this);
    }

    public DispatcherMethodBean getGatewayMethod(String gatewayKey) {
        DispatcherMethodBean methodBean = gatewayMethodBeanMap.get(gatewayKey);
        if (methodBean == null) {
            logger.warn("GateWay接口Key=" + gatewayKey + "不存在!");
            return null;
        }
        return methodBean;
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("GatewayPlugin init.");
        StringBuilder log = new StringBuilder();
        // 获取接口类
        List<Class<?>> interfaceClazzes = new PluginScan<Class<?>>().scanClazzes(new GatewayClassFilter());
        GatewayImplClassFilter gatewayImplClassFilter = new GatewayImplClassFilter(interfaceClazzes);
        // 获取接口实现类
        new PluginScan<Class<?>>().scanClazzes(gatewayImplClassFilter);
        Map<Class<?>, List<Class<?>>> scanImplMap = gatewayImplClassFilter.getImpl();

        // 获取接口类
        // List<Class<?>> interfaceClazzes = gatewayClassScan.scan();
        // GatewayImplClassScan serviceImplClassScan = new
        // GatewayImplClassScan(interfaceClazzes);
        // 获取接口实现类
        // Map<Class<?>, List<Class<?>>> scanImplMap =
        // serviceImplClassScan.scanImpl();

        for (Map.Entry<Class<?>, List<Class<?>>> entry : scanImplMap.entrySet()) {
            // 接口类
            Class<?> interfaceClass = entry.getKey();
            List<Class<?>> interfaceImplClassList = entry.getValue();
            // 接口唯一实现类
            if (interfaceImplClassList.size() != 1) {
                throw new RuntimeException(
                        "GateWay接口有不是唯一的实现类: 接口" + interfaceClass.getName() + ", 实现类" + interfaceImplClassList);
            }
            Class<?> implClass = interfaceImplClassList.get(0);
            // 接口方法型参名列表
            Map<Method, String[]> methodParamNames = interfaceConfigClassReader.getMethodParamNames(interfaceClass,
                    implClass);
            Object implInstance = SpringContextUtil.getBean(AnnotationUtil.getBeanName(implClass), implClass);

            for (Map.Entry<Method, String[]> entry1 : methodParamNames.entrySet()) {
                Method method = entry1.getKey();
                if (!GwUtil.hasGwApi(method)) {
                    continue;
                }
                String apiKey = GwUtil.getGwApiKey(method);
                if (apiKey.equals("")) {
                    throw new RuntimeException(
                            "GateWay接口没定义@GwApi的key: 接口" + interfaceClass.getName() + ", 方法" + method.getName());
                }
                DispatcherMethodBean methodBean = getMethodBean(interfaceClass, method, entry1.getValue());
                methodBean.target = implInstance;
                gatewayMethodBeanMap.put(apiKey, methodBean);
            }
            log.append("Added {" + interfaceClass.getName()).append("} to Gateway\n");
        }

        if (SwiftConfig.enableZookeeper()) {
            register();
        }

        logger.info("GatewayPlugin init.");
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        gatewayMethodBeanMap.clear();
        logger.info("GatewayPlugin destroy.");
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
            throw new RuntimeException("GateWay定义接口类有重载方法: " + interfaceClass.getName() + ":" + method.getName());
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

    @Override
    public Map<String, DispatcherMethodBean> getObject() {
        return Collections.unmodifiableMap(gatewayMethodBeanMap);
    }

    @Override
    public void describe() {
        if (gatewayMethodBeanMap.isEmpty()) {
            return;
        }

        // zk注册节点准备
        IZookeeperClient zookeeperClient = ZookeeperFactory.useServerZookeeperWithoutException();
        String path1 = "/rpc/server/interfaceDefinition/gateway/" + ProjectId.getProjectName();
        String path2 = path1 + "/" + SystemUtil.getInNetworkIp() + ":" + ServicePortUtil.getServerPort();
        zookeeperClient.addNode(EnumNamespace.PUBLIC, path1);

        // 准备数据
        Map<String, Object> definitions = new LinkedHashMap<>();
        gatewayMethodBeanMap.entrySet().stream().forEach((entry) -> {
            String apiKey = entry.getKey();
            DispatcherMethodBean dispatcherMethodBean = entry.getValue();

            GwApi gwApi = GwUtil.getGwApi(dispatcherMethodBean.method);
            Map<String, String> paramMap = new LinkedHashMap<>();
            dispatcherMethodBean.getParamMap().forEach((param, paramBean) -> {
                paramMap.put(param, paramBean.toString());
            });

            Map<String, Object> prMap = new LinkedHashMap<>();
            prMap.put("name", gwApi.name());
            prMap.put("description", gwApi.description());
            prMap.put("params", paramMap);
            prMap.put("return", Collections.EMPTY_MAP);// FIXME 添加返回结构

            definitions.put("/GW/" + apiKey, prMap);
        });

        // 写zk
        zookeeperClient.setTempNode4Map(EnumNamespace.PUBLIC, path2, definitions);
    }

    private void register() {
        boolean flag = gatewayMethodBeanMap.size() > 0;
        try {
            LockUtil.doAndRetry(ProjectId.getProjectName(), new Function() {

                @Override
                public void run() {
                    // zk注册接口标示
                    IZookeeperClient zookeeperClient = ZookeeperFactory.useServerZookeeperWithoutException();
                    String path = "/rpc/server/isInterface/" + ProjectId.getProjectName();
                    Map<String, Object> map = zookeeperClient.getMap(EnumNamespace.PUBLIC, path);
                    map = Optional.ofNullable(map).orElseGet(HashMap::new);
                    map.put("gateway", flag);
                    zookeeperClient.setNode4Map(EnumNamespace.PUBLIC, path, map);
                }

            });
        } catch (Exception e) {
        }
    }

    @Override
    public String name() {
        return "服务端GW加载";
    }

}
