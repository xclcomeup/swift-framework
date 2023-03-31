package com.liepin.swift.framework.plugin.jsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.catalina.core.StandardWrapper;
import org.apache.jasper.servlet.JspServlet;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.common.conf.SystemUtil;
import com.liepin.common.conf.SystemUtil.DeployType;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.util.EmptyHttpServletWrapper;

public class JspPlugin implements IPlugin<List<String>> {

    private static final Logger logger = Logger.getLogger(JspPlugin.class);

    private final List<String> jspFiles = new ArrayList<>();

    private static final List<String> defaultClasspaths = Arrays.asList("/static", "/public", "/resources",
            "/META-INF/resources");

    // private String viewPrefix = null;

    public static StandardWrapper standardWrapper = null;

    public JspPlugin() {
        // this.viewPrefix =
        // PropUtil.getInstance().get(JspPlugin.class.getClassLoader(),
        // "application.properties",
        // "spring.mvc.view.prefix");
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        // WAR包才执行
        if (!isWar()) {
            return;
        }
        // 为了启动提速，线下环境可以异步加载
        if (SwiftConfig.enableStartupPreload()) {
            start();
        } else {
            new Thread(() -> {
                start();
            }).start();
        }
    }

    private void start() {
        logger.info("JspPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<JspURLFilter>().scanUrls(new JspURLFilter()).forEach(e -> {
            String servletPath = found(e.getPath());
            jspFiles.add(servletPath);
            log.append("Added {" + servletPath).append("} to JspUrl\n");
        });
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        jspFiles.clear();
        logger.info("JspPlugin destroy.");
    }

    @Override
    public List<String> getObject() {
        return Collections.unmodifiableList(jspFiles);
    }

    public static void setStandardWrapper(StandardWrapper wrapper) {
        JspPlugin.standardWrapper = wrapper;
    }

    public void compileJsp() {
        if (jspFiles.isEmpty()) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool((jspFiles.size() > 10) ? 10 : jspFiles.size());
        try {
            CountDownLatch doneSignal = new CountDownLatch(jspFiles.size());
            long s = System.currentTimeMillis();
            jspFiles.forEach(t -> {
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            preCompile(t);
                        } catch (Exception e) {
                        }
                        doneSignal.countDown();
                    }

                });

            });
            doneSignal.await();
            logger.info("预编译JSP文件总耗时: " + (System.currentTimeMillis() - s) + " ms");
        } catch (Exception e) {
            logger.warn("预编译JSP文件失败", e);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void preCompile(String servletPath) {
        JspServlet jsp = (JspServlet) standardWrapper.getServlet();
        try {
            long s = System.currentTimeMillis();
            jsp.service(EmptyHttpServletWrapper.request(servletPath), EmptyHttpServletWrapper.response());
            logger.info("预编译JSP文件: " + servletPath + ", 耗时: " + (System.currentTimeMillis() - s) + " ms");
        } catch (Exception e) {
            MonitorLogger.getInstance().log("预编译JSP文件失败: " + servletPath, e);
        }
    }

    private boolean isWar() {
        return DeployType.WAR == SystemUtil.getDeployType();
    }

    private String found(String path) {
        // 去掉绝对路径
        String servletPath = subString(path, ".war!");

        // 去掉公共目录
        for (String classPath : defaultClasspaths) {
            String tmp = subString(servletPath, classPath);
            if (!tmp.equals(servletPath)) {
                servletPath = tmp;
                break;
            }
        }

        // // 去掉prefix前缀
        // if (Objects.nonNull(viewPrefix)) {
        // String tmp = viewPrefix;
        // if (viewPrefix.endsWith("/")) {
        // tmp = viewPrefix.substring(0, viewPrefix.length() - 1);
        // }
        // servletPath = subString(servletPath, tmp);
        // }

        return servletPath;
    }

    private String subString(String source, String flag) {
        int pos = source.indexOf(flag);
        return (pos != -1) ? source.substring(pos + flag.length()) : source;
    }

    @Override
    public String name() {
        return "JSP预加载";
    }

}
