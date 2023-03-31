package com.liepin.swift.framework.bundle.choice;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;

import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 通用白名单开关
 * 
 * @author yuanxl
 *
 */
public class EasySwitcher {

    private static final Logger logger = Logger.getLogger(EasySwitcher.class);

    private String zkPath;
    private String enableKeyName;
    private String listKeyName;

    // 开关
    private volatile boolean enable;
    // 列表
    private volatile Set<String> list;

    public EasySwitcher(String zkPath, String enableKeyName, String listKeyName) {
        this.zkPath = zkPath;
        this.enableKeyName = enableKeyName;
        this.listKeyName = listKeyName;
        loadConfig();
        createListener();
    }

    /**
     * 返回开关是否开的
     * <p>
     * 开:true<br>
     * 关:false<br>
     * 
     * @param value
     * @return
     */
    public boolean getEnable(String value) {
        return enable ? ((Objects.nonNull(list)) ? list.contains(value) : false) : false;
    }

    private Map<String, Object> readFromZk() {
        return ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC, zkPath);
    }

    /**
     * 根据配置触发事件
     * 
     * @param map
     */
    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Map<String, Object> map = readFromZk();
        if (map == null) {
            return;
        }

        this.enable = Optional.ofNullable((Boolean) map.get(enableKeyName)).orElse(Boolean.FALSE);
        this.list = Optional.ofNullable((List<String>) map.get(listKeyName)).map((List<String> t) -> {
            return new HashSet<>(t);
        }).orElse(new HashSet<>());
    }

    private void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + zkPath;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type || EnumChangedEvent.ADDED == type) {
                    try {
                        // 错开时间
                        StaggerTime.waited();
                        loadConfig();
                    } catch (Exception e) {
                        logger.error("EasySwitcher获取zk路径path=" + listeningPath() + "配置失败!", e);
                    }
                }
            }

        });
    }

}
