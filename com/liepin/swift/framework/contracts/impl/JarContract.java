package com.liepin.swift.framework.contracts.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.contracts.AbstractContract;
import com.liepin.swift.framework.contracts.IContract;
import com.liepin.swift.framework.util.DependencyUtil;

public class JarContract extends AbstractContract implements IContract {

    private static final Logger logger = Logger.getLogger(JarContract.class);

    @Override
    public void review() throws SysException {
        if (!enable()) {
            return;
        }

        try {
            // 分析
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> jarNames = DependencyUtil.getJarNames();
            for (String jarName : jarNames) {
                jarName = jarName.substring(0, jarName.indexOf(".jar"));
                char[] array = jarName.toCharArray();
                StringBuilder artifactIdBuilder = new StringBuilder();
                StringBuilder versionBuilder = new StringBuilder();
                boolean startVersion = false;
                for (int i = 0; i < array.length; i++) {
                    if (startVersion) {
                        versionBuilder.append(array[i]);
                    } else {
                        if (i + 1 != array.length && '-' == array[i] && Character.isDigit(array[i + 1])) {
                            startVersion = true;
                        } else {
                            artifactIdBuilder.append(array[i]);
                        }
                    }
                }
                String artifactId = artifactIdBuilder.toString();
                String version = versionBuilder.toString();

                List<String> list = map.get(artifactId);
                if (list == null) {
                    map.put(artifactId, list = new ArrayList<String>());
                }
                list.add(version);
            }
            // 预警
            StringBuilder log = new StringBuilder(
                    "【工程dependencies包检查】，jar包同名不同版本冲突，如下，【注意：指解析打包后生成的war或jar包里的lib目录下的jar文件，不是检查工程pom.xml文件】");
            int cnt = 0;
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getValue().size() > 1) {
                    log.append("\n");
                    String notice = "\"artifactId=" + entry.getKey() + "\" 有冲突，请检查: " + entry.getValue();
                    log.append(notice);
                    cnt++;
                }
            }
            if (cnt > 0) {
                log.append("\n").append("以上冲突可能出现同一个class出现在不同jar包，并且部分代码不一致，避免类运行时异常，请及时排查!");
                logger.error(log.toString());
            }
        } catch (Throwable e) {
            MonitorLogger.getInstance().log("工程dependencies包依赖检查失败：" + e.getMessage(), e);
        }
    }

}
