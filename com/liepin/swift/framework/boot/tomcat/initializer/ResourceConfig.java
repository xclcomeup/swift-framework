package com.liepin.swift.framework.boot.tomcat.initializer;

import com.liepin.swift.framework.conf.SwiftConfig;

/**
 * tomcat Resource配置
 * 
 * @author yuanxl
 *
 */
public class ResourceConfig extends SwiftConfig {

    private boolean cachingAllowed;
    private long cacheMaxSize;

    public ResourceConfig() {
        this.cachingAllowed = getBooleanValue("tomcat.resource.cachingAllowed", true);
        this.cacheMaxSize = getLongValue("tomcat.resource.cacheMaxSize", 10 * 1024);// 单位字节，默认10K
    }

    public boolean isCachingAllowed() {
        return cachingAllowed;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

}
