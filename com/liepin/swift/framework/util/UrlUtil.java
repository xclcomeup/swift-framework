package com.liepin.swift.framework.util;

import java.util.HashSet;
import java.util.Set;

import com.liepin.swift.core.consts.Const;
import com.liepin.swift.framework.mvc.filter.handler.InnerFilterHandler.InnerHandler;

public class UrlUtil {

    public static final String getNamespace4API() {
        return "/" + Const.NAMESPACE_API + "/";
    }

    public static final String getNamespace4APIDefine() {
        return "/" + Const.NAMESPACE_API_DEFINE + "/";
    }

    public static final String getNamespace4Monitor() {
        return "/" + Const.NAMESPACE_MONITOR + "/";
    }

    public static final String getNamespace4GWAPI() {
        return "/" + Const.NAMESPACE_GW_API + "/";
    }

    /**
     * 判断是否RPC接口
     * <p>
     * 例如：/RPC/ILikeLogService/selectLL
     * 
     * @param servletPath
     * @return
     */
    public static boolean isRPC(String servletPath) {
        return servletPath.startsWith(getNamespace4API());
    }

    /**
     * 判断是否GW接口
     * <p>
     * 例如：/GW/com.liepin.c.user.get-my-name
     * 
     * @param servletPath
     * @return
     */
    public static boolean isGW(String servletPath) {
        return servletPath.startsWith(getNamespace4GWAPI());
    }

    private static final Set<String> INNER_SERVLETPATHS = new HashSet<String>();

    static {
        for (InnerHandler innerHandler : InnerHandler.values()) {
            INNER_SERVLETPATHS.add(innerHandler.path());
        }
        INNER_SERVLETPATHS.add("/hystrix.stream");
    }

    /**
     * 判断是否内部接口
     * <p>
     * 目前包括/monitor<br>
     * 如：<br>
     * /monitor/jvm.do<br>
     * /monitor/http.do<br>
     * /monitor/em.do<br>
     * /monitor/isFlow.do<br>
     * <p>
     * /service/healthstatus.do<br>
     * /hystrix.stream<br>
     * 
     * @param servletPath
     * @return
     */
    public static boolean isInner(String servletPath) {
        return servletPath.startsWith(getNamespace4Monitor()) || INNER_SERVLETPATHS.contains(servletPath);
    }

    /**
     * 去掉命令空间
     * <p>
     * 例如: /GW/xxxxx => /xxxxx
     * 
     * @param servletPath
     * @return
     */
    public static String compile(String servletPath, String namespace) {
        return servletPath.substring(namespace.length() - 1);
    }

    /**
     * 例如: /GW/xxxxx => xxxxx
     * 
     * @param servletPath
     * @param namespace
     * @return
     */
    public static String removeNamespace(String servletPath, String namespace) {
        return servletPath.substring(namespace.length());
    }

    /**
     * 解析请求路径=> [service, method]<br>
     * 例如：/userService/getDtoByUserId => [userService, getDtoByUserId]<br>
     * 
     * @param servletPath
     * @return
     */
    public static String[] uncompile(String servletPath) {
        return servletPath.substring(1).split("\\/");
    }

    /**
     * 生成url接口路径<br>
     * 例如：/RPC/service/method
     * 
     * @param namespace
     * @param service
     * @param method
     * @return
     */
    public static String getUrl(String namespace, String service, String method) {
        return namespace + service + "/" + method;
    }

    /**
     * 判断是否请求静态资源
     * 
     * @param servletPath
     * @return
     */
    public static boolean isStaticResource(String servletPath, final Set<String> inculdes) {
        int pos = servletPath.indexOf("?");
        String tmp = servletPath;
        if (pos != -1) {
            tmp = servletPath.substring(0, pos);
        }
        pos = tmp.lastIndexOf(".");
        if (pos == -1) {
            return false;
        }
        tmp = tmp.substring(pos);
        return inculdes.contains(tmp);
    }

}
