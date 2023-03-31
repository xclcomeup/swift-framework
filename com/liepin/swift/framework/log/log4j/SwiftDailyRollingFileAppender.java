package com.liepin.swift.framework.log.log4j;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.DailyRollingFileAppender;

import com.liepin.swift.core.util.Log4jUtil;

/**
 * 自动创建日志目录
 * 
 * @author yuanxl
 * 
 */
public class SwiftDailyRollingFileAppender extends DailyRollingFileAppender {

    /**
     * 相对路径
     * <p>
     * 如：eventinfo/eventInfo.log
     */
    @Override
    public void setFile(String file) {
        String path = Log4jUtil.getLogRootDirectory() + file;
        try {
            FileUtils.forceMkdir(new File(path).getParentFile());
        } catch (IOException e) {
            throw new RuntimeException("创建日志目录失败: " + path, e);
        }
        super.setFile(path);
    }

}
