package com.liepin.swift.framework.monitor.proc.fd;

import java.io.File;
import java.text.MessageFormat;

/**
 * 获取进程占用句柄数
 * 
 * @author yuanxl
 * @date 2017-8-13 下午11:48:20
 */
public class ProcForFd {

    private static final String FD_PATH_LINUX = "/proc/{0}/fd";

    /**
     * 返回当前进程占用句柄数
     * <p>
     * 如果获取失败返回：－1
     * 
     * @param pid
     * @return
     */
    public static int print(String pid) {
        String path = MessageFormat.format(FD_PATH_LINUX, new Object[] { pid });
        File dir = new File(path);
        String[] list = dir.list();
        return (list != null) ? list.length : -1;
    }

}
