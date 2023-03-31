package com.liepin.swift.framework.mvc.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.liepin.common.conf.PropUtil;

/**
 * 上传文件处理模块
 * 
 * @author yuanxl
 * 
 */
@Deprecated
public class UploadHandler {

    private static final Logger logger = Logger.getLogger(UploadHandler.class);

    private DiskFileItemFactory factory;

    public UploadHandler() {
        this.factory = new DiskFileItemFactory();
        factory.setSizeThreshold(PropUtil.getInstance().getInt("upload.file.SizeThreshold", 10240));
        String dir = PropUtil.getInstance().get("upload.file.dir");
        if (dir != null) {
            // default System.getProperty("java.io.tmpdir")
            factory.setRepository(new File(dir));
        }
    }

    @SuppressWarnings("unchecked")
    public List<UploadFileDto> process(final Map<String, Object> inputMap, final HttpServletRequest req) {
        List<UploadFileDto> dtos = new ArrayList<UploadFileDto>();
        try {
            ServletFileUpload upload = new ServletFileUpload(factory);
            // 设置允许上传的最大文件大小 100MB
            upload.setSizeMax(100 * 1024 * 1024);
            // 解析HTTP请求消息头
            List<FileItem> fileItems = upload.parseRequest(req);
            Iterator<FileItem> iterator = fileItems.iterator();
            UploadFileDto dto = null;
            boolean doParameter = false;
            int i = 0;
            while (iterator.hasNext()) {
                FileItem item = (FileItem) iterator.next();
                if (item.isFormField()) {
                    if (i == 0) {
                        doParameter = true;
                    }
                    if (doParameter) {
                        processFormField(item, inputMap);
                    } else {
                        processFormField(item, dto);
                    }
                } else {
                    doParameter = false;
                    if (dto != null) {
                        dtos.add(dto);
                    }
                    dto = new UploadFileDto();
                    processUploadFile(item, dto);
                }
                i++;
            }
            if (dto != null) {
                dtos.add(dto);
            }
        } catch (Exception e) {
            logger.error("文件上传处理失败: " + e.getMessage(), e);
        }
        return dtos;
    }

    /**
     * 处理表单
     */
    private void processFormField(FileItem item, final UploadFileDto dto) throws Exception {
        String name = item.getFieldName();
        String value = item.getString("UTF-8");
        dto.appendMetadata(name, value);
    }

    @SuppressWarnings("unchecked")
    private void processFormField(FileItem item, final Map<String, Object> inputMap) throws Exception {
        String name = item.getFieldName();
        String value = item.getString("UTF-8");
        Object obj = inputMap.get(name);
        if (obj != null) {
            if (obj instanceof List) {
                List<String> list = (List<String>) obj;
                list.add(value);
            } else if (obj instanceof String) {
                List<String> list = new ArrayList<String>();
                list.add((String) obj);
                list.add(value);
                inputMap.put(name, list);
            }
        } else {
            inputMap.put(name, value);
        }
    }

    /**
     * 处理文件
     * 
     * @param item
     * @param dto
     * @throws Exception
     */
    private void processUploadFile(FileItem item, final UploadFileDto dto) throws Exception {
        dto.setFieldName(item.getFieldName());
        String filename = item.getName();
        int index = filename.lastIndexOf("\\");
        filename = filename.substring(index + 1, filename.length());
        dto.setName(filename);
        dto.setBytes(item.get());
        dto.setContentType(item.getContentType());
    }

}
