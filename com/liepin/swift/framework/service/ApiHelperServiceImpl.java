package com.liepin.swift.framework.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

import com.liepin.swift.framework.conf.SwiftConfig;

public class ApiHelperServiceImpl implements IApiHelperService {

    private static final String METADATA = "/maven-metadata.xml";

    public ApiHelperServiceImpl() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getProjectVersions(String projectName) {
        int pos = projectName.indexOf("platform");
        String clientName = projectName.substring(0, pos) + "client";
        String address = SwiftConfig.NEXUS_RELEASES_URI + clientName + METADATA;

        SAXReader reader = new SAXReader();
        Document doc = null;
        try {
            doc = reader.read(address);
        } catch (DocumentException e) {
            throw new RuntimeException("Read " + address + " fail, please check it first!!");
        }

        Element root = doc.getRootElement();
        Element versioning = root.element("versioning");
        Element versions = versioning.element("versions");
        List<Element> elements = versions.elements("version");
        List<String> list = new ArrayList<String>();
        for (Element element : elements) {
            list.add(element.getText());
        }
        return list;
    }

    @Override
    public InputStream getProjectApis(String projectName, String version) {
        // TODO Auto-generated method stub
        return null;
    }

}
