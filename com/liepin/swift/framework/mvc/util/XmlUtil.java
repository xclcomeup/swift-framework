package com.liepin.swift.framework.mvc.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XmlUtil {

    public static Map<String, Object> xml2Map(final InputStream inputStream) {
        Map<String, Object> inputMap = new HashMap<String, Object>();
        SAXReader reader = new SAXReader();
        Document doc = null;
        try {
            // 防御XXE攻击
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            // 是否包含外部生成的实体。当正在解析文档时为只读属性，未解析文档的状态下为读写。
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // 是否包含外部的参数，包括外部DTD子集。当正在解析文档时为只读属性，未解析文档的状态下为读写。
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            doc = reader.read(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("getXml() read InputStream fail", e);
        }
        Element rootElement = doc.getRootElement();

        List<?> elements = rootElement.elements();
        for (Object obj : elements) {
            Element element = (Element) obj;
            inputMap.put(element.getName(), element.getTextTrim());
        }
        return inputMap;
    }

}
