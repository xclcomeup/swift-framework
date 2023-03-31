package com.liepin.swift.framework.rpc.config;

import java.io.File;

public class ConfigBean {

    private Class<?> implClass;
    private File file;

    public Class<?> getImplClass() {
        return implClass;
    }

    public void setImplClass(Class<?> implClass) {
        this.implClass = implClass;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
