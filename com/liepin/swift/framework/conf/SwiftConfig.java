package com.liepin.swift.framework.conf;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.conf.SystemUtil;

public class SwiftConfig {

    // 客户端打包目录
    public static final String[] CLIENT_INCLUDES = { "**/dto/**", "**/enums/**", "**/service/*",
            "interface.properties" };

    // 接口及实现类目录
    public static final String CLIENT_SERVICE = "**/service/*";
    public static final String CLIENT_SERVICE_IMPL = "**/service/impl/*";

    // mvc接口目录
    public static final String CONTROLLER_API_INCLUDES = "com/liepin/**/controller/*";

    // 正式版本中央仓库地址
    public static final String NEXUS_RELEASES_URI = "http://nexus.tongdao.cn/nexus/repository/releases/com/liepin/";

    // 静态资源文件请求URL后缀
    public static final String STATIC_REQUEST_SUFFIXS = ".shtml;.html;.htm;.css;.js;.gif;.png;.jpeg;.jpg;.bmp;.ico;.txt;.xml";
    // config.properties的servlet.defalut.urlPattern=none 表示静态资源处理器不处理任何静态请求
    public static final String STATIC_REQUEST_SUFFIX_NONE = "none";

    /**
     * 静态资源文件请求URL后缀
     * 
     * @return
     */
    public static String getServletStaticUrlPattern() {
        return PropUtil.getInstance().getString("servlet.defalut.urlPattern", STATIC_REQUEST_SUFFIXS);
    }

    // 心跳控制接口参数密钥
    public static final String HEARTBEAT_SECRET_KEY = "1b4jf40fk";

    public static String getValue(String key, String defaultValue) {
        return PropUtil.getInstance().getString(key, defaultValue);
    }

    public static int getIntValue(String key, int defaultValue) {
        return PropUtil.getInstance().getInt(key, defaultValue);
    }

    public static long getLongValue(String key, long defaultValue) {
        return PropUtil.getInstance().getLong(key, defaultValue);
    }

    public static boolean getBooleanValue(String key, boolean defaultValue) {
        return PropUtil.getInstance().getBoolean(key, defaultValue);
    }

    /**
     * 启动校验数据库ip授权
     * 
     * @return
     */
    public static boolean enableDaoIpAuth() {
        return getBooleanValue("enable.dao.ipAuth", true);
    }

    /**
     * mvc普通页面请求是否走雨燕协议包装
     * 
     * @return
     */
    public static boolean enableServletDefalutSwiftAgreePack() {
        // 应该默认true。但是为了向下兼容，改成让业务配置才生效
        return getBooleanValue("servlet.defalut.swiftAgreePack.enable", false);
    }

    /**
     * 设置线下环境启动中是否加载jsp资源
     * 
     * @see #enableStartupPreloadInOffline()
     * @return
     */
    @Deprecated
    public static boolean enablePreload4JspInOffline() {
        return getBooleanValue("plugin.preload.jsp.offline.enable", true);
    }

    /**
     * 设置线下环境启动中是否加载静态资源（js、css）
     * 
     * @see #enableStartupPreloadInOffline()
     * @return
     */
    @Deprecated
    public static boolean enablePreload4ResourceInOffline() {
        return getBooleanValue("plugin.preload.resource.offline.enable", true);
    }

    /**
     * 在线下环境服务启动是否对依赖资源进行预加载，默认不加载 (提升启动速度)
     * 
     * @return
     */
    public static boolean enableStartupPreloadInOffline() {
        return getBooleanValue("startup.preload.offline.enable", false);
    }

    /**
     * 服务启动是否对依赖资源进行预加载
     * 
     * @return
     */
    public static boolean enableStartupPreload() {
        return SystemUtil.isOnline() || (SystemUtil.isOffline() && enableStartupPreloadInOffline());
    }

    /**
     * entity与数据源是否一致性加载，默认是
     * 
     * @return
     */
    public static boolean enableEntityAndDataSourceConsistencyLoad() {
        return getBooleanValue("entityAndDataSourceConsistencyLoad", true);
    }

    /**
     * json请求是否走雨燕协议，全局开关
     * 
     * @return
     */
    public static boolean enableServletJsonSwiftAgreePack() {
        return getBooleanValue("servlet.json.swiftAgreePack.enable", true);
    }

    /**
     * 非标准协议异常error级别
     * <p>
     * true: 输出错误日志和cat异常<br>
     * false: 输出monitor日志<br>
     * 
     * @return
     */
    public static boolean noStandardProtocolError() {
        return getBooleanValue("noStandardProtocolError", true);
    }

    /**
     * 配置是否依赖zookeeper
     * 
     * @return
     */
    public static boolean enableZookeeper() {
        return getBooleanValue("Zookeeper.Enabled", true);
    }

    /**
     * 是否在rpc请求时检查透传的必要参数，默认检查并输出错误日志
     * 
     * @return
     */
    public static boolean enableRpcParamsCheck() {
        return getBooleanValue("rpcParamsCheck.enable", true);
    }

}
