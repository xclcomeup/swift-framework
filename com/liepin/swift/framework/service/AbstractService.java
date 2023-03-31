package com.liepin.swift.framework.service;

import org.apache.log4j.Logger;

import com.liepin.swift.core.log.MonitorLogger;

/**
 * service层抽象类，便于扩展
 * 
 * @author yuanxl
 * 
 */
public abstract class AbstractService {

    protected final Logger catalinaLog = Logger.getLogger(getClass());

    protected final MonitorLogger monitorLog = MonitorLogger.getInstance();

}
