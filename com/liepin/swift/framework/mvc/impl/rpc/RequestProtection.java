package com.liepin.swift.framework.mvc.impl.rpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.datastructure.Pair;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

/**
 * 控制服务接口访问，通过白名单或黑名单方式<br>
 * 注意：同一个接口，配置白名单和黑名单互斥<br>
 * 
 * @author yuanxl
 *
 */
public class RequestProtection {

    // 白名单 key: requestPath， value-> A: initClientId B: lastClientId
    private volatile Map<String, Pair<Set<String>, Set<String>>> whiteListMap = new HashMap<>();
    // 黑名单 key: requestPath， value-> A: initClientId B: lastClientId
    private volatile Map<String, Pair<Set<String>, Set<String>>> blackListMap = new HashMap<>();

    private final String zkPath;

    public RequestProtection() {
        this.zkPath = "rpc/config/protection/" + ProjectId.getClientId();
        load();
        createListener();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                zkPath);
        if (data != null && !data.isEmpty()) {
            this.whiteListMap = build((Map<String, Object>) data.get("whiteList"));
            this.blackListMap = build((Map<String, Object>) data.get("blackList"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Pair<Set<String>, Set<String>>> build(final Map<String, Object> map) {
        Map<String, Pair<Set<String>, Set<String>>> listMap = new HashMap<>();
        if (map != null && !map.isEmpty()) {
            map.forEach((String requestPath, Object obj) -> {
                Map<String, Object> valueMap = (Map<String, Object>) obj;
                Set<String> initClientIdSet = null;
                Set<String> lastClientIdSet = null;
                List<String> initClientIdList = (List<String>) valueMap.get("initClientId");
                if (initClientIdList != null && !initClientIdList.isEmpty()) {
                    initClientIdSet = new HashSet<>(initClientIdList);
                }
                List<String> lastClientIdList = (List<String>) valueMap.get("lastClientId");
                if (lastClientIdList != null && !lastClientIdList.isEmpty()) {
                    lastClientIdSet = new HashSet<>(lastClientIdList);
                }
                listMap.put(requestPath, new Pair<Set<String>, Set<String>>(initClientIdSet, lastClientIdSet));
            });
        }
        return listMap;
    }

    private void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/" + zkPath;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                if (EnumChangedEvent.ADDED == type || EnumChangedEvent.UPDATED == type) {
                    load();
                }
                if (EnumChangedEvent.REMOVED == type) {
                    whiteListMap = new HashMap<>();
                    blackListMap = new HashMap<>();
                }
            }
        });
    }

    /**
     * true:允许访问|false:禁止访问
     * 
     * @param requestPath
     * @returnO
     */
    public boolean check(String requestPath) {
        // 先检查白名单
        Pair<Set<String>, Set<String>> white = whiteListMap.get(requestPath);
        if (white != null) {// 如果不是null代表有配置
            // 先检查initClientId
            Set<String> first = white.getFirst();
            if (first != null) {// 如果不是null代表有配置
                if (first.contains(ThreadLocalUtil.getInstance().getInitClientId())) {
                    return true;
                }
            }
            // 再检查lastClientId
            Set<String> second = white.getSecond();
            if (second != null) {// 如果不是null代表有配置
                if (second.contains(ThreadLocalUtil.getInstance().getLastClientId())) {
                    return true;
                }
            }
            return false;
        }

        // 再检查黑名单
        Pair<Set<String>, Set<String>> black = blackListMap.get(requestPath);
        if (black != null) {// 如果不是null代表有配置
            // 先检查initClientId
            Set<String> first = black.getFirst();
            if (first != null) {// 如果不是null代表有配置
                if (first.contains(ThreadLocalUtil.getInstance().getInitClientId())) {
                    return false;
                }
            }
            // 再检查lastClientId
            Set<String> second = black.getSecond();
            if (second != null) {// 如果不是null代表有配置
                if (second.contains(ThreadLocalUtil.getInstance().getLastClientId())) {
                    return false;
                }
            }
            return true;
        }

        // 如果白名单和黑名单都没有配置允许访问
        return true;
    }

    /**
     * 禁止访问, 抛出new SysException(SystemEnum.Auth);
     * 
     * @param requestPath
     */
    public void arbitrate(String requestPath) {
        if (!check(requestPath)) {
            throw new SysException(SystemEnum.Auth);
        }
    }

}
