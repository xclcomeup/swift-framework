package com.liepin.swift.framework.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.liepin.common.httpclient.HttpSendClient;
import com.liepin.common.httpclient.HttpSendClientFactory;
import com.liepin.swift.core.annotation.SwiftInterface;
import com.liepin.swift.core.annotation.SwiftService;
import com.liepin.swift.framework.rpc.config.InterfaceConfigReader;

/**
 * 根据仓库客户端jar包生成接口文档
 * <p>
 * 多例模式，非线程安全
 * <p>
 * 服务端命令规范：ins-***-platform<br>
 * 客户端命令规范：ins-***-client<br>
 * <p>
 * 使用方式：<br>
 * {@code MavenJarApiCollector collector = new MavenJarApiCollector("ins-user-platform", "1.9.2");}
 * <br>
 * {@code collector.collector();}
 * <p>
 * 生成的接口描述文件存放目录：${catalina.base}/logs<br>
 * 如果是tomcat容器，无需另外设置<br>
 * 如果是单元测试、进程模式，需要设置系统变量，如：-Dcatalina.base=自定义目录<br>
 * 
 * 
 * @author yuanxl
 * 
 */
public class MavenJarApiCollector {

    private static final Logger logger = Logger.getLogger(MavenJarApiCollector.class);

    private static final HttpSendClient HTTP_SEND_CLIENT = HttpSendClientFactory.getNTOInstance();

    /**
     * 正式版本 3.26.1-02版本
     */
    private static final String NEXUS_RELEASES_URI = "http://nexus.tongdao.cn/nexus/repository/releases/com/liepin/";

    public static final String FILE_SEPARATOR = "file.separator";

    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 客户端版本号
     */
    private String version;

    private File tmpDir;
    /**
     * 客户端jar包文件
     */
    private File clientFile;
    /**
     * 客户端jar包接口描述文件
     */
    private File interfaceDefineFile;

    private URLClassLoader classLoader;

    private Document document;
    private Element interfaceDefines;
    private XMLWriter xmlWriter;

    /**
     * 
     * @param projectName 项目名
     * @param version 版本号
     */
    public MavenJarApiCollector(String projectName, String version) {
        int pos = projectName.lastIndexOf("-");
        this.clientName = projectName.substring(0, pos + 1) + "client";
        this.version = version;

        this.tmpDir = new File(
                Optional.ofNullable(System.getProperty("catalina.base")).orElse(System.getProperty("user.dir"))
                        + fileSeparator() + "logs");
        if (!tmpDir.exists() || !tmpDir.isDirectory()) {
            throw new RuntimeException("directory: " + tmpDir.getPath() + " is not exists");
        }

        this.clientFile = new File(tmpDir, clientName + "-" + this.version + ".jar");
        if (clientFile.exists()) {
            clientFile.delete();
        }

        this.interfaceDefineFile = new File(tmpDir, clientName + "_" + this.version + ".xml");
        if (interfaceDefineFile.exists()) {
            interfaceDefineFile.delete();
        }
    }

    /**
     * 收集生成
     */
    public void collector() {
        boolean clean = false;
        // 下载jar包
        if (getMavenJar()) {
            try {
                // 准备xml
                prepareXml();
                // 分析jar包
                analyze();
                // 保存xml
                saveXml();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                clean = true;
            }
        }
        // 释放资源
        clean(clean);
    }

    /**
     * 获取客户端jar包接口描述文件输入流
     * 
     * @return
     */
    public InputStream getInterfaceDefineFile() {
        try {
            return new FileInputStream(interfaceDefineFile);
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 临时获取客户端jar包
     * 
     * @return
     */
    private boolean getMavenJar() {
        String url = NEXUS_RELEASES_URI + clientName + "/" + version + "/" + clientFile.getName();
        URL[] urls = null;
        try {
            urls = new URL[] { new URL(url) };
        } catch (MalformedURLException e) {
            logger.error(url + " new URL() fail", e);
            return false;
        }

        this.classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(clientFile);
            HTTP_SEND_CLIENT.download(url, os);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    /**
     * 分析jar包
     * 
     * @throws Exception
     */
    private void analyze() throws Exception {
        InterfaceConfigReader reader = new InterfaceConfigReader(clientFile, classLoader);
        Map<Class<?>, Map<String, String[]>> interfaceParamsNameMap = reader.read();
        reader.close();

        JarFile jarFile = new JarFile(clientFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String className = jarEntry.getName();
            if (className.endsWith(".class")) {
                className = className.replace("/", ".");
                className = className.substring(0, className.length() - ".class".length());
                Class<?> clazz = null;
                try {
                    clazz = classLoader.loadClass(className);
                    if (clazz.isInterface()) {
                        learnService(clazz, interfaceParamsNameMap.get(clazz));
                    }
                } catch (Exception e) {
                    logger.warn(className + " loadClass fail, ignore", e);
                }
            }
        }
        jarFile.close();
    }

    /**
     * 学习接口类
     * 
     * @param clazz
     * @throws Exception
     */
    private void learnService(final Class<?> clazz, final Map<String, String[]> interfaceParamsNameMap)
            throws Exception {
        if (interfaceParamsNameMap == null) {
            return;
        }
        String interfaceName = clazz.getSimpleName();
        SwiftService swiftService = clazz.getAnnotation(SwiftService.class);
        if (swiftService != null && !"".equals(swiftService.implName())) {
            interfaceName = swiftService.implName();
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            SwiftInterface swiftInterface = method.getAnnotation(SwiftInterface.class);
            if (swiftInterface != null && !"".equals(swiftInterface.uri())) {
                methodName = swiftInterface.uri();
            }
            writeInterfaceDefine(interfaceName, method, interfaceParamsNameMap.get(methodName));
        }
    }

    /**
     * 写接口描述
     * 
     * @param context
     * @param method
     */
    private void writeInterfaceDefine(String context, final Method method, final String[] parameterNames) {
        String methodName = method.getName();
        SwiftInterface swiftInterface = method.getAnnotation(SwiftInterface.class);
        if (swiftInterface != null && !"".equals(swiftInterface.uri())) {
            methodName = swiftInterface.uri();
        }

        Element interfaceDefine = interfaceDefines.addElement("interface_define");
        interfaceDefine.addAttribute("url", "/RPC/" + context + "/" + methodName);

        // doing interface info
        Element _interface = interfaceDefine.addElement("interface");
        writeInterfaceParams(_interface, method, parameterNames, false);

        // doing cases info
        Element cases = interfaceDefine.addElement("cases");
        Element _case = cases.addElement("case");
        writeInterfaceParams(_case, method, parameterNames, true);

        writeInterfaceReturn(_case, method);
    }

    private void writeInterfaceParams(Element _interface, final Method method, final String[] parameterNames,
            boolean hasDefaultValue) {
        Type[] genericParameterTypes = method.getGenericParameterTypes();

        Element params = _interface.addElement("params");
        for (int i = 0; i < parameterNames.length; i++) {
            Element rootParam = params.addElement("param");
            recursiveParam(parameterNames[i], genericParameterTypes[i], null, rootParam, hasDefaultValue);
        }
    }

    private void writeInterfaceReturn(Element _case, final Method method) {
        Element _return = _case.addElement("return");
        // doing response info
        Element response = _return.addElement("response");
        Element params = response.addElement("params");
        Type returnType = method.getGenericReturnType();

        Element param = params.addElement("param");
        recursiveReturn(null, returnType, null, param);

        // doing redirect info
        Element redirect = _return.addElement("redirect");
        redirect.addAttribute("url", "");
    }

    /**
     * 递归处理参数<br>
     * 参数类型：
     * boolean，int，long，byte，string，short,float,double,char,dto,list,map,array
     * ,date
     */
    private void recursiveParam(String name, Type type, Type parentType, Element rootParam, boolean hasDefaultValue) {
        if (name != null) {
            rootParam.addAttribute("name", name);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == List.class) {
                rootParam.addAttribute("type", "list");
                Type[] array = ((ParameterizedType) type).getActualTypeArguments();
                Element nextElement = rootParam.addElement("param");
                recursiveParam(null, array[0], parentType, nextElement, hasDefaultValue);
            } else if (parameterizedType.getRawType() == Map.class) {
                rootParam.addAttribute("type", "map");
                // 忽略
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            rootParam.addAttribute("type", "list");
            Element nextElement = rootParam.addElement("param");
            recursiveParam(null, genericComponentType, parentType, nextElement, hasDefaultValue);
        } else {
            if (type == boolean.class || type == Boolean.class) {
                rootParam.addAttribute("type", "boolean");
            } else if (type == int.class || type == Integer.class) {
                rootParam.addAttribute("type", "int");
            } else if (type == byte.class || type == Byte.class) {
                rootParam.addAttribute("type", "byte");
            } else if (type == short.class || type == Short.class) {
                rootParam.addAttribute("type", "short");
            } else if (type == long.class || type == Long.class) {
                rootParam.addAttribute("type", "long");
            } else if (type == float.class || type == Float.class) {
                rootParam.addAttribute("type", "float");
            } else if (type == double.class || type == Double.class) {
                rootParam.addAttribute("type", "double");
            } else if (type == char.class || type == Character.class) {
                rootParam.addAttribute("type", "char");
            } else if (type == String.class || ((Class<?>) type).isEnum()) {
                rootParam.addAttribute("type", "string");
            } else if (type == java.util.Date.class) {
                rootParam.addAttribute("type", "date");
            } else if (type == List.class) {
                rootParam.addAttribute("type", "list");
            } else if (type == Map.class) {
                rootParam.addAttribute("type", "map");
            } else {
                rootParam.addAttribute("type", "dto");
                rootParam.addAttribute("class", type.getTypeName());
                if (parentType != type) {
                    AccessibleObject[] aos = ((Class<?>) type).getDeclaredFields();
                    for (AccessibleObject ao : aos) {
                        ao.setAccessible(true);
                        Field field = (Field) ao;
                        int mod = field.getModifiers();
                        if (Modifier.isFinal(mod)) {
                            continue;
                        }
                        Type genericType = field.getGenericType();
                        Element nextParam = rootParam.addElement("param");
                        recursiveParam(field.getName(), genericType, type, nextParam, hasDefaultValue);
                    }
                }
            }
        }
        if (name != null && hasDefaultValue) {
            rootParam.addAttribute("value", "");
        }
    }

    /**
     * 递归处理参数<br>
     * 参数类型：
     * boolean，int，long，byte，string，short,float,double,char,dto,list,map,array
     * ,date
     */
    private void recursiveReturn(String name, Type type, Type parentType, Element rootParam) {
        if (name != null) {
            rootParam.addAttribute("name", name);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == List.class) {
                rootParam.addAttribute("type", "list");
                Type[] array = ((ParameterizedType) type).getActualTypeArguments();
                Element nextElement = rootParam.addElement("param");
                recursiveReturn(null, array[0], parentType, nextElement);
            } else if (parameterizedType.getRawType() == Map.class) {
                rootParam.addAttribute("type", "map");
                // 忽略
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            rootParam.addAttribute("type", "list");
            Element nextElement = rootParam.addElement("param");
            recursiveReturn(null, genericComponentType, parentType, nextElement);
        } else {
            if (type == Void.class || type == void.class) {
                rootParam.addAttribute("type", "void");
            } else if (type == boolean.class || type == Boolean.class) {
                rootParam.addAttribute("type", "boolean");
            } else if (type == int.class || type == Integer.class) {
                rootParam.addAttribute("type", "int");
            } else if (type == byte.class || type == Byte.class) {
                rootParam.addAttribute("type", "byte");
            } else if (type == short.class || type == Short.class) {
                rootParam.addAttribute("type", "short");
            } else if (type == long.class || type == Long.class) {
                rootParam.addAttribute("type", "long");
            } else if (type == float.class || type == Float.class) {
                rootParam.addAttribute("type", "float");
            } else if (type == double.class || type == Double.class) {
                rootParam.addAttribute("type", "double");
            } else if (type == char.class || type == Character.class) {
                rootParam.addAttribute("type", "char");
            } else if (type == String.class || ((Class<?>) type).isEnum()) {
                rootParam.addAttribute("type", "string");
            } else if (type == java.util.Date.class) {
                rootParam.addAttribute("type", "date");
            } else if (type == List.class) {
                rootParam.addAttribute("type", "list");
            } else if (type == Map.class) {
                rootParam.addAttribute("type", "map");
            } else {
                rootParam.addAttribute("type", "dto");
                rootParam.addAttribute("class", type.getTypeName());
                if (parentType != type) {
                    AccessibleObject[] aos = ((Class<?>) type).getDeclaredFields();
                    for (AccessibleObject ao : aos) {
                        ao.setAccessible(true);
                        Field field = (Field) ao;
                        int mod = field.getModifiers();
                        if (Modifier.isFinal(mod)) {
                            continue;
                        }
                        Type genericType = field.getGenericType();
                        Element nextParam = rootParam.addElement("param");
                        recursiveReturn(field.getName(), genericType, type, nextParam);
                    }
                }
            }
        }
    }

    /**
     * 准备xml处理
     * 
     * @throws IOException
     */
    private void prepareXml() throws IOException {
        this.document = DocumentHelper.createDocument();
        Element root = DocumentHelper.createElement("root");
        this.document.setRootElement(root);
        this.interfaceDefines = root.addElement("interface_defines");

        OutputFormat format = new OutputFormat();
        format.setEncoding("UTF-8");
        format.setNewlines(true);
        format.setIndent(true);
        format.setPadText(true);

        this.xmlWriter = new XMLWriter(new FileWriter(interfaceDefineFile), format);
    }

    /**
     * 保存xml文件
     * 
     * @throws IOException
     */
    private void saveXml() throws IOException {
        xmlWriter.write(this.document);
        xmlWriter.close();
    }

    /**
     * 文件名分隔符
     * 
     * @return
     */
    public static String fileSeparator() {
        return System.getProperty(FILE_SEPARATOR);
    }

    /**
     * 清理临时文件
     * 
     * @param clean 是否清理接口描述文件
     */
    private void clean(boolean clean) {
        if (clean) {
            interfaceDefineFile.delete();
        }
        clientFile.delete();
    }

}
