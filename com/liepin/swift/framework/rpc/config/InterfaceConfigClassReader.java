package com.liepin.swift.framework.rpc.config;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import com.liepin.swift.core.annotation.SwiftService;
import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.framework.rpc.compile.LocalVariableTableParameterNameFileDiscoverer;

/**
 * 读取接口方法参数名信息类
 * 
 * @author yuanxl
 * 
 */
public class InterfaceConfigClassReader extends InterfaceConfig {

    /**
     * 获取Class文件里的符号信息 （“LocalVariableTable”）
     */
    private final LocalVariableTableParameterNameDiscoverer classPathDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    private final LocalVariableTableParameterNameFileDiscoverer fileDiscoverer = new LocalVariableTableParameterNameFileDiscoverer();

    /**
     * 返回一个接口的实现类class，最多一个
     * <p>
     * 启动运行时使用
     * 
     * @param interfaceClass 接口类
     * @param interfaceImplClasses 接口实现类，可能是多个
     */
    public Class<?> getOneImplClass(Class<?> interfaceClass, List<Class<?>> interfaceImplClasses) {
        if (interfaceImplClasses.isEmpty()) {
            throw new RuntimeException(interfaceClass.getName() + "接口缺少一个唯一的实现类!");
        }
        Class<?> implClass = null;
        // 如果有多个实现类
        if (interfaceImplClasses.size() > 1) {
            // 获取注解 @SwiftService 标示的具体实现类
            SwiftService annotation = interfaceClass.getAnnotation(SwiftService.class);
            String implName = "";
            if (annotation == null || (implName = annotation.implName()).equals("")) {
                throw new RuntimeException(interfaceClass.getName() + "接口缺少@SwiftService注解, 因为接口有实现类对外提供服务!");
            }

            for (Class<?> interfaceImplClass : interfaceImplClasses) {
                // 获取spring service层注解，得到beanName
                String beanName = AnnotationUtil.getBeanName(interfaceImplClass);
                if (implName.equals(beanName)) {
                    implClass = interfaceImplClass;
                    break;
                }
            }
            if (implClass == null) {
                throw new RuntimeException(
                        interfaceClass.getName() + "基于接口的beanName或者@SwiftService的implName没有发现对应的实现类!");
            }
        } else {
            implClass = interfaceImplClasses.get(0);
        }
        return implClass;
    }

    /**
     * 打包运行时使用
     * 
     * @param interfaceClass
     * @param interfaceImplClasses
     * @return
     */
    public ConfigBean getOneImplClass(Class<?> interfaceClass, Map<Class<?>, File> interfaceImplClasses) {
        ConfigBean bean = new ConfigBean();
        // 如果有多个实现类
        if (interfaceImplClasses.size() > 1) {
            // 获取注解 @SwiftService 标示的具体实现类
            SwiftService annotation = interfaceClass.getAnnotation(SwiftService.class);
            String implName = "";
            if (annotation == null || (implName = annotation.implName()).equals("")) {
                throw new RuntimeException(interfaceClass.getName() + "接口缺少@SwiftService注解, 因为接口有实现类对外提供服务!");
            }

            for (Map.Entry<Class<?>, File> entry : interfaceImplClasses.entrySet()) {
                Class<?> interfaceImplClass = entry.getKey();
                // 获取spring service层注解，得到beanName
                String beanName = AnnotationUtil.getBeanName(interfaceImplClass);
                if (implName.equals(beanName)) {
                    bean.setImplClass(interfaceImplClass);
                    bean.setFile(entry.getValue());
                    break;
                }
            }
            if (bean.getImplClass() == null) {
                throw new RuntimeException(
                        interfaceClass.getName() + "基于接口的beanName或者@SwiftService的implName没有发现对应的实现类!");
            }
        } else {
            for (Map.Entry<Class<?>, File> entry : interfaceImplClasses.entrySet()) {
                bean.setImplClass(entry.getKey());
                bean.setFile(entry.getValue());
                break;
            }
        }
        return bean;
    }

    /**
     * 方法提供的接口名uri 与 方法参数列表名
     * 
     * @param interfaceClass
     * @param interfaceImplClass
     * @param implClassFile
     */
    public Map<String, String[]> getMethodParamNames(Class<?> interfaceClass, Class<?> interfaceImplClass,
            File implClassFile) {
        Map<String, String[]> paramNames = new HashMap<String, String[]>();
        Map<Method, Class<?>[]> methodMap = new LinkedHashMap<Method, Class<?>[]>();
        // Method[] methods = interfaceClass.getDeclaredMethods();
        // 支持父类
        Method[] methods = interfaceClass.getMethods();
        for (Method method : methods) {
            // 过滤不开放的方法
            if (!AnnotationUtil.hasSwiftInterface(method)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            methodMap.put(method, parameterTypes);
        }

        // methods = interfaceImplClass.getDeclaredMethods();
        // 支持父类
        methods = interfaceImplClass.getMethods();
        // 打包时候循环比较接口方法与实现类方法一致，不涉及性能
        for (Method method : methods) {
            int mod = method.getModifiers();
            // 去掉私有的
            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) {
                continue;
            }
            boolean equal = false;
            Method iMethod = null;
            for (Map.Entry<Method, Class<?>[]> entry : methodMap.entrySet()) {
                // 名称相同 + 参数个数和顺序类型相同
                if (entry.getKey().getName().equals(method.getName())) {
                    if (equals(entry.getValue(), method.getParameterTypes())) {
                        equal = true;
                        iMethod = entry.getKey();
                        break;
                    }
                }
            }
            if (equal) {
                String[] parameterNames = (implClassFile != null)
                        ? fileDiscoverer.getParameterNames(method, implClassFile)
                        : classPathDiscoverer.getParameterNames(method);
                String uri = AnnotationUtil.getMethodUri(iMethod);
                paramNames.put(uri, parameterNames);
            }
        }
        return paramNames;
    }

    /**
     * Gateway 获取 方法=>方法参数名列表 集合
     * 
     * @param interfaceClass
     * @param interfaceImplClass
     */
    public Map<Method, String[]> getMethodParamNames(Class<?> interfaceClass, Class<?> interfaceImplClass) {
        Map<Method, String[]> paramNames = new HashMap<Method, String[]>();
        Map<Method, Class<?>[]> methodMap = new LinkedHashMap<Method, Class<?>[]>();
        // 支持父类
        Method[] methods = interfaceClass.getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            methodMap.put(method, parameterTypes);
        }

        // 支持父类
        methods = interfaceImplClass.getMethods();
        // 打包时候循环比较接口方法与实现类方法一致，不涉及性能
        for (Method method : methods) {
            int mod = method.getModifiers();
            // 去掉私有的
            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) {
                continue;
            }
            boolean equal = false;
            Method iMethod = null;
            for (Map.Entry<Method, Class<?>[]> entry : methodMap.entrySet()) {
                // 名称相同 + 参数个数和顺序类型相同
                if (entry.getKey().getName().equals(method.getName())) {
                    if (equals(entry.getValue(), method.getParameterTypes())) {
                        equal = true;
                        iMethod = entry.getKey();
                        break;
                    }
                }
            }
            if (equal) {
                String[] parameterNames = classPathDiscoverer.getParameterNames(method);
                paramNames.put(iMethod, parameterNames);
            }
        }
        return paramNames;
    }

    /**
     * 判断2个数组内容是否一样
     * 
     * @param arg1
     * @param arg2
     * @return
     */
    private boolean equals(Class<?>[] arg1, Class<?>[] arg2) {
        if (arg1.length != arg2.length) {
            return false;
        }
        for (int i = 0; i < arg1.length; i++) {
            if (arg1[i] != arg2[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
    }

}
