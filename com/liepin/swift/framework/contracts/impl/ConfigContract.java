package com.liepin.swift.framework.contracts.impl;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.ProjectIdMap;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.contracts.IContract;

/**
 * config.properties配置文件检查
 * 
 * @author yuanxl
 *
 */
public class ConfigContract implements IContract {

    @Override
    public void review() throws SysException {
        if (!SwiftConfig.enableZookeeper()) {
            return;
        }
        String clientId = ProjectId.getClientId();
        String projectName = ProjectId.getProjectName();

        String checkClientId = ProjectIdMap.clientId(projectName);
        String checkProjectName = ProjectIdMap.projectName(clientId);

        if (!clientId.equals(checkClientId) || !projectName.equals(checkProjectName)) {
            StringBuilder log = new StringBuilder("config.properties，有冲突，如下：");
            log.append("\n");
            log.append("project.client_id=").append(clientId).append("\n");
            log.append("project.name=").append(projectName).append("\n");
            log.append("正确配置如下，请检查: \n");
            log.append("\"" + checkClientId + "\":\"" + projectName + "\"\n");
            log.append("\"" + clientId + "\":\"" + checkProjectName + "\"\n");
            throw new SysException("-1", log.toString());
        }
    }

}
