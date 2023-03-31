package com.liepin.swift.framework.dao.initializer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.liepin.common.conf.PropUtil;
import com.liepin.swift.framework.conf.SwiftConfig;

public class DaoConfig extends SwiftConfig {

    private static final Map<String, String> db2initialSizeMap = new HashMap<>();

    private String url;
    private String slaves;
    private String initialSize;
    private String minIdle;
    private String maxIdle;
    private String maxWait;
    private String maxActive;
    private String removeAbandoned;
    private String removeAbandonedTimeout;
    private String logAbandoned;
    private String validationQuery;
    private String testWhileIdle;
    private String testOnBorrow;
    private String testOnReturn;
    private String timeBetweenEvictionRunsMillis;
    private String minEvictableIdleTimeMillis;
    private String validationInterval;
    private String validationQueryTimeout;

    static {
        Iterator<String> logConfig = PropUtil.getInstance().getKeys("datasource.initialSize.db");
        while (logConfig.hasNext()) {
            String key = logConfig.next();
            String value = PropUtil.getInstance().get(key);
            String name = key.substring("datasource.initialSize.db".length() + 1);
            db2initialSizeMap.put(name, value);
        }
    }

    public DaoConfig() {
        this.url = getValue("datasource.jdbc.url",
                "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&autoReconnectForPools=true&failOverReadOnly=false&maxReconnects=3&initialTimeout=2&connectTimeout=500&socketTimeout=10000&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true&serverTimezone=GMT%2B8");
        this.slaves = getValue("datasource.jdbc.slaves", "ro:0-1");// rw、ro
        this.initialSize = SwiftConfig.enableStartupPreload() ? getValue("datasource.initialSize", "5") : "2"; // 线下环境减少启动连接数（减少资源使用、提升启动速度）
        this.minIdle = SwiftConfig.enableStartupPreload() ? getValue("datasource.minIdle", "5") : "2";// 线下环境减少启动连接数（减少资源使用、提升启动速度）
        this.maxIdle = getValue("datasource.maxIdle", "50");
        this.maxWait = getValue("datasource.maxWait", "2000");
        this.maxActive = getValue("datasource.maxActive", "500");
        this.removeAbandoned = getValue("datasource.removeAbandoned", "true");
        this.removeAbandonedTimeout = getValue("datasource.removeAbandonedTimeout", "60");
        this.logAbandoned = getValue("datasource.logAbandoned", "false");// 如果配置true，dao模块性能开销增加50%
        this.validationQuery = getValue("datasource.validationQuery", "select 1");
        this.testWhileIdle = getValue("datasource.testWhileIdle", "false");
        this.testOnBorrow = getValue("datasource.testOnBorrow", "true");
        this.testOnReturn = getValue("datasource.testOnReturn", "false");
        this.timeBetweenEvictionRunsMillis = getValue("datasource.timeBetweenEvictionRunsMillis", "600000");
        this.minEvictableIdleTimeMillis = getValue("datasource.minEvictableIdleTimeMillis", "3600000");
        this.validationInterval = getValue("datasource.validationInterval", "60000");
        this.validationQueryTimeout = getValue("datasource.validationQueryTimeout", "1");
    }

    public DaoConfig(String jndiName) {
        this();
        db2initialSizeMap.forEach((k, v) -> {
            if (jndiName.contains(k)) {
                this.initialSize = v;
                this.minIdle = v;
            }
        });
    }

    public String getUrl() {
        return url;
    }

    public String getSlaves() {
        return slaves;
    }

    public String getInitialSize() {
        return initialSize;
    }

    public String getMinIdle() {
        return minIdle;
    }

    public String getMaxIdle() {
        return maxIdle;
    }

    public String getMaxWait() {
        return maxWait;
    }

    public String getMaxActive() {
        return maxActive;
    }

    public String getRemoveAbandoned() {
        return removeAbandoned;
    }

    public String getRemoveAbandonedTimeout() {
        return removeAbandonedTimeout;
    }

    public String getLogAbandoned() {
        return logAbandoned;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public String getTestWhileIdle() {
        return testWhileIdle;
    }

    public String getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public String getValidationInterval() {
        return validationInterval;
    }

    public String getValidationQueryTimeout() {
        return validationQueryTimeout;
    }

    public String getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public String getTestOnBorrow() {
        return testOnBorrow;
    }

    public String getTestOnReturn() {
        return testOnReturn;
    }

}
