package com.liepin.swift.framework.rpc.compile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * 动态文件类加载器
 * 
 * @author yuanxl
 * 
 */
public class FileClassLoader extends ClassLoader {

    private String classPath;

    /**
     * 已加载的class name
     */
    private final Map<String, Class<?>> loadedClassMap = new HashMap<String, Class<?>>();

    public FileClassLoader(final ClassLoader classLoader, String classPath) {
        super(classLoader);
        this.classPath = classPath;
    }

    /**
     * 根据类名或字节流查找类
     * 
     * @param bytes
     * @param className
     * @return
     */
    public Class<?> findClass(final byte[] bytes, String className) {
        if (className != null) {
            if (loadedClassMap.containsKey(className)) {
                return loadedClassMap.get(className);
            }
        }
        Class<?> clazz = defineClass(null, bytes, 0, bytes.length);
        // 记录已加载类
        logClass(clazz);
        return clazz;
    }

    /**
     * 复写自定义扩展类查找接口<br>
     * 根据类名从目录查找类<br>
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = loadClassData(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return super.defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * 根据类名获取类的字节流
     * <p>
     * 例如：com.liepin.demo.service.IUserService => byte[]
     * 
     * @param className
     * @return
     * @throws
     */
    private byte[] loadClassData(String className) {
        File file = getFile(className);
        byte[] bytes = null;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (Exception e) {
            throw new RuntimeException("read " + className + " to byte[] fail", e);
        }
        return bytes;
    }

    /**
     * 根据类名字符串返回一个 File 对象
     * 
     * @param name
     * @return
     */
    private File getFile(String name) {
        // TODO 优化replace
        String _classPath = classPath.replaceAll("[\\\\]", "/");
        int offset = _classPath.lastIndexOf("/");
        name = name.replaceAll("[.]", "/");
        if (offset != -1 && offset < _classPath.length() - 1) {
            _classPath += "/";
        }
        _classPath += name + ".class";
        return new File(_classPath);
    }

    private void logClass(Class<?> clazz) {
        loadedClassMap.put(clazz.getName(), clazz);
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || Object.class == superclass) {
            return;
        } else {
            logClass(superclass);
        }
    }

}
