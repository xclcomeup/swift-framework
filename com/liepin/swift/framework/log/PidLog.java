package com.liepin.swift.framework.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;

public class PidLog {

    public static void logPid() {
        if (isWindows()) {
            return;
        }

        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        write(pid);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static final String FILENAME = ProjectId.getClientId() + ".pid";

    private static void write(String pid) {
        File dir = new File(SystemUtil.getDeployDirectory());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter stderrBufferedWriter = new BufferedWriter(
                new FileWriter(SystemUtil.getDeployDirectory() + "/" + FILENAME, false))) {
            stderrBufferedWriter.write(pid);
            stderrBufferedWriter.newLine();
        } catch (IOException e) {
        }
    }

}
