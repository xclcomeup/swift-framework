package com.liepin.swift.framework.rpc.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.configuration.ConfigurationException;

/**
 * 读取service客户端接口参数列表
 * <p>
 * 
 * @author yuanxl
 * 
 */
public class InterfaceConfigReader extends InterfaceConfig {

    private InputStream inputStream;

    private JarFile jarFile;

    private URLClassLoader classLoader;

    public InterfaceConfigReader(ClassLoader classLoader) {
        this.inputStream = classLoader.getResourceAsStream(FILENAME);
        load();
    }

    public InterfaceConfigReader(String jarPath) {
        this(new File(jarPath));
    }

    public InterfaceConfigReader(File jarPath) {
        this(jarPath, null);
    }

    public InterfaceConfigReader(File jarPath, URLClassLoader classLoader) {
        try {
            this.jarFile = new JarFile(jarPath);
            JarEntry entry = jarFile.getJarEntry(FILENAME);
            if (entry != null) {
                this.inputStream = jarFile.getInputStream(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取RPC接口文档" + jarPath + "失败: " + e.getMessage(), e);
        }
        this.classLoader = classLoader;
        load();
    }

    public InterfaceConfigReader(InputStream inputStream) {
        this.inputStream = inputStream;
        load();
    }

    private void load() {
        if (inputStream != null) {
            try {
                config.load(inputStream);
            } catch (ConfigurationException e) {
                throw new RuntimeException("加载RPC接口文档" + FILENAME + "失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException e) {
            }
        }
    }

    public String readProjectName() {
        return config.getString(PROJECT_NAME);
    }

    public Map<Class<?>, Map<String, String[]>> read() {
        if (inputStream == null) {
            return null;
        }
        Map<Class<?>, Map<String, String[]>> data = new HashMap<Class<?>, Map<String, String[]>>();
        try {
            Iterator<String> iterator = config.getKeys(SERVICE_PREFIX);
            while (iterator.hasNext()) {
                // 取class信息
                String key = (String) iterator.next();
                String serviceClass = config.getString(key);

                Class<?> clazz = (classLoader != null) ? classLoader.loadClass(serviceClass)
                        : Class.forName(serviceClass);
                Map<String, String[]> map = new HashMap<String, String[]>();
                data.put(clazz, map);

                // 取class的接口方法信息
                Iterator<String> iterator1 = config.getKeys(serviceClass);
                while (iterator1.hasNext()) {
                    String key1 = (String) iterator1.next();
                    int pos = key1.lastIndexOf(".");
                    String methodName = key1.substring(serviceClass.length() + 1, pos);
                    String params = config.getString(key1);
                    map.put(methodName, (!"".equals(params)) ? params.split(",") : new String[] {});
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("基于RPC文档里类名加载接口失败: " + e.getMessage(), e);
        }
        return data;
    }

}
