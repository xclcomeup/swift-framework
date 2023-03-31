package com.liepin.swift.framework.mvc.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.Logger;

import com.liepin.common.file.IoUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.magic.EscapeFilter;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.monitor.cat.CatConsumer;
import com.liepin.swift.framework.mvc.util.JsonBodyPathFinder;
import com.liepin.swift.framework.util.CatHelper;

public class StreamReplicationHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger logger = Logger.getLogger(StreamReplicationHttpServletRequestWrapper.class);

    private String body;
    private byte[] data;
    private static boolean isPack = SwiftConfig.enableServletJsonSwiftAgreePack();
    private boolean isNoPack;

    public StreamReplicationHttpServletRequestWrapper(HttpServletRequest request, boolean isNoPack) {
        super(request);
        this.isNoPack = isNoPack;
        try {
            CatHelper.point("Request", "jsonStream", new CatConsumer() {

                @Override
                public void accept() throws Exception {
                    init(request);
                }

            });
        } catch (SysException e1) {
            throw e1;
        } catch (Exception e2) {
            throw new SysException(e2);
        }
    }

    private void init(HttpServletRequest request) {
        try {
            // 原始数据
            byte[] bytes = IoUtil.inputStreamToByte(request.getInputStream());
            body = new String(bytes, "UTF-8");
            if (!JsonBodyPathFinder.getUnEscapeHtml().match(request.getServletPath())) {
                // xss的双引号转义
                body = (String) EscapeFilter.escapeHtml(body);
            }
            // 不走json协议包装
            if (!isPack || isNoPack) {
                data = bytes;
                return;
            }
        } catch (Exception e) {
            logger.error("读取" + request.getServletPath() + "的请求流失败: " + e.getMessage(), e);
            throw new SysException(SystemEnum.INVALID, e);
        }

        try {
            // 提取data数据
            String dataString = JsonUtil.getJsonData(body, "data");
            if (Objects.nonNull(dataString)) {
                data = dataString.getBytes("UTF-8");
            }
        } catch (Exception e) {
            logger.error("读取" + request.getServletPath() + "的请求流失败: " + e.getMessage(), e);
            throw new SysException(SystemEnum.INVALID, e);
        }
        if (Objects.isNull(data)) {
            logger.error("读取" + request.getServletPath() + "的请求流失败: 协议体缺少data参数");
            throw new SysException(SystemEnum.INVALID);
        }
    }

    public String getBody() throws UnsupportedEncodingException {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public void setReadListener(ReadListener listener) {

            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

}
