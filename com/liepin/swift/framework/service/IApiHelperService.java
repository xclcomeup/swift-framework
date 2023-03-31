package com.liepin.swift.framework.service;

import java.io.InputStream;
import java.util.List;

/**
 * API帮助接口
 * 
 * @author yuanxl
 * 
 */
public interface IApiHelperService {

    /**
     * 根据项目名获取项目的所有版本号
     * 
     * @param projectName
     * @return
     */
    public List<String> getProjectVersions(String projectName);

    /**
     * 根据项目名和版本号获取所有接口列表
     * 
     * @param projectName
     * @param version
     * @return
     */
    public InputStream getProjectApis(String projectName, String version);

}
