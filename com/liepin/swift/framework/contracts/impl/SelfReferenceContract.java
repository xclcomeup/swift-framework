package com.liepin.swift.framework.contracts.impl;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.datastructure.Pair;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ConfUtil;
import com.liepin.swift.framework.contracts.IContract;
import com.liepin.swift.framework.util.DependencyUtil;

public class SelfReferenceContract implements IContract {

    private static final Logger logger = Logger.getLogger(SelfReferenceContract.class);

    @Override
    public void review() throws SysException {
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
            Pair<Boolean, String> pair = DependencyUtil.hasSwiftPlugin4Rpc(doc);
            if (pair.getFirst()) {
                String clientName = pair.getSecond();
                if (clientName == null) {
                    clientName = ConfUtil.projectName2ClientName(ProjectId.getProjectName());
                }
                Element dependencies = doc.getRootElement().element("dependencies");
                for (@SuppressWarnings("unchecked")
                Iterator<Element> i = dependencies.elementIterator("dependency"); i.hasNext();) {
                    Element dependency = (Element) i.next();
                    if (clientName.equals(dependency.elementText("artifactId"))) {
                        dependencyLog.append("<dependency>\n");
                        Iterator<?> dependencyIterator = dependency.elementIterator();
                        while (dependencyIterator.hasNext()) {
                            Element element = (Element) dependencyIterator.next();
                            dependencyLog.append("  <" + element.getName() + ">" + element.getStringValue() + "</"
                                    + element.getName() + ">\n");
                        }
                        dependencyLog.append("</dependency>\n");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            MonitorLogger.getInstance().log("部署包的pom.xml文件读取失败，不是xml格式", e);
        }

        if (dependencyLog.length() > 0) {
            dependencyLog.insert(0, "本项目引本项目客户端，不合理，应该走内部调用，请检查pom.xml文件，如下：\n");
            // throw new SysException("-1", dependencyLog.toString());
            logger.error(dependencyLog.toString());
        }
    }

}
