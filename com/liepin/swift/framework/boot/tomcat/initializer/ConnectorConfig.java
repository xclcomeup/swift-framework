package com.liepin.swift.framework.boot.tomcat.initializer;

import com.liepin.swift.framework.conf.SwiftConfig;

/**
 * tomcat Connector配置
 * 
 * @author yuanxl
 *
 */
public class ConnectorConfig extends SwiftConfig {

    private String URIEncoding;
    private boolean enableLookups;
    private int acceptCount;
    private String compression;
    private String address;
    private int connectionTimeout;
    private int maxKeepAliveRequests;
    private int redirectPort;
    private int keepAliveTimeout;
    private int maxPostSize;
    private int maxHttpHeaderSize;
    private int maxHeaderCount;

    public ConnectorConfig() {
        this.URIEncoding = getValue("tomcat.connector.URIEncoding", "UTF-8");
        this.enableLookups = getBooleanValue("tomcat.connector.enableLookups", false);
        this.acceptCount = getIntValue("tomcat.connector.acceptCount", 100);
        this.compression = getValue("tomcat.connector.compression", "force");// force、off、on
        this.address = getValue("tomcat.connector.address", "0.0.0.0");
        this.connectionTimeout = getIntValue("tomcat.connector.connectionTimeout", 20000);
        this.maxKeepAliveRequests = getIntValue("tomcat.connector.maxKeepAliveRequests", 1024);
        this.redirectPort = getIntValue("tomcat.connector.redirectPort", 8443);
        this.keepAliveTimeout = getIntValue("tomcat.connector.keepAliveTimeout", 15000);
        this.maxPostSize = getIntValue("tomcat.connector.maxPostSize", 2 * 1024 * 1024);
        this.maxHttpHeaderSize = getIntValue("tomcat.connector.maxHttpHeaderSize", 8 * 1024);
        this.maxHeaderCount = getIntValue("tomcat.connector.maxHeaderCount", 100);
    }

    public String getURIEncoding() {
        return URIEncoding;
    }

    public boolean isEnableLookups() {
        return enableLookups;
    }

    public int getAcceptCount() {
        return acceptCount;
    }

    public String getCompression() {
        return compression;
    }

    public String getAddress() {
        return address;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getMaxKeepAliveRequests() {
        return maxKeepAliveRequests;
    }

    public int getRedirectPort() {
        return redirectPort;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public int getMaxPostSize() {
        return maxPostSize;
    }

    public int getMaxHttpHeaderSize() {
        return maxHttpHeaderSize;
    }

    public int getMaxHeaderCount() {
        return maxHeaderCount;
    }

    public String addressErrorPrompt() {
        return "Tomcat的Connector的address参数配置错误: address=" + address
                + "不是合法ip地址, 请在config.properties配置文件里修改tomcat.connector.address的值!";
    }

}
