package com.liepin.swift.framework.monitor.search;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.other.DateUtil;

public class WorkspaceSearch {

    private String rootPath;
    private String classesPath;
    private String libPath;
    private String tempPath;
    private String jarName;
    private boolean containJarPackage;
    private boolean debug;

    // text => search key
    private Map<String, String> keyRefMap = new LinkedHashMap<String, String>();
    // text => [class]
    private Map<String, List<String>> classResultMap = new LinkedHashMap<String, List<String>>();
    // text => {jar, [class]}
    private Map<String, Map<String, List<String>>> jarResultMap = new LinkedHashMap<String, Map<String, List<String>>>();
    // text => [jsp、html、js]
    private Map<String, List<String>> staticResultMap = new LinkedHashMap<>();

    private String[] scanJarNamePrefixs = new String[] { "ins-", "erp-", "bi-" };
    private String[] filterScanJarNamePrefixs = new String[] { "ins-jackson-mapper-asl", "ins-jedis", "ins-zk-client",
            "ins-idp-client", "ins-common-util", "ins-schedule-client", "ins-swift-cache", "ins-swift-core",
            "ins-swift-dao", "ins-swift-distributed", "ins-swift-framework", "ins-swift-router" };

    public WorkspaceSearch() {
        String projectName = ProjectId.getProjectName();
        this.rootPath = SystemUtil.getTomcatRootDirectory("webapps/" + projectName);
        this.classesPath = SystemUtil.getTomcatRootDirectory("webapps/" + projectName + "/WEB-INF/classes");
        this.libPath = SystemUtil.getTomcatRootDirectory("webapps/" + projectName + "/WEB-INF/lib");
        this.tempPath = SystemUtil.getTomcatRootDirectory("temp");
    }

    public void reset() {
        this.jarName = null;
        this.containJarPackage = false;
        this.keyRefMap.clear();
        this.classResultMap.clear();
        this.jarResultMap.clear();
        this.staticResultMap.clear();
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public void setContainJarPackage(boolean containJarPackage) {
        this.containJarPackage = containJarPackage;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setSearchText(String... text) {
        for (int i = 0; i < text.length; i++) {
            keyRefMap.put(text[i], replace(text[i]));
            classResultMap.put(text[i], new ArrayList<String>());
            staticResultMap.put(text[i], new ArrayList<String>());
        }
    }

    private String replace(String text) {
        String[] array = text.split("\\.");
        if (array.length > 2) {
            boolean isClass = false;
            String tmp = array[array.length - 1];
            if (tmp.length() > 0) {
                isClass = Character.isUpperCase(tmp.toCharArray()[0]);
            }
            StringBuilder rtext = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    if (isClass) {
                        rtext.append("/");
                    } else {
                        if (i != array.length - 1) {
                            rtext.append("/");
                        } else {
                            rtext.append(".");
                        }
                    }
                }
                rtext.append(array[i]);

            }
            return rtext.toString();
        } else {
            return text;
        }
    }

    /**
     * 返回 true:不扫描, false:需要扫描
     * 
     * @param filename
     * @return
     */
    private boolean filter(String filename) {
        for (String prefix : scanJarNamePrefixs) {
            if (filename.startsWith(prefix)) {
                for (String prefix1 : filterScanJarNamePrefixs) {
                    if (filename.startsWith(prefix1)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public void search() {
        // 过滤
        if (jarName != null) {
            // 没有引用要查的jar包，忽略此次查询
            if (!isDependency()) {
                return;
            }
        }

        // 扫描classes
        println("分析classes目录开始, start time：" + DateUtil.getCurrentDateTime());
        long start = System.currentTimeMillis();
        List<String> classPaths = scanClass();
        for (String classPath : classPaths) {
            print("开始反编译类：" + getClassName(classPath));
            long s = System.currentTimeMillis();
            String bytecodeString = ClassSearch.javap(classPath);
            long e = System.currentTimeMillis();
            if (bytecodeString != null && bytecodeString.length() != 0) {
                print(" 成功，耗时：" + (e - s) + "ms ｜ 扫描：");
                for (Map.Entry<String, String> entry : keyRefMap.entrySet()) {
                    s = System.currentTimeMillis();
                    int count = Kmp.find(bytecodeString, entry.getValue());
                    e = System.currentTimeMillis();
                    if (count > 0) {
                        String className = getClassName(classPath);
                        // 合并子类
                        int pos = className.indexOf("$");
                        if (pos != -1) {
                            className = className.substring(0, pos);
                        }
                        List<String> list = classResultMap.get(entry.getKey());
                        if (!list.contains(className)) {
                            list.add(className);
                        }
                        print(" text=" + entry.getKey() + " 命中" + count + "处，耗时：" + (e - s) + "ms、");
                    } else {
                        print(" text=" + entry.getKey() + " 无命中，耗时：" + (e - s) + "ms、");
                    }
                }
                println();
            } else {
                println(" 失败.");
            }
            sleep();
        }
        long end = System.currentTimeMillis();
        println("分析classes目录结束，end time：" + DateUtil.getCurrentDateTime() + "，耗时：" + (end - start) + "ms");
        println();

        // 扫描jsp、js、html文件
        println("分析根目录开始, start time：" + DateUtil.getCurrentDateTime());
        start = System.currentTimeMillis();
        List<String> filePaths = scanStaticFile();
        for (String filePath : filePaths) {
            print("扫描：" + filePath);
            for (Map.Entry<String, String> entry : keyRefMap.entrySet()) {
                String content = "";
                try (FileInputStream inputStream = new FileInputStream(new File(filePath))) {
                    content = IOUtils.toString(inputStream, "UTF-8");
                } catch (Exception e) {
                }

                long s = System.currentTimeMillis();
                int count = Kmp.find(content, entry.getKey());
                long e = System.currentTimeMillis();
                if (count > 0) {
                    String path = getRelativePath(filePath);
                    List<String> list = staticResultMap.get(entry.getKey());
                    if (!list.contains(path)) {
                        list.add(path);
                    }
                    print(" text=" + entry.getKey() + " 命中" + count + "处，耗时：" + (e - s) + "ms、");
                } else {
                    print(" text=" + entry.getKey() + " 无命中，耗时：" + (e - s) + "ms、");
                }
            }
            println();
            sleep();
        }
        end = System.currentTimeMillis();
        println("分析根目录结束，end time：" + DateUtil.getCurrentDateTime() + "，耗时：" + (end - start) + "ms");
        println();

        // 扫描lib
        if (containJarPackage) {
            println("分析lib目录开始, start time：" + DateUtil.getCurrentDateTime());
            start = System.currentTimeMillis();
            File[] jarFiles = scanJar();
            if (jarFiles != null && jarFiles.length != 0) {
                for (File file : jarFiles) {
                    if (filter(file.getName())) {
                        continue;
                    }
                    println("开始解析jar包：" + file.getName());
                    long s = System.currentTimeMillis();
                    boolean flag = true;
                    Map<String, List<String>> oneResultMap = null;
                    try {
                        oneResultMap = findJar(file, keyRefMap);
                        if (oneResultMap != null && oneResultMap.size() > 0) {
                            print("查找到结果: ");
                            for (Map.Entry<String, List<String>> entry : oneResultMap.entrySet()) {
                                Map<String, List<String>> map = jarResultMap.get(entry.getKey());
                                if (map == null) {
                                    jarResultMap.put(entry.getKey(), map = new LinkedHashMap<String, List<String>>());
                                }
                                map.put(file.getName(), entry.getValue());
                                print(" text=" + entry.getKey() + " classes=" + entry.getValue() + "、");
                            }
                        }
                    } catch (Exception e) {
                        flag = false;
                    }
                    long e = System.currentTimeMillis();
                    println("结束解析jar包：" + file.getName() + " 解析" + ((flag) ? "成功" : "失败") + "，耗时：" + (e - s) + "ms");
                    println();
                }
            }
            end = System.currentTimeMillis();
            println("分析lib目录结束，end time：" + DateUtil.getCurrentDateTime() + "，耗时：" + (end - start) + "ms");
            println();
        }
    }

    // key => classes
    private Map<String, List<String>> findJar(File file, Map<String, String> keyRefMap) throws Exception {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String className = jarEntry.getName();// com/liepin/kong/filter/KongFilter.class
            if (jarEntry.isDirectory() || !className.endsWith(".class")) {
                continue;
            }
            print("开始反编译jar包中类：" + className);
            // String className1 = className.replaceAll("\\/", ".");
            File classFile = createClassFile(jarFile, jarEntry);
            print(" | 生成临时类：" + classFile.getName() + " ｜ 扫描：");
            String bytecodeString = ClassSearch.javap(classFile.getPath());
            classFile.delete();
            if (bytecodeString != null && bytecodeString.length() != 0) {
                for (Map.Entry<String, String> entry : keyRefMap.entrySet()) {
                    int count = Kmp.find(bytecodeString, entry.getValue());
                    if (count > 0) {
                        List<String> list = result.get(entry.getKey());
                        if (list == null) {
                            result.put(entry.getKey(), list = new ArrayList<String>());
                        }
                        String className1 = getClassName1(className);
                        // 合并子类
                        int pos = className1.indexOf("$");
                        if (pos != -1) {
                            className1 = className1.substring(0, pos);
                        }
                        if (!list.contains(className1)) {
                            list.add(className1);
                        }
                        print(" text=" + entry.getKey() + " 命中" + count + "处、");
                    } else {
                        print(" text=" + entry.getKey() + " 无命中、");
                    }
                }
                println();
            } else {
                println(" 失败.");
            }
            sleep();
        }
        return result;
    }

    private File createClassFile(JarFile jarFile, JarEntry jarEntry) {
        String simpleClassName = jarEntry.getName();
        int pos = jarEntry.getName().lastIndexOf("/");
        if (pos != -1) {
            simpleClassName = jarEntry.getName().substring(pos + 1);
        }
        File tempFile = new File(tempPath, simpleClassName);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = jarFile.getInputStream(jarEntry);
            out = new BufferedOutputStream(new FileOutputStream(tempFile));
            byte[] buffer = new byte[2048];
            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.flush();
        } catch (IOException e) {
            // ignore
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return tempFile;
    }

    private List<String> scanClass() {
        List<String> filePaths = new ArrayList<String>();
        File rootPath = new File(classesPath);
        scan(rootPath, filePaths);
        return filePaths;
    }

    private List<String> scanStaticFile() {
        List<String> filePaths = new ArrayList<>();
        File rootPath = new File(this.rootPath);
        scan1(rootPath, filePaths);
        return filePaths;
    }

    private File[] scanJar() {
        File rootPath = new File(libPath);
        return rootPath.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }

        });
    }

    private static void scan(File root, List<String> filePaths) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scan(file, filePaths);
                } else if (file.isFile()) {
                    if (file.getName().endsWith(".class")) {
                        filePaths.add(file.getPath());
                    }
                }
            }
        }
    }

    private void scan1(File root, List<String> filePaths) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                if (file.isDirectory()) {
                    if ("META-INF".equals(filename) || "WEB-INF".equals(filename)) {
                        continue;
                    }
                    scan1(file, filePaths);
                } else if (file.isFile()) {
                    if (filename.endsWith(".jsp") || filename.endsWith(".js") || filename.endsWith(".html")) {
                        filePaths.add(file.getPath());
                    }
                }
            }
        }
    }

    private boolean isDependency() {
        File rootPath = new File(libPath);
        File[] files = rootPath.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(jarName);
            }

        });
        return (files == null || files.length == 0) ? false : true;
    }

    private String getClassName(String classPath) {
        String className = classPath.substring(classesPath.length(), classPath.indexOf(".class"));
        className = className.replaceAll("\\/", ".");
        return className;
    }

    private String getClassName1(String className) {
        className = className.replaceAll("\\/", ".");
        return className.substring(0, className.lastIndexOf(".class"));
    }

    private String getRelativePath(String path) {
        return path.substring(rootPath.length() - 1);
    }

    private void print(String value) {
        if (debug) {
            System.out.print(value);
        }
    }

    private void println(String value) {
        if (debug) {
            System.out.println(value);
        }
    }

    private void println() {
        if (debug) {
            System.out.println();
        }
    }

    /**
     * text => [class]
     * 
     * @return
     */
    public Map<String, List<String>> analysisClassResult() {
        return classResultMap;
    }

    /**
     * text => {jar, [class]}
     * 
     * @return
     */
    public Map<String, Map<String, List<String>>> analysisJarResult() {
        return jarResultMap;
    }

    /**
     * text => [jsp、html、js]
     * 
     * @return
     */
    public Map<String, List<String>> analysisStaticResult() {
        return staticResultMap;
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
