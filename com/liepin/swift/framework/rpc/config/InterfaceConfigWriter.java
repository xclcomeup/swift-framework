package com.liepin.swift.framework.rpc.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

/**
 * 在jar包里封装service客户端接口参数列表
 * 
 * @author yuanxl
 * 
 */
public class InterfaceConfigWriter extends InterfaceConfigClassReader {

    private OutputStream outputStream;

    private int serviceCnt = 1;

    private File file;

    public InterfaceConfigWriter(String path) {
        this.file = new File(path, FILENAME);
        try {
            this.outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("生成RPC文档文件" + path + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 记录项目名
     * 
     * @param projectName
     */
    public void write(String projectName) {
        config.addProperty(PROJECT_NAME, projectName);
    }

    /**
     * 记录一个接口
     * 
     * @param interfaceClass 接口类
     * @param interfaceImplClasses 接口实现类，可能是多个
     */
    public void write(Class<?> interfaceClass, List<Class<?>> interfaceImplClasses) {
        Class<?> implClass = getOneImplClass(interfaceClass, interfaceImplClasses);
        write(interfaceClass, implClass, null);
    }

    /**
     * 记录一个接口
     * 
     * @param interfaceClass 接口类
     * @param interfaceImplClasses 接口实现类，可能是多个
     */
    public void write(Class<?> interfaceClass, Map<Class<?>, File> interfaceImplClasses) {
        ConfigBean bean = getOneImplClass(interfaceClass, interfaceImplClasses);
        write(interfaceClass, bean.getImplClass(), bean.getFile());
    }

    /**
     * service.class.name.{n}={serviceclass}<br>
     * {serviceclass}.{methodname}.{n}={arg1name},{arg2name},{arg3name}<br>
     * 
     * @param interfaceClass
     * @param interfaceImplClass
     * @param implClassFile
     */
    public void write(Class<?> interfaceClass, Class<?> interfaceImplClass, File implClassFile) {
        // 写service class信息
        config.addProperty(SERVICE_PREFIX + "." + serviceCnt++, interfaceClass.getName());

        // 写方法及参数名
        int methodCnt = 1;
        Map<String, String[]> methodParamNames = getMethodParamNames(interfaceClass, interfaceImplClass, implClassFile);
        for (Map.Entry<String, String[]> entry : methodParamNames.entrySet()) {
            String uri = entry.getKey();
            String[] params = entry.getValue();
            config.addProperty(interfaceClass.getName() + "." + uri + "." + methodCnt++, toString(params));
            // 直接输出到控制台
            System.out.println(uri + " " + Arrays.toString(params));
        }
    }

    /**
     * 配置文件
     * 
     * @return
     */
    public File getFile() {
        return file;
    }

    @Override
    public void close() {
        try {
            config.save(outputStream);
        } catch (ConfigurationException e) {
            throw new RuntimeException("保存RPC文档文件失败: " + e.getMessage(), e);
        }
    }

}
