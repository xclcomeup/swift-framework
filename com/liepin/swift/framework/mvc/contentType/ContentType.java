package com.liepin.swift.framework.mvc.contentType;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liepin.common.file.IoUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.swift.framework.mvc.http.StreamReplicationHttpServletRequestWrapper;
import com.liepin.swift.framework.mvc.util.FormUtil;
import com.liepin.swift.framework.mvc.util.XmlUtil;

public enum ContentType implements IContentTypeHandler {

    JSON_UTF8() {

        @Override
        public String getContentType() {
            return "application/json;charset=UTF-8";
        }

        @Override
        public Map<String, Object> transform(String content) throws Exception {
            return null;// nothing
        }

        @Override
        public Map<String, Object> transform(HttpServletRequest request) throws Exception {
            return null;// nothing
        }

    },

    FORM() {

        @Override
        public String getContentType() {
            return "application/x-www-form-urlencoded";
        }

        @Override
        public Map<String, Object> transform(String content) throws Exception {
            throw new UnsupportedOperationException("Content-Type:\"" + getContentType() + "\" 类型转换不支持!");
        }

        @Override
        public Map<String, Object> transform(final HttpServletRequest request) throws Exception {
            return FormUtil.form2Map(request);
        }

    },
    JSON() {

        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public Map<String, Object> transform(String content) throws Exception {
            return JsonUtil.json2map(content);
        }

        @Override
        public Map<String, Object> transform(HttpServletRequest request) throws Exception {
            String json = null;
            if (request instanceof StreamReplicationHttpServletRequestWrapper) {
                StreamReplicationHttpServletRequestWrapper requestWrapper = (StreamReplicationHttpServletRequestWrapper) request;
                json = requestWrapper.getBody();
            } else {
                byte[] inputStreamToByte = IoUtil.inputStreamToByte(request.getInputStream());
                json = new String(inputStreamToByte, "UTF-8");
            }
            return transform(json);
        }

    },
    @Deprecated
    XMLTEXT() {

        @Override
        public String getContentType() {
            return "text/xml";
        }

        @Override
        public Map<String, Object> transform(String content) throws Exception {
            return XmlUtil.xml2Map(new ByteArrayInputStream(content.getBytes(CHARSET_UTF_8)));
        }

        @Override
        public Map<String, Object> transform(HttpServletRequest request) throws Exception {
            return XmlUtil.xml2Map(request.getInputStream());
        }

    },
    MULTIPART_FORMDATA() {

        // private UploadHandler uploadHandler = new UploadHandler();

        @Override
        public String getContentType() {
            return "multipart/form-data";
        }

        @Override
        public Map<String, Object> transform(String content) throws Exception {
            throw new UnsupportedOperationException("Content-Type:\"" + getContentType() + "\" 类型转换不支持!");
        }

        @Override
        public Map<String, Object> transform(HttpServletRequest request) throws Exception {
            Map<String, Object> input = FormUtil.form2Map(request);// 只读取表单参数，文件流不读取
            // List<UploadFileDto> dtos = uploadHandler.process(input, request);
            // input.put(Const.UPLOAD_FILE, dtos);
            return input;
        }

    };

    public static ContentType support(String contentType) {
        if (contentType == null || contentType.trim().length() == 0
                || contentType.toLowerCase().startsWith(ContentType.FORM.getContentType())) {
            return FORM;
        } else if (contentType.toLowerCase().startsWith(ContentType.JSON.getContentType())) {
            return JSON;
        } else if (contentType.toLowerCase().startsWith(ContentType.XMLTEXT.getContentType())) {
            return XMLTEXT;
        } else if (contentType.toLowerCase().startsWith(ContentType.MULTIPART_FORMDATA.getContentType())) {
            return MULTIPART_FORMDATA;
        } else {
            return null;
        }
    }

}
