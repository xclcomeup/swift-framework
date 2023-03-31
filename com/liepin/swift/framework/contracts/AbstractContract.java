package com.liepin.swift.framework.contracts;

import java.util.List;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class AbstractContract {


    /**
     * 启动检查jar功能是否开启
     * <p>
     * 开启：true<br>
     * 关闭：false<br>
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean enable() {
        try {
            Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/monitor/jar");
            if (data != null) {
                Boolean enable = (Boolean) data.get("startCheck");
                boolean check = (enable != null) ? enable.booleanValue() : false;
                if (check) {
                    List<String> projects = (List<String>) data.get("disableProjects");
                    if (projects != null) {
                        return !projects.contains(ProjectId.getProjectName());
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

}
