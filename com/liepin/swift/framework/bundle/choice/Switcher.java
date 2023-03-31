package com.liepin.swift.framework.bundle.choice;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 通用项目黑白名单开关
 * 
 * @author yuanxl
 *
 */
public class Switcher {

    private static final Logger logger = Logger.getLogger(Switcher.class);

    private String zkPath;
    // 开关
    private volatile boolean enable;
    // 白名单
    private volatile Pair<Boolean, Set<String>> whiteListPair;
    // 黑名单
    private volatile Pair<Boolean, Set<String>> blacklistPair;

    public Switcher(String zkPath) {
        this.zkPath = zkPath;
        loadConfig();
        createListener();
    }

    /**
     * 返回开关是否开的
     * <p>
     * 开:true<br>
     * 关:false<br>
     * 
     * @param clientId
     * @return
     */
    public boolean getEnable(String clientId) {
        if (!enable) {
            return false;
        }
        if (Objects.nonNull(whiteListPair) && whiteListPair.getFirst()) {
            return whiteListPair.getSecond().contains(clientId);
        }
        if (Objects.nonNull(blacklistPair) && blacklistPair.getFirst()) {
            return !blacklistPair.getSecond().contains(clientId);
        }
        return false;
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

        Boolean whiteListEnable = Optional.ofNullable((Boolean) map.get("whiteListEnable")).orElse(Boolean.FALSE);
        Set<String> whiteListProjects = Optional.ofNullable((List<String>) map.get("whiteListProjects"))
                .map((List<String> t) -> {
                    return new HashSet<>(t);
                }).orElse(new HashSet<>());
        Boolean blacklistEnable = Optional.ofNullable((Boolean) map.get("blacklistEnable")).orElse(Boolean.FALSE);
        Set<String> blacklistProjects = Optional.ofNullable((List<String>) map.get("blacklistProjects"))
                .map((List<String> t) -> {
                    return new HashSet<>(t);
                }).orElse(new HashSet<>());
        if (whiteListEnable == true && blacklistEnable == true) {
            this.enable = false;
            logger.warn("Switcher加载zk配置path=" + zkPath
                    + "异常, 降级为开关关闭。因为whitelistenable白名单和blacklistenable黑名单开关同时打开, 解决方案：黑白名单只能开启一个!");
            return;
        }
        Pair<Boolean, Set<String>> whiteTmp = new Pair<Boolean, Set<String>>(whiteListEnable, whiteListProjects);
        Pair<Boolean, Set<String>> blackTmp = new Pair<Boolean, Set<String>>(blacklistEnable, blacklistProjects);
        this.whiteListPair = whiteTmp;
        this.blacklistPair = blackTmp;
        this.enable = true;
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
                        logger.error("Switcher获取zk路径path=" + listeningPath() + "配置失败!", e);
                    }
                }
            }
        });
    }

}
