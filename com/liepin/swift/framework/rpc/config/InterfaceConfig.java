package com.liepin.swift.framework.rpc.config;

import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 文件格式：<br>
 * service.class.name.{n}={serviceclass}<br>
 * {serviceclass}.{methodname}.{n}={arg1name},{arg2name},{arg3name}<br>
 * 
 * @author yuanxl
 * 
 */
public abstract class InterfaceConfig {

    /**
     * 文件名
     */
    public static final String FILENAME = "interface.properties";

    protected static final String SERVICE_PREFIX = "service.class.name";
    
    protected static final String PROJECT_NAME = "project.name";

    protected PropertiesConfiguration config;

    protected InterfaceConfig() {
        this.config = new PropertiesConfiguration();
        this.config.setListDelimiter((char) 0);
    }

    public abstract void close();

    public String toString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

}
