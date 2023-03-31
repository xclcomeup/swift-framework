package com.liepin.swift.framework.mvc.upload;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.magic.EscapeFilter;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;

public class XssCommonsMultipartResolver extends CommonsMultipartResolver implements IPluginListener {

    private ControllerPlugin controllerPlugin;

    public XssCommonsMultipartResolver() {
        long maxUploadSize = PropUtil.getInstance().getLong("maxUploadSize", -1);
        super.setMaxUploadSize(maxUploadSize);
        try {
            super.setUploadTempDir(getUploadTempDir());
        } catch (IOException e) {
            throw new RuntimeException("加载上传临时目录失败: " + e.getMessage(), e);
        }
        ControllerPlugin.listen(this);
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        this.controllerPlugin = (ControllerPlugin) plugin;
    }

    @Override
    protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
        MultipartParsingResult parseRequest = super.parseRequest(request);
        Map<String, String[]> multipartParameters = parseRequest.getMultipartParameters();
        // url的请求参数白名单
        Set<String> paramNames = controllerPlugin.getUnEscapeHtmlControllerParam(request.getServletPath());

        if (multipartParameters != null) {
            for (Entry<String, String[]> entry : multipartParameters.entrySet()) {
                String name = entry.getKey();
                String[] value = entry.getValue();
                if (isWhitelist(name, paramNames)) {
                    multipartParameters.put(name, value);
                    continue;
                }
                String[] escapeArray = null;
                if (value != null && value.length != 0) {
                    if (value.length == 1) {
                        escapeArray = new String[1];
                        escapeArray[0] = escape(value[0]);
                    } else {
                        escapeArray = new String[value.length];
                        for (int i = 0; i < value.length; i++) {
                            escapeArray[i] = escape(value[i]);
                        }
                    }
                }
                multipartParameters.put(name, escapeArray);
            }
        }
        return parseRequest;
    }

    /**
     * 判断白名单
     * 
     * @param name
     * @param paramNames
     * @return
     */
    private boolean isWhitelist(String name, Set<String> paramNames) {
        return (paramNames != null) ? paramNames.contains(name) : false;
    }

    private String escape(String value) {
        return (String) EscapeFilter.escapeHtml(value);
    }

    private Resource getUploadTempDir() {
        String path = SystemUtil.getDeployDirectory("work" + System.getProperty("file.separator") + "upload");
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new FileSystemResource(dir);
    }

}
