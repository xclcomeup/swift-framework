package com.liepin.swift.framework.contracts.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.contracts.AbstractContract;
import com.liepin.swift.framework.contracts.IContract;
import com.liepin.swift.framework.util.DependencyUtil;

public class NonParentReferenceContract extends AbstractContract implements IContract {

    private static final Logger logger = Logger.getLogger(NonParentReferenceContract.class);

    private static List<String> artifactIdsInParent = Arrays.asList("ins-swift-core", "ins-swift-framework",
            "ins-swift-cache", "ins-swift-dao", "ins-swift-router", "ins-swift-distributed", "ins-swift-collaboration",
            "ins-common-util", "cat-client-config", "ins-idp-client", "ins-zk-client", "ins-schedule-client",
            "ins-kafka-client", "ins-cuckoo-client", "ins-apigateway-spi");

    @Override
    public void review() throws SysException {
        if (!enable()) {
            return;
        }

        InputStream jarPomInputStream = DependencyUtil.getPomXmlStream();
        if (jarPomInputStream == null) {
            // throw new RuntimeException();
            logger.error("部署包里没有找到pom.xml文件，查找路径jar|war:file:***.jar|war!/META-INF/maven/**/pom.xml");
            return;
        }

        StringBuilder dependencyLog = new StringBuilder();
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(jarPomInputStream);
            Element dependencies = doc.getRootElement().element("dependencies");
            for (@SuppressWarnings("unchecked")
            Iterator<Element> i = dependencies.elementIterator("dependency"); i.hasNext();) {
                Element dependency = (Element) i.next();
                if (artifactIdsInParent.contains(dependency.elementText("artifactId"))) {
                    dependencyLog.append("<dependency>\n");
                    Iterator<?> dependencyIterator = dependency.elementIterator();
                    while (dependencyIterator.hasNext()) {
                        Element element = (Element) dependencyIterator.next();
                        dependencyLog.append("  <" + element.getName() + ">" + element.getStringValue() + "</"
                                + element.getName() + ">\n");
                    }
                    dependencyLog.append("</dependency>\n");
                }
            }
        } catch (Exception e) {
            MonitorLogger.getInstance().log("部署包的pom.xml文件读取失败，不是xml格式", e);
        }

        if (dependencyLog.length() > 0) {
            dependencyLog.insert(0, "项目单独引了parent里的包，请检查pom.xml文件，如下：\n");
            throw new SysException("-1", dependencyLog.toString());
        }
    }

}
