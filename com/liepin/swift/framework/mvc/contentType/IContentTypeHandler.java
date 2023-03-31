package com.liepin.swift.framework.mvc.contentType;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface IContentTypeHandler {

    public String getContentType();

    public Map<String, Object> transform(String content) throws Exception;
    
    public Map<String, Object> transform(final HttpServletRequest request) throws Exception;
    
    public static final String CHARSET_UTF_8 = "UTF-8";
    
}
