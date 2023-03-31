package com.liepin.swift.framework.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.web.bind.annotation.RequestMapping;

import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.rpc.compile.FileClassLoader;
import com.liepin.swift.framework.rpc.compile.LocalVariableTableParameterNameFileDiscoverer;

/**
 * maven打包模块使用，自动生成controller代码的API文档
 * <p>
 * 多例模式，非线程安全
 * <p>
 * 使用方式：<br>
 * {@code ControllerApiCollector collector = new ControllerApiCollector("ins-user-platform", "1.9.2", classesPath);}
 * <br>
 * {@code collector.collector();}
 * <p>
 * 生成的接口描述文件存放在classes目录下<br>
 * 
 * @author yuanxl
 * 
 */
public class ControllerApiCollector {

    private static final Logger logger = Logger.getLogger(ControllerApiCollector.class);

    private String[] includes = new String[] { SwiftConfig.CONTROLLER_API_INCLUDES };
    private static final String[] DEFAULT_EXCLUDES = new String[] {};

    private File classesDir;

    private FileClassLoader fileClassLoader;

    private Map<Class<?>, File> controllerClasses = new HashMap<Class<?>, File>();

    private final LocalVariableTableParameterNameFileDiscoverer fileDiscoverer = new LocalVariableTableParameterNameFileDiscoverer();

    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 客户端版本号
     */
    private String version;

    /**
     * 客户端jar包接口描述文件
     */
    private File interfaceDefineFile;

    private Document document;
    private Element interfaceDefines;
    private XMLWriter xmlWriter;

    private JavadocCollecter javadocCollecter;
    private Map<String, Map<String, List<String>>> javadocMap;


    /**
     * 
     * @param projectName 项目名
     * @param version 版本号
     * @param classes class根目录
     * @param srcDir 源代码根目录
     */
    public ControllerApiCollector(String projectName, String version, File directory, File srcDir) {
        int pos = projectName.lastIndexOf("-");
        this.clientName = projectName.substring(0, pos + 1) + "client";
        this.version = version;

        this.interfaceDefineFile = new File(directory.getParentFile(), clientName + "_" + this.version + ".xml");
        if (interfaceDefineFile.exists()) {
            interfaceDefineFile.delete();
        }
        this.classesDir = directory;
        if (!classesDir.exists()) {
            throw new RuntimeException(directory + " not exists.");
        }
        if (!classesDir.isDirectory()) {
            throw new RuntimeException(directory + " not directory.");
        }
        this.fileClassLoader = new FileClassLoader(Thread.currentThread().getContextClassLoader(),
                classesDir.getPath());
        this.javadocCollecter = new JavadocCollecter(srcDir);
    }

    /**
     * 收集生成
     */
    public void collector() {
        boolean clean = false;
        if (scan()) {
            try {
                // 准备xml
                prepareXml();
                // 提取javadoc
                javadocMap = javadocCollecter.collector();
                // 分析controller类
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
     * 分析class
     * 
     * @throws Exception
     */
    private void analyze() throws Exception {
        for (Map.Entry<Class<?>, File> entry : controllerClasses.entrySet()) {
            try {
                learnService(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                logger.warn(entry.getKey() + " loadClass fail, ignore", e);
            }
        }
    }

    /**
     * 学习接口类
     * 
     * @param clazz
     * @throws Exception
     */
    private void learnService(final Class<?> clazz, final File file) throws Exception {
        String interfaceName = AnnotationUtil.getRequestMapping(clazz)[0];
        Method[] methods = clazz.getDeclaredMethods();

        Map<String, List<String>> javadoc = javadocMap.get(clazz.getSimpleName());

        for (Method method : methods) {
            if (method.getAnnotation(RequestMapping.class) != null) {
                String[] parameterNames = fileDiscoverer.getParameterNames(method, file);
                String[] annationNames = getParamAnnations(method);
                writeInterfaceDefine(interfaceName + AnnotationUtil.getRequestMapping(method)[0], method,
                        parameterNames, annationNames, javadoc.get(method.getName()));
            }
        }
    }

    /**
     * 写接口描述
     * 
     * @param context
     * @param method
     * @param parameterNames
     * @param annationNames
     * @param notes
     */
    private void writeInterfaceDefine(String context, final Method method, final String[] parameterNames,
            String[] annationNames, List<String> notes) {
        Element interfaceDefine = interfaceDefines.addElement("interface_define");
        interfaceDefine.addAttribute("url", context);

        // doing javadoc
        Element javadoc = interfaceDefine.addElement("javadoc");
        StringBuilder text = new StringBuilder();
        if (notes != null) {
            for (String line : notes) {
                text.append(line);
                text.append("\n");
            }
        }
        javadoc.addCDATA(text.toString());

        // doing interface info
        Element _interface = interfaceDefine.addElement("interface");
        writeInterfaceParams(_interface, method, parameterNames, annationNames, false);

        // doing cases info
        Element cases = interfaceDefine.addElement("cases");
        Element _case = cases.addElement("case");
        writeInterfaceParams(_case, method, parameterNames, annationNames, true);

        writeInterfaceReturn(_case, method);
    }

    private void writeInterfaceParams(Element _interface, final Method method, final String[] parameterNames,
            String[] annationNames, boolean hasDefaultValue) {
        Type[] genericParameterTypes = method.getGenericParameterTypes();

        Element params = _interface.addElement("params");
        for (int i = 0; i < parameterNames.length; i++) {
            Element rootParam = params.addElement("param");
            recursiveParam(parameterNames[i], annationNames[i], genericParameterTypes[i], rootParam, hasDefaultValue,
                    false);
        }
    }

    private void writeInterfaceReturn(Element _case, final Method method) {
        Element _return = _case.addElement("return");
        // doing response info
        Element response = _return.addElement("response");
        Element params = response.addElement("params");
        Type returnType = method.getGenericReturnType();

        Element param = params.addElement("param");
        recursiveReturn(null, returnType, param, false);

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
    private void recursiveParam(String name, String annationName, Type type, Element rootParam, boolean hasDefaultValue,
            boolean isDto) {
        if (name != null) {
            rootParam.addAttribute("name", name);
        }
        if (annationName != null) {
            rootParam.addAttribute("annotation", annationName);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == List.class) {
                rootParam.addAttribute("type", "list");
                Type[] array = ((ParameterizedType) type).getActualTypeArguments();
                Element nextElement = rootParam.addElement("param");
                recursiveParam(null, null, array[0], nextElement, hasDefaultValue, isDto);
            } else if (parameterizedType.getRawType() == Map.class) {
                rootParam.addAttribute("type", "map");
                // 忽略
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            rootParam.addAttribute("type", "list");
            Element nextElement = rootParam.addElement("param");
            recursiveParam(null, null, genericComponentType, nextElement, hasDefaultValue, isDto);
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
                if (!isDto) {
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
                        recursiveParam(field.getName(), getParamAnnations(field.getAnnotations()), genericType,
                                nextParam, hasDefaultValue, true);
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
    private void recursiveReturn(String name, Type type, Element rootParam, boolean isDto) {
        if (name != null) {
            rootParam.addAttribute("name", name);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == List.class) {
                rootParam.addAttribute("type", "list");
                Type[] array = ((ParameterizedType) type).getActualTypeArguments();
                Element nextElement = rootParam.addElement("param");
                recursiveReturn(null, array[0], nextElement, isDto);
            } else if (parameterizedType.getRawType() == Map.class) {
                rootParam.addAttribute("type", "map");
                // 忽略
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            rootParam.addAttribute("type", "list");
            Element nextElement = rootParam.addElement("param");
            recursiveReturn(null, genericComponentType, nextElement, isDto);
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
                if (!isDto) {
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
                        recursiveReturn(field.getName(), genericType, nextParam, true);
                    }
                }
            }
        }
    }

    private String[] getParamAnnations(final Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] array = new String[parameterAnnotations.length];

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] pa = parameterAnnotations[i];
            array[i] = getParamAnnations(pa);
        }
        return array;
    }

    private String getParamAnnations(Annotation[] annotations) {
        if (annotations.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < annotations.length; j++) {
            Annotation pa1 = annotations[j];
            sb.append(pa1.annotationType().getSimpleName());
            if (j != annotations.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
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
     * 清理临时文件
     * 
     * @param clean 是否清理接口描述文件
     */
    private void clean(boolean clean) {
        if (clean) {
            interfaceDefineFile.delete();
        }
    }

    private boolean scan() {
        try {
            // 取接口文件
            List<File> controllerFiles = directoryScan(includes);
            if (controllerFiles.isEmpty()) {
                return false;
            }

            // 类加载
            for (File controllerFile : controllerFiles) {
                Class<?> controllerClass = forClass(controllerFile, getClassName(controllerFile));
                // 过滤非打包接口类
                if (!AnnotationUtil.isController(controllerClass)) {
                    continue;
                }
                controllerClasses.put(controllerClass, controllerFile);
            }
            return controllerClasses.size() > 0;
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 过滤扫描目录
     * 
     * @return
     * @throws IOException
     */
    private List<File> directoryScan(String[] includes) throws IOException {
        List<File> files = new ArrayList<File>();
        PlexusIoFileResourceCollection collection = new PlexusIoFileResourceCollection();
        collection.setIncludes(includes);
        collection.setExcludes(DEFAULT_EXCLUDES);
        collection.setBaseDir(this.classesDir);
        collection.setFileSelectors(null);
        collection.setIncludingEmptyDirectories(true);
        collection.setPrefix("");
        collection.setCaseSensitive(true);
        collection.setUsingDefaultExcludes(true);
        Iterator<?> resources = collection.getResources();
        while (resources.hasNext()) {
            PlexusIoFileResource resource = (PlexusIoFileResource) resources.next();
            if (resource.isDirectory()) {
                continue;
            }
            files.add(resource.getFile());
            // 控制台输出
            System.out.println("classes directory scan add: " + resource.getFile());
        }
        return files;
    }


    /**
     * 从文件转换为类
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private Class<?> forClass(File file, String className) throws IOException {
        byte[] newInterfaceClassbytes = FileUtils.readFileToByteArray(file);
        return fileClassLoader.findClass(newInterfaceClassbytes, className);
    }

    /**
     * 根据文件路径反解析文件类名
     * 
     * @param file
     * @return
     */
    private String getClassName(File file) {
        String temp = getRelativePath(file);
        temp = temp.replaceAll("[\\" + File.separator + "]", ".");
        temp = temp.substring(0, temp.length() - ".class".length());
        return temp;
    }

    /**
     * 相对路径
     * 
     * @param file
     * @return
     */
    private String getRelativePath(File file) {
        String classPath = classesDir.getPath();
        String filePath = file.getPath();
        return filePath.substring(classPath.length() + 1);
    }

}
