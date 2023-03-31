package com.liepin.swift.framework.contracts.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.contracts.IContract;

/**
 * jvm启动变量冲突检查
 * 
 * @author yuanxl
 *
 */
public class SystemEnvContract implements IContract {

    @Override
    public void review() throws SysException {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            // System.getenv()
            // System.getProperties()

            Map<String, String> argEnvs = new LinkedHashMap<String, String>();
            Map<String, List<String>> argEnvsConflict = new LinkedHashMap<String, List<String>>();

            for (String arg : args) {
                arg = arg.trim();
                if (arg.startsWith("-D")) {
                    arg = arg.substring(2);
                    int pos = arg.indexOf("=");
                    String key = arg.substring(0, pos);
                    String value = arg.substring(pos + 1, arg.length());
                    if (argEnvs.containsKey(key)) {
                        List<String> list = argEnvsConflict.get(key);
                        if (list == null) {
                            argEnvsConflict.put(key, list = new ArrayList<String>());
                            list.add(key + "=" + argEnvs.get(key));
                        }
                        list.add(arg);
                    } else {
                        argEnvs.put(key, value);
                    }
                } else if (arg.startsWith("-X")) {
                    // 暂不处理X系列的JVM配置
                }
            }

            if (argEnvsConflict.size() > 0) {
                StringBuilder log = new StringBuilder("java环境变量冲突，如下：");
                for (Map.Entry<String, List<String>> entry : argEnvsConflict.entrySet()) {
                    log.append("\n");
                    String notice = "\"-D" + entry.getKey() + "\" 有冲突，请检查: " + entry.getValue();
                    log.append(notice);
                }
                throw new SysException("-1", log.toString());
            }
        } catch (Throwable e) {
            if (e instanceof SysException) {
                throw (SysException) e;
            }
        }
    }

}
