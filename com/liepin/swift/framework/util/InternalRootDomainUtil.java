package com.liepin.swift.framework.util;

import java.util.Map;

import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 内部域名
 * 
 * @author yuanxl
 *
 */
public class InternalRootDomainUtil {

    private static volatile String rootDomainDefault = "tongdao.cn";
    private static final String PATH = "common/internal";

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
     * 获取内部默认根域名
     * 
     * @return
     */
    public static final String getDefaultRootDomain() {
        return rootDomainDefault;
    }

    /**
     * 根据前缀拼接
     * <p>
     * prefix + "." + $rootDomain
     * 
     * @param prefix
     * @return
     */
    public static final String joinDomain(String prefix) {
        return prefix + "." + rootDomainDefault;
    }

}
