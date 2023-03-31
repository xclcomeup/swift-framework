package com.liepin.swift.framework.boot.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * 暂时不用
 * 
 * @author yuanxl
 *
 */
@WebListener
public class WebxmlListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 暂时没使用场景
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 暂时没使用场景
    }

}
