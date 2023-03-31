package com.liepin.swift.framework.monitor.dependency;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.util.DependencyUtil;
import com.liepin.swift.framework.util.Pair;

public class JarDependency {

    private static final Logger logger = Logger.getLogger(JarDependency.class);

    // <parent> groupId=com.liepin.parent
    private String parentArtifactId;
    private String parentVersion;

    private List<Pair<String, String>> jars = new ArrayList<Pair<String, String>>();

    public JarDependency() {
    }

    public void collect() throws Exception {
        Exception exception = null;
        // 分析pom.xml抽取parent
        try {
            pomAnalysis();
        } catch (Exception e) {
            exception = e;
            MonitorLogger.getInstance().log("读取pom里\"com.liepin.parent\"版本失败: " + e.getMessage());
        }

        // 扫描lib目录
        dirScan();

        if (Objects.nonNull(exception)) {
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private void pomAnalysis() throws Exception {
        // webapps/mobile-xyclub-web/META-INF/maven/com.liepin/mobile-xyclub-web/pom.xml
        // webapps/*/META-INF/maven/com.liepin/*/pom.xml

        // 方式一：tomcat
        // File pomFile = DependencyUtil.getPomFile();
        // if (pomFile == null) {
        // throw new RuntimeException("webapps/*/META-INF/maven/*/*/pom.xml
        // 没找到");
        // }

        // 方式二：jar
        InputStream jarPomInputStream = DependencyUtil.getPomXmlStream();
        if (jarPomInputStream == null) {
            throw new RuntimeException("部署包里没有找到pom.xml文件，查找路径jar|war:file:***.jar|war!/META-INF/maven/**/pom.xml");
        }

        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(jarPomInputStream);
            Element root = doc.getRootElement();
            Element parent = null;
            for (Iterator<Element> i = root.elementIterator("parent"); i.hasNext();) {
                Element tmp = (Element) i.next();
                if ("com.liepin.parent".equals(tmp.elementText("groupId"))) {
                    parent = tmp;
                    break;
                }
            }
            if (Objects.nonNull(parent)) {
                this.parentArtifactId = parent.elementText("artifactId");
                this.parentVersion = parent.elementText("version");
            } else {
                // 降级
                parent = Optional.ofNullable(root.element("dependencies"))
                        .map(t -> (Iterator<Element>) t.elementIterator("dependency")).map((Iterator<Element> k) -> {
                            while (k.hasNext()) {
                                Element tmp = (Element) k.next();
                                if ("com.liepin.parent".equals(tmp.elementText("groupId"))) {
                                    return tmp;
                                }
                            }
                            return null;
                        }).orElse(null);
                if (Objects.nonNull(parent)) {
                    this.parentArtifactId = parent.elementText("artifactId");
                    this.parentVersion = parent.elementText("version");
                } else {
                    throw new RuntimeException(
                            "pom.xml文件里没有发现以下引用: <parent><groupId>com.liepin.parent</groudId></parent>或者<dependency><groupId>com.liepin.parent</groupId></dependency>");
                }
            }
        } catch (DocumentException e) {
            MonitorLogger.getInstance()
                    .log("读取pom里\"com.liepin.parent\"版本失败: 部署包的pom.xml文件读取失败，不是xml格式. " + e.getMessage());
            throw new RuntimeException("读取pom里\"com.liepin.parent\"版本失败: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("读取pom里\"com.liepin.parent\"版本失败: " + e.getMessage());
        }
    }

    private void dirScan() {
        Map<String, InputStream> jarLibStreams = DependencyUtil.getJarLibStreams();
        jarLibStreams.forEach((String name, InputStream is) -> {
            if (Objects.isNull(is)) {
                logger.warn("依赖jar=" + name + "的scope是provided，扫描被忽略.");
                return;
            }
            jars.add(new Pair<String, String>(name, md5(is)));
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        });
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public List<Pair<String, String>> getJars() {
        return jars;
    }

    @Override
    public String toString() {
        return "JarDependency [parentArtifactId=" + parentArtifactId + ", parentVersion=" + parentVersion + ", jars="
                + jars + "]";
    }

    private String md5(InputStream is) {
        try {
            return DigestUtils.md5Hex(IOUtils.toByteArray(is));
        } catch (Exception e) {
            logger.warn("JAR包依赖查找: 对lib/jar的二进制流进行md5计算失败", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return null;
    }

}
