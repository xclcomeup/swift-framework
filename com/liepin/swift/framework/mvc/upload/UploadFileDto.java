package com.liepin.swift.framework.mvc.upload;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传dto
 * 
 * @author yuanxl
 * 
 */
@Deprecated
public class UploadFileDto {

    private String name;
    private String fieldName;
    private byte[] bytes;
    private String contentType;
    private Map<String, String> metadata = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void appendMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

}
