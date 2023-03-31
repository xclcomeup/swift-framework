package com.liepin.swift.framework.limit.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.limit.config.controll.LimitControll;
import com.liepin.swift.framework.limit.rules.Bridge;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.ZookeeperException;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeChildListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class LimitControllConfiger {

    private static final Logger logger = Logger.getLogger(LimitControllConfiger.class);

    private volatile boolean enable = false;
    private volatile List<LimitControll> rules = new ArrayList<LimitControll>();

    private static final String LIMIT_PATH = "rpc/server/limit";

    private static LimitControllConfiger instance = new LimitControllConfiger();

    private LimitControllConfiger() {
        if (load()) {
            Bridge.refresh(getAllLimitControll());
        }
        createListener();
    }

    public static LimitControllConfiger get() {
        return instance;
    }

    /**
     * 
     * @return true:刷新|false:忽略
     */
    @SuppressWarnings("unchecked")
    private boolean load() {
        try {
            Map<String, Object> data = ZookeeperFactory.useServerZookeeper().getMap(EnumNamespace.PUBLIC,
                    LIMIT_PATH + "/" + ProjectId.getProjectName());
            if (data == null || data.isEmpty()) {
                this.enable = false;
                this.rules = new ArrayList<LimitControll>();
            } else {
                Boolean enableObj = (Boolean) data.get("enable");
                this.enable = (enableObj != null) ? enableObj.booleanValue() : true;// 默认开启

                List<Map<String, Object>> definitions = (List<Map<String, Object>>) data.get("definitions");
                List<LimitControll> ruleConfigs = definitions.stream().map((Map<String, Object> t) -> {
                    Map<String, Object> limitRuleMap = (Map<String, Object>) t.get("limitRule");
                    String ruleName = limitRuleMap.keySet().iterator().next();
                    EnumLimitRuleConfig enumLimitRuleConfig = EnumLimitRuleConfig.valueOf(ruleName);
                    return enumLimitRuleConfig.read(t);
                }).collect(Collectors.toList());

                if (ruleConfigs.isEmpty()) {
                    this.enable = false;
                }

                // 补充删除的
                this.rules.forEach(oc -> {
                    if (!ruleConfigs.contains(oc)) {
                        oc.setEnable(false);
                        ruleConfigs.add(oc);
                    }
                });

                this.rules = ruleConfigs;
            }
            MonitorLogger.getInstance().log("ServiceLimit reload limit controll config: " + this.rules);
            return true;
        } catch (ZookeeperException e) {
            logger.error("读取zk配置失败: 路径=" + LIMIT_PATH + "/" + ProjectId.getProjectName(), e);
            return false;
        }
    }

    private void createListener() {
        ZookeeperFactory.useServerZookeeperWithCacheWithoutException().addListener(new NewNodeChildListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/" + LIMIT_PATH;
            }

            @Override
            public void childChanged(IZookeeperClient zookeeperClient, String childName, EnumChangedEvent type) {
                if (!ProjectId.getProjectName().equals(childName)) {
                    return;
                }
                if (EnumChangedEvent.CHILD_ADDED == type || EnumChangedEvent.CHILD_UPDATED == type
                        || EnumChangedEvent.CHILD_REMOVED == type) {
                    if (load()) {
                        Bridge.refresh(getAllLimitControll());
                    }
                }
                if (EnumChangedEvent.CHILD_REMOVED == type) {
                    // 强一致
                    enable = false;
                }
            }

        });
    }

    public boolean isEnable() {
        return enable;
    }

    public List<LimitControll> getAllLimitControll() {
        return rules;
    }

}
