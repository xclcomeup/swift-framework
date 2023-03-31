package com.liepin.swift.framework.rpc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.swift.core.annotation.SwiftInterface;
import com.liepin.swift.core.annotation.Timeout;
import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.framework.util.TypeUtil;
import com.liepin.swift.framework.util.UrlUtil;

/**
 * 接口元数据
 * 
 * @author yuanxl
 * 
 */
public final class ServiceMetadata {

    private String projectName;// 项目名

    private Class<?> clazz;// 接口类

    private String serviceName;// service名称

    private Map<String, String[]> methodParamMap;// 映射关系: 方法名=>参数名列表

    private Map<Method, String> methodUriMap = new HashMap<Method, String>();// 映射关系：方法=>接口名
    private Map<Method, SwiftInterface> methodAnnotationMap = new HashMap<Method, SwiftInterface>();// 映射关系：方法=>注解
    private Map<Method, Class<?>[]> methodReturnClassMap = new HashMap<Method, Class<?>[]>();// 映射关系：方法=>返回值类型

    /**
     * 启动扫描Jar包创建
     * 
     * @param clazz
     * @param projectName
     * @param methodDatas
     */
    public ServiceMetadata(String projectName, Class<?> clazz, Map<String, String[]> methodDatas) {
        this.projectName = projectName;
        this.clazz = clazz;
        this.methodParamMap = methodDatas;
        // 读取注解信息
        loadAnnotation();
    }

    // private String loadProjectName() {
    // String jarFilePath =
    // clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
    // File jarFile = new File(jarFilePath);
    // if (jarFile.isFile()) {
    // String filename = jarFile.getName();
    // if ((filename.indexOf(".jar")) != -1) {
    // return clientName2ProjectName(filename);
    // }
    // }
    // throw new RuntimeException(this.clazz.getName() +
    // " loadProjectName fail");
    // }

    // /**
    // * 客户端名字转为项目名
    // * <p>
    // * ins-demo-client-1.0.2.jar => ins-demo-platform<br>
    // *
    // * @param name
    // * @return
    // */
    // public static String clientName2ProjectName(String name) {
    // return name.substring(0, name.lastIndexOf("client-")) + "platform";
    // }

    // private Map<String, String[]> loadInterfaceConfig() {
    // InterfaceConfigReader interfaceConfigReader = new
    // InterfaceConfigReader(this.clazz.getClassLoader());
    // Map<Class<?>, Map<String, String[]>> read = interfaceConfigReader.read();
    // interfaceConfigReader.close();
    // Map<String, String[]> map = read.get(this.clazz);
    // if (map == null || map.isEmpty()) {
    // throw new RuntimeException(this.clazz.getName() +
    // " loadInterfaceConfig fail");
    // }
    // return map;
    // }

    /**
     * 分析注解信息
     */
    private void loadAnnotation() {
        this.serviceName = AnnotationUtil.getServiceName(this.clazz);

        Method[] declaredMethods = this.clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            SwiftInterface swiftInterface = method.getAnnotation(SwiftInterface.class);
            if (swiftInterface == null) {
                continue;
            }
            methodAnnotationMap.put(method, swiftInterface);
            String uri = method.getName();
            if (!swiftInterface.uri().equals("")) {
                uri = swiftInterface.uri();
            }
            methodUriMap.put(method, uri);

            // 顺序outputDataClass, elementClass
            List<Class<?>> clazzes = new ArrayList<Class<?>>();
            TypeUtil.recursiveParamClasses(method.getGenericReturnType(), clazzes);
            Class<?>[] returnClasses = clazzes.toArray(new Class<?>[] {});
            methodReturnClassMap.put(method, returnClasses);
        }
    }

    /**
     * 获取请求地址
     * <p>
     * 如： /RPC/userService/getUserByUserId<br>
     * 如： /RPC/IUserService/getUserByUserId<br>
     * 
     * @param method
     * @return
     */
    public String getUri(final Method method) {
        return UrlUtil.getNamespace4API() + serviceName + "/" + methodUriMap.get(method);
    }

    /**
     * 获取地址
     * <p>
     * 如： userService/getUserByUserId<br>
     * 如： IUserService/getUserByUserId<br>
     * 
     * @param method
     * @return
     */
    public String getSimpleUri(final Method method) {
        return serviceName + "/" + methodUriMap.get(method);
    }

    /**
     * 获取限流框架限流粒度标识
     * 
     * @param method
     * @param args
     * @return
     */
    public String getHystrixCommandKey(final Method method, Object[] args) {
        SwiftInterface swiftInterface = methodAnnotationMap.get(method);
        // FIXME 如果对rpc接口对象进行toString()执行，这块会获取不到，抛NPE
        int location = swiftInterface.limitIdentificationParameterlocation();
        String commandKey = getSimpleUri(method);
        if (location != -1 && location < args.length && args[location] != null) {
            commandKey = commandKey + "/" + args[location].toString();
        }
        return commandKey;
    }

    /**
     * 判断方法是否有超时限制
     * 
     * @param method
     * @return
     */
    public Timeout getTimeout(final Method method) {
        SwiftInterface swiftInterface = methodAnnotationMap.get(method);
        return swiftInterface.timeout();
    }

    /**
     * 判断方法是否需要压缩传输
     * 
     * @param method
     * @return
     */
    public boolean isCompress(final Method method) {
        SwiftInterface swiftInterface = methodAnnotationMap.get(method);
        return swiftInterface.compress();
    }

    /**
     * 判断是否应该有超时
     * 
     * @param method
     * @return true:超时、false:永不超时
     */
    public boolean isTimeout(final Method method) {
        SwiftInterface swiftInterface = methodAnnotationMap.get(method);
        if (swiftInterface.compress()) {
            return false;
        } else {
            Timeout timeout = swiftInterface.timeout();
            return Timeout.OVERTIME == timeout;
        }
    }

    /**
     * 判断是否需要客户端缓存
     * 
     * @param method
     * @return
     */
    public boolean isCache(final Method method) {
        SwiftInterface swiftInterface = methodAnnotationMap.get(method);
        return swiftInterface.cache();
    }

    /**
     * 方法返回值类型
     * 
     * @param method
     * @return
     */
    public Class<?>[] getReturnClass(final Method method) {
        return methodReturnClassMap.get(method);
    }

    /**
     * 封装请求参数对象
     * 
     * @param method
     * @param args
     * @return
     */
    public LinkedHashMap<String, Object> packaging(Method method, Object[] args) {
        String uri = methodUriMap.get(method);
        String[] names = methodParamMap.get(uri);
        if (names.length == 0 && args == null) {
            // 无参请求
            return null;
        }
        LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        for (int i = 0; i < names.length; i++) {
            params.put(names[i], args[i]);
        }
        return params;
    }

    /**
     * 接口归属那个项目
     * 
     * @return
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * service接口名
     * 
     * @return
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 接口类
     * 
     * @return
     */
    public Class<?> getClazz() {
        return clazz;
    }

    public Map<Method, SwiftInterface> getMethodAnnotationMap() {
        return methodAnnotationMap;
    }

}
