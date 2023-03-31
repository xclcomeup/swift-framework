package com.liepin.swift.framework.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class RootDomainUtil {

    private static String rootDomainDefault = "liepin.com";
    private static final String PATH = "common/web";

    static {
        load();
        createListener();
    }

    private static void load() {
        String value = readRootDomainDefault();
        if (value != null) {
            rootDomainDefault = value;
        }
    }

    private static String readRootDomainDefault() {
        Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                PATH);
        if (data != null && data.size() > 0) {
            return (String) data.get("rootDomainDefault");
        }
        return null;
    }

    private static void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/" + PATH;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type) {
                    load();
                }
            }
        });
    }

    /**
     * 获取默认根域名
     * <p>
     * 业务可以调用
     * 
     * @return
     */
    public static final String getDefaultRootDomain() {
        return rootDomainDefault;
    }

    /**
     * 获取当前请求的根域名，没有返回默认根域名，并且如果不是liepin的域名统一返回默认根域名
     * <p>
     * for 业务调用
     * 
     * @return
     */
    public static final String getCurrentRootDomain() {
        String rootDomain = ThreadLocalUtil.getInstance().getRootDomain();
        return ("".equals(rootDomain) || !rootDomain.startsWith("liepin.")) ? rootDomainDefault : rootDomain;
    }

    /**
     * 判断当前请求的根域名是否liepin.com
     * <p>
     * for 业务调用
     * 
     * @return
     */
    public static final boolean isLiepinCom() {
        String rootDomain = ThreadLocalUtil.getInstance().getRootDomain();
        return "liepin.com".equals(rootDomain);
    }

    /**
     * 判断当前请求的根域名是否liepin.cn
     * <p>
     * for 业务调用
     * 
     * @return
     */
    public static final boolean isLiepinCn() {
        String rootDomain = ThreadLocalUtil.getInstance().getRootDomain();
        return "liepin.cn".equals(rootDomain);
    }

    /**
     * 获取当前请求的根域名
     * 
     * @param request
     * @return
     */
    public static final String getCrrentRootDomain(final HttpServletRequest request) {
        return com.liepin.swift.framework.mvc.util.RequestUtil.getRootDomain(request);
    }

}
