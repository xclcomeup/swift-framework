package com.liepin.swift.framework.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.ResourceUtils;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.SwiftApplication;
import com.liepin.swift.core.log.MonitorLogger;

public class DependencyUtil {

    private static final Logger logger = Logger.getLogger(DependencyUtil.class);

    /**
     * 判断是否存在客户端打包插件
     * 
     * @param doc
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean hasSwiftPlugin(final Document doc) {
        // Element plugins =
        // doc.getRootElement().element("build").element("plugins");
        // for (Iterator<Element> i = plugins.elementIterator("plugin");
        // i.hasNext();) {
        // Element tmp = (Element) i.next();
        // if ("maven-swift-plugin".equals(tmp.elementText("artifactId"))) {
        // return true;
        // }
        // }

        return Optional.ofNullable(doc.getRootElement().element("build")).map(e -> e.element("plugins"))
                .map(t -> (Iterator<Element>) t.elementIterator("plugin")).map((Iterator<Element> k) -> {
                    while (k.hasNext()) {
                        if ("maven-swift-plugin".equals(((Element) k.next()).elementText("artifactId"))) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
    }

    /**
     * 判断客户端打包插件可以打出rpc客户端
     * 
     * @param doc
     * @return Pair<是否生成客户端,客户端名称>
     */
    @SuppressWarnings("unchecked")
    public static Pair<Boolean, String> hasSwiftPlugin4Rpc(final Document doc) {
        return Optional.ofNullable(doc.getRootElement().element("build")).map(e -> e.element("plugins"))
                .map(t -> (Iterator<Element>) t.elementIterator("plugin")).map((Iterator<Element> k) -> {
                    boolean hasRpc = false;
                    String clientName = null;
                    for (Iterator<Element> i = k; i.hasNext();) {
                        Element plugin = (Element) i.next();
                        if ("maven-swift-plugin".equals(plugin.elementText("artifactId"))) {
                            Element configuration = plugin.element("executions").element("execution")
                                    .element("configuration");
                            if (Objects.nonNull(configuration)) {
                                if (!Boolean.parseBoolean(configuration.elementText("controllerApi"))) {
                                    hasRpc = true;
                                }
                                clientName = configuration.elementText("clientArtifactId");
                                if (Objects.nonNull(clientName)) {
                                    hasRpc = true;
                                }
                            }
                        }
                    }
                    return new Pair<Boolean, String>(hasRpc, clientName);
                }).orElseGet(() -> {
                    return new Pair<Boolean, String>(false, null);
                });

        // if (build != null) {
        // Element plugins = build.element("plugins");
        // if (plugins != null) {
        // for (Iterator<Element> i = plugins.elementIterator("plugin");
        // i.hasNext();) {
        // Element plugin = (Element) i.next();
        // if ("maven-swift-plugin".equals(plugin.elementText("artifactId"))) {
        // Element configuration =
        // plugin.element("executions").element("execution")
        // .element("configuration");
        // if (configuration != null) {
        // if
        // (!Boolean.parseBoolean(configuration.elementText("controllerApi"))) {
        // hasRpc = true;
        // }
        // clientName = configuration.elementText("clientArtifactId");
        // if (clientName != null) {
        // hasRpc = true;
        // }
        // }
        // }
        // }
        // }
        // }
        // return new Pair<Boolean, String>(hasRpc, clientName);
    }

    /**
     * 判断是否在pom.xml里配置了rpc客户端打包插件
     * 
     * @return
     */
    public static boolean hadSwiftPlugin() {
        InputStream pomXmlStream = getPomXmlStream();
        if (pomXmlStream == null) {
            return false;
        }
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(pomXmlStream);
            return hasSwiftPlugin(doc);
        } catch (Exception e) {
            MonitorLogger.getInstance().log("从pom.xml文件提取\"maven-swift-plugin\"插件失败: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean hadSwiftPlugin4Rpc() {
        InputStream pomFile = getPomXmlStream();
        if (pomFile == null) {
            return false;
        }
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(pomFile);
            Pair<Boolean, String> pair = hasSwiftPlugin4Rpc(doc);
            return pair.getFirst();
        } catch (Exception e) {
            MonitorLogger.getInstance().log("从pom.xml文件提取\"maven-swift-plugin\"插件失败: " + e.getMessage(), e);
        }
        return false;
    }


    /**
     * 适配不同的运行模式，获取项目依赖的jar包的名称列表
     * 
     * @return
     */
    public static List<String> getJarNames() {
        List<String> list = getLibJars();
        return (list.size() > 0) ? list : getClassPathJars();
    }

    /**
     * 在jar包运行模式下获取项目依赖的jar包的名称列表
     * 
     * @return
     */
    public static List<String> getLibJars() {
        URL location = DependencyUtil.class.getProtectionDomain().getCodeSource().getLocation();
        List<String> clientNames = new ArrayList<>();
        JarFile jarFile = null;
        try {
            File file = ResourceUtils.getFile(location.getFile());
            File parentFile = file.getParentFile();
            // 获取lib下面的jar包
            String libPath = parentFile.getPath();// file:..../lib
            int pos = libPath.indexOf("!/BOOT-INF/lib");// jar
            pos = (pos == -1) ? libPath.indexOf("!/WEB-INF/lib") : pos;// war
            if (pos != -1) {
                String jarPath = libPath.substring(0, pos);
                jarFile = new JarFile(jarPath);
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
                    if (isJar && name.endsWith(".jar")) {
                        clientNames.add(name);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取lib目录下jar包失败: " + e.getMessage(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
        return clientNames;
    }

    /**
     * 在本地main方法运行模式下获取项目依赖的jar包的名称列表
     * 
     * @return
     */
    public static List<String> getClassPathJars() {
        List<String> clientNames = new ArrayList<>();
        String paths = System.getProperty("java.class.path");
        String[] array = paths.split("\\" + File.pathSeparator);
        for (String path : array) {
            File jarFile = new File(path);
            if (path.endsWith(".jar") && isInnerClient(jarFile.getName())) {
                clientNames.add(jarFile.getName());
            }
        }
        return clientNames;
    }

    /**
     * 获取jar或者war包里面的pom.xml文件流
     * 
     * @return
     */
    public static InputStream getPomXmlStream() {
        URL location = DependencyUtil.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader classLoader = DependencyUtil.class.getClassLoader();
        JarFile jarFile = null;
        InputStream is = null;
        try {
            File file = ResourceUtils.getFile(location.getFile());
            File parentFile = file.getParentFile();
            String libPath = parentFile.getPath();// file:..../lib
            int pos = libPath.indexOf("!/BOOT-INF/lib");// jar
            if (pos == -1) {
                pos = libPath.indexOf("!/WEB-INF/lib");// war
                if (pos == -1) {
                    // 本地classpath
                    return null;
                }
            }
            String jarPath = libPath.substring(0, pos);
            jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            String pomPath = "";
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith("META-INF/maven") && name.endsWith("pom.xml")) {
                    pomPath = name;
                    break;
                }
            }
            String urlString = "jar:file:" + jarPath + "!/" + pomPath;
            is = classLoader.getResourceAsStream(urlString);
        } catch (Exception e) {
            logger.warn("获取pom.xml文件流失败: " + e.getMessage(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
        return is;
    }

    /**
     * 获取jar或者war包里的lib包下的jar文件流列表
     * 
     * @return
     */
    public static Map<String, InputStream> getJarLibStreams() {
        URL location = DependencyUtil.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader classLoader = DependencyUtil.class.getClassLoader();
        Map<String, InputStream> inputStreams = new LinkedHashMap<>();
        JarFile jarFile = null;
        try {
            File file = ResourceUtils.getFile(location.getFile());
            File parentFile = file.getParentFile();
            // 获取lib下面的jar包
            String libPath = parentFile.getPath();// file:..../lib
            int pos = libPath.indexOf("!/BOOT-INF/lib");
            pos = (pos == -1) ? libPath.indexOf("!/WEB-INF/lib") : pos;
            String jarPath = libPath.substring(0, pos);
            jarFile = new JarFile(jarPath);
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
                if (isJar && name.endsWith(".jar")) {
                    String urlString = "jar:file:" + libPath + "/" + name;
                    InputStream openStream = classLoader.getResourceAsStream(urlString);
                    inputStreams.put(name, openStream);
                }
            }
        } catch (Exception e) {
            logger.warn("获取lib目录下jar文件流失败: " + e.getMessage(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
        return inputStreams;
    }

    /**
     * 判断是否是内部服务客户端
     * 
     * @param name
     * @return
     */
    public static boolean isInnerClient(String name) {
        if (name.startsWith("ins-") || name.startsWith("erp-") || name.startsWith("bi-")) {
            if (name.indexOf("-client-") != -1) {
                return true;
            }
        }
        return false;
    }

    private static final String JAR_FILE_LIB = "!/BOOT-INF/lib/";
    private static final String FILE_CLASS = ".class";
    private static final String JAR_FILE_CLASS = "!/BOOT-INF/classes!/";

    public static boolean isInJar(Class<?> clazz) {
        URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (Objects.isNull(resource)) {
            return false;
        } else {
            String path = resource.getPath();
            return path.contains(JAR_FILE_LIB) && path.endsWith(FILE_CLASS);
        }
    }

    public static boolean isInClasses(Class<?> clazz) {
        String path = clazz.getResource(clazz.getSimpleName()).getPath();
        return path.contains(JAR_FILE_CLASS) && path.endsWith(FILE_CLASS);
    }

    /**
     * 判断是否jar包发布
     * 
     * @return
     */
    public static boolean isJarDeploy() {
        return isInJar(SwiftApplication.class);
    }

}
