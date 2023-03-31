package com.liepin.swift.framework.plugin;

import org.springframework.context.ApplicationContext;

public interface IPlugin<T> {

    public void init(ApplicationContext applicationContext);

    public void destroy();

    public T getObject();

    public String name();

}
