package com.liepin.swift.framework.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;

public class JavadocCollecter {

    private static final Logger logger = Logger.getLogger(JavadocCollecter.class);

    private static final String[] INCLUDES = { "**/com/liepin/**/controller/**/*Controller.java" };

    private File srcDir;

    private Pattern leftPattern = Pattern.compile("/\\*");
    private Pattern rightPattern = Pattern.compile("\\*/");

    public JavadocCollecter(File srcDir) {
        this.srcDir = srcDir;
    }

    private List<File> directoryScan() throws IOException {
        List<File> files = new ArrayList<File>();
        PlexusIoFileResourceCollection collection = new PlexusIoFileResourceCollection();
        collection.setIncludes(INCLUDES);
        collection.setExcludes(new String[] {});
        collection.setBaseDir(srcDir);
        collection.setFileSelectors(null);
        collection.setIncludingEmptyDirectories(true);
        collection.setPrefix("");
        collection.setCaseSensitive(true);
        collection.setUsingDefaultExcludes(true);
        Iterator<?> resources = collection.getResources();
        while (resources.hasNext()) {
            PlexusIoFileResource resource = (PlexusIoFileResource) resources.next();
            if (resource.isDirectory()) {
                continue;
            }
            if (!resource.getName().endsWith(".java")) {
                continue;
            }
            files.add(resource.getFile());
            // 控制台输出
            System.out.println("sources directory scan add: " + resource.getFile());
        }
        return files;
    }

    public Map<String, Map<String, List<String>>> collector() throws Exception {
        Map<String, Map<String, List<String>>> data = new LinkedHashMap<String, Map<String, List<String>>>();
        List<File> qualifiedFiles = directoryScan();
        for (File srcFile : qualifiedFiles) {
            Map<String, List<String>> javadocs = collectorOne(srcFile);
            data.put(getClassName(srcFile.getName()), javadocs);
        }
        return data;
    }

    private String getClassName(String fileName) {
        return fileName.substring(0, fileName.indexOf(".java"));
    }

    /**
     * 提取java文件方法注释
     * 
     * @param srcFile
     * @return
     */
    public Map<String, List<String>> collectorOne(File srcFile) {
        Map<String, List<String>> data = new LinkedHashMap<String, List<String>>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(srcFile));
            StringBuilder sb = new StringBuilder();
            List<String> list = new ArrayList<String>();

            String temp = "";
            /**
             * 读取文件内容，并将读取的每一行后都加上\n，<br>
             * 目的：<br>
             * 1：解析时恢复源格式<br>
             * 2: 为后面在解析双反斜杠（//）注释时做注释中止符<br>
             */
            int i = 1;
            while ((temp = br.readLine()) != null) {
                sb.append(temp);
                sb.append("\t" + i++ + "\n");// 标识行号
                list.add(temp);
            }
            String srcText = sb.toString();

            /**
             * 做/* 注释的正则匹配
             * <p>
             * 通过渐进法做注释的正则匹配，因为/*注释总是成对出现 当匹配到一个/*时总会在接下来的内容中会首先匹配到"*\\/",
             * 因此在获取对应的"*\\/"注释时只需要从当前匹配的/*开始即可， 下一次匹配时只需要从上一次匹配的结尾开始即可
             * （这样对于大文本可以节省匹配效率）—— 这就是渐进匹配法
             * 
             * */
            Matcher leftmatcher = leftPattern.matcher(srcText);
            Matcher rightmatcher = rightPattern.matcher(srcText);

            int begin = 0; // 变量用来做渐进匹配的游标 {@value} 初始值为文件开头
            while (leftmatcher.find(begin)) {
                // 判断过滤类似写法：@RequestMapping(value = {"/*",""}) 
                if (isSpecial(srcText, leftmatcher.start())) {
                    begin = leftmatcher.start()+1;
                    continue;
                }
                rightmatcher.find(leftmatcher.start());
                String tmp = srcText.substring(leftmatcher.start(), rightmatcher.end()) + "\t\n";
                String[] array = tmp.split("\n");

                try {
                    // 提取注释和起始、结尾行号
                    @SuppressWarnings("unused")
                    int start = 0;
                    int end = 0;
                    List<String> doc = new ArrayList<String>();
                    if (array.length == 1) {
                        String line = array[0].substring(0, array[0].lastIndexOf("\t"));
                        doc.add("    " + line);
                        for (int j = 0; j < list.size(); j++) {
                            if (list.get(j).contains(line)) {
                                end = j;
                                break;
                            }
                        }
                    } else {
                        for (int j = 0; j < array.length; j++) {
                            String s = array[j];
                            int pos = s.lastIndexOf("\t");
                            String line = s.substring(0, pos);
                            String number = s.substring(pos + 1);
                            if (j == 0) {
                                start = Integer.parseInt(number);
                            }
                            if (j == array.length - 2) {
                                end = Integer.parseInt(number) + 1;
                            }
                            if (j == 0) {
                                line = "    " + line;
                            }
                            doc.add(line);
                        }
                    }
                    // 提取注释紧跟的方法名，注意：注释和方法定义行之间允许相隔5行，可调整
                    String method = null;
                    for (int x = end; x < end + 5; x++) {
                        String s = list.get(x);
                        if (s.indexOf("@RequestMapping") != -1) {
                            for (int y = x; y < x + 5; y++) {
                                s = list.get(y);
                                if (s.indexOf("public") != -1 && s.indexOf("class") == -1) {
                                    int pos = s.indexOf("(");
                                    if (pos != -1) {
                                        s = s.substring(0, pos);
                                        pos = s.lastIndexOf(" ");
                                        method = s.substring(pos + 1);
                                    }
                                }
                            }
                        }
                    }
                    if (method != null) {
                        data.put(method, doc);
                    }
                } catch (Exception e) {
                    logger.warn("javadoc parse text=" + tmp + ", fail", e);
                }
                begin = rightmatcher.end();
            }
        } catch (Throwable e) {
            logger.error(srcFile.getPath() + " 文件读取失败", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }

        return data;
    }
    
    private boolean isSpecial(String text, int pos) {
        char[] array = text.toCharArray();
        StringBuilder special = new StringBuilder();
        for(int i = pos;i>0;i--) {
            if ('\n'==array[i]) {
                break;
            }
            special.append(array[i]);
        }
        special.reverse();
        String string = special.toString();
        return string.contains("@RequestMapping");
    }

}
