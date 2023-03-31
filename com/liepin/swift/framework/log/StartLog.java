package com.liepin.swift.framework.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.liepin.swift.core.util.Log4jUtil;

public class StartLog {

    private static final String STDOUT_FILENAME = "stdout.log";
    private static final String STDERR_FILENAME = "stderr.log";
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static String logsDir;

    public static void reset() {
        //logsDir = SystemUtil.getDeployDirectory("logs");
        logsDir = Log4jUtil.getLogRootDirectory();
        File logsDirFile = new File(logsDir);
        if (!logsDirFile.exists()) {
            logsDirFile.mkdirs();
        }
        // 重置文件
        clear(STDOUT_FILENAME);
        clear(STDERR_FILENAME);
    }

    private static void clear(String filename) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new FileWriter(logsDir + FILE_SEPARATOR + filename, false))) {
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("启动日志文件创建失败【" + filename + "】", e);
        }
    }

    /**
     * 启动成功输出
     * 
     * @param logger
     * @param message
     */
    public static void out(final Logger logger, String message) {
        try (BufferedWriter stdoutBufferedWriter = new BufferedWriter(
                new FileWriter(logsDir + FILE_SEPARATOR + STDOUT_FILENAME, false))) {
            stdoutBufferedWriter.write(message);
            stdoutBufferedWriter.newLine();
        } catch (IOException e) {
        }
        logger.warn(message);
    }

    /**
     * 启动异常输出
     * 
     * @param logger
     * @param message
     * @param throwable
     */
    public static void err(final Logger logger, String message, Throwable throwable) {
        try (BufferedWriter stderrBufferedWriter = new BufferedWriter(
                new FileWriter(logsDir + FILE_SEPARATOR + STDERR_FILENAME, false))) {
            stderrBufferedWriter.write(message);
            stderrBufferedWriter.newLine();
        } catch (IOException e) {
        }
        logger.error(message, throwable);
    }
    
    /**
     * 启动异常输出
     * 
     * @param logger
     * @param message
     */
    public static void err(String message) {
        try (BufferedWriter stderrBufferedWriter = new BufferedWriter(
                new FileWriter(logsDir + FILE_SEPARATOR + STDERR_FILENAME, false))) {
            stderrBufferedWriter.write(message);
            stderrBufferedWriter.newLine();
        } catch (IOException e) {
        }
        System.out.println(message);
    }

}
