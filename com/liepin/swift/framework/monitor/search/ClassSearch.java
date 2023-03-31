package com.liepin.swift.framework.monitor.search;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;

/**
 * 获取类字节码
 * 
 * @author yuanxl
 * @date 2017-9-3 下午11:12:44
 */
public class ClassSearch {

    private static final String COMMAND_LINUX = "javap -v {0}";

    /**
     * 获取类字节码
     * 
     * @param classFile class文件绝对路径
     * @return
     */
    public static String javap(String classFile) {
        String classFile1 = classFile;
        if (classFile1.indexOf("$") != -1) {
            classFile1 = classFile1.replaceAll("\\$", "\\\\\\$");
        }

        String command = MessageFormat.format(COMMAND_LINUX, new Object[] { classFile1 });
        Process process = null;
        LineNumberReader input = null;
        StringBuilder text = new StringBuilder();
        try {
            String[] commands = new String[] { "sh", "-c", command };
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            process = pb.start();
            input = new LineNumberReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
        } catch (Throwable e) {
            // ignore
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
            }
            if (process != null) {
                process.destroy();
            }
        }
        return text.toString();
    }

}
