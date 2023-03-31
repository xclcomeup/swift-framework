package com.liepin.swift.framework.service;

import java.io.Serializable;

/**
 * 
 * @author yuanxl
 *
 */
public class ApiDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接口名称
     */
    private String uriName;

    /**
     * 接口入参描述信息
     */
    private String xmlDesc;

    public String getUriName() {
        return uriName;
    }

    public void setUriName(String uriName) {
        this.uriName = uriName;
    }

    public String getXmlDesc() {
        return xmlDesc;
    }

    public void setXmlDesc(String xmlDesc) {
        this.xmlDesc = xmlDesc;
    }

}
