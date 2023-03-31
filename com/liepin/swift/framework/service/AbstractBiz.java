package com.liepin.swift.framework.service;

import org.apache.log4j.Logger;

import com.liepin.swift.core.log.MonitorLogger;

/**
 * biz层抽象类，便于扩展
 * 
 * @author yuanxl
 * @date 2015-5-12 上午11:13:07
 */
public abstract class AbstractBiz {

    protected final Logger catalinaLog = Logger.getLogger(getClass());

    protected final MonitorLogger monitorLog = MonitorLogger.getInstance();

}
