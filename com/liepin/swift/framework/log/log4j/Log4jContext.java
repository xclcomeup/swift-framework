package com.liepin.swift.framework.log.log4j;

import java.util.Iterator;

import org.apache.log4j.Level;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.conf.SystemUtil;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.util.Log4jUtil;

public class Log4jContext {

    public static void initialize() {
        // 配置日志打印级别
        int levelInt = Integer.MAX_VALUE;
        Iterator<String> logConfig = PropUtil.getInstance().getKeys(Const.NAMESPACE_LOG_LEVEL);
        while (logConfig.hasNext()) {
            String key = logConfig.next();
            String value = PropUtil.getInstance().get(key);
            if (SystemUtil.isOnline()) {
                // 线上环境限制日志级别WARN
                value = Level.WARN.toString();
            }
            String name = key.substring(Const.NAMESPACE_LOG_LEVEL.length() + 1);
            levelInt = Math.min(levelInt, Log4jUtil.setLevel(name, value).toInt());
        }
        if (levelInt != Integer.MAX_VALUE) {
            Log4jUtil.setConsoleLevel(levelInt);
        }
    }

}
