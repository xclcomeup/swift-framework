package com.liepin.swift.framework.monitor.heartbeat;

import java.util.HashMap;
import java.util.Objects;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.router.discovery.ServiceCurator;
import com.liepin.router.discovery.ServiceCurator.ServiceInfoListener;
import com.liepin.router.discovery.ServiceInfo;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.unusual.IZookeeperClient;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class HttpHeartbeatManager {

    private static HttpHeartbeatManager instance = new HttpHeartbeatManager();

    /**
     * 控制心跳开关
     */
    private volatile boolean heartbeat = true;

    private HttpHeartbeatManager() {
        heartbeat = provideFlow();
        createListener();
        registerHeartbeat();
    }

    public static HttpHeartbeatManager get() {
        return instance;
    }

    @SuppressWarnings("serial")
    private void registerHeartbeat() {
        try {
            // zk注册心跳
            IZookeeperClient zookeeperClient = ZookeeperFactory.useServerZookeeperWithoutException();
            String path = "/rpc/server/heartbeat/" + ProjectId.getProjectName();
            Boolean exist = zookeeperClient.exist(EnumNamespace.PUBLIC, path);
            if (!Objects.isNull(exist) && !exist) {
                zookeeperClient.setNode4Map(EnumNamespace.PUBLIC, path, new HashMap<String, Object>() {
                    {
                        put("heartbeat", true);
                    }
                });
            }
        } catch (Exception e) {
        }
    }

    public boolean http() {
        return heartbeat;
    }

    private boolean provideFlow() {
        ServiceInfo serviceInfo = ServiceCurator.get().getServiceInfo(ProjectId.getProjectName());
        return (serviceInfo != null) ? serviceInfo.isFlow(SystemUtil.getPod()) : true;
    }

    private void createListener() {
        ServiceCurator.get().createServiceInfoListener(ProjectId.getProjectName(), new ServiceInfoListener() {

            @Override
            public void update(String projectName) {
                heartbeat = provideFlow();
            }

        });
    }

}
