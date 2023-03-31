package com.liepin.swift.framework.rpc.compile;

/**
 * 字节流类加载器
 * 
 * @author yuanxl
 * 
 */
public class BytesClassLoader extends ClassLoader {

    public BytesClassLoader(final ClassLoader classLoader) {
        super(classLoader);
    }

    public Class<?> findClass(final byte[] bytes) {
        return defineClass(null, bytes, 0, bytes.length);
    }

}
