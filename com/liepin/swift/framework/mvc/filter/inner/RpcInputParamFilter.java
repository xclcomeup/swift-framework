package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.common.other.GzipUtil;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.mvc.compress.ContentEncoding;
import com.liepin.swift.framework.mvc.contentType.ContentType;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.RequestUtil;

public class RpcInputParamFilter extends GenericFilter {

    private static final Logger logger = Logger.getLogger(RpcInputParamFilter.class);

    public RpcInputParamFilter() {
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RequestUtil.setInput(request, Optional.ofNullable(getInput(request)).orElseGet(HashMap::new));
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

    protected Map<String, Object> getInput(HttpServletRequest request) {
        Map<String, Object> input = null;

        // contentType排除
        ContentType contentType = ContentType.support(request.getContentType());
        if (contentType == null) {
            MonitorLogger.getInstance().log("不支持的Http Content-Type请求: Content-Type:" + request.getContentType()
                    + ", ServletPath=" + request.getServletPath() + ", Referer=" + request.getHeader("referer"));
            return input;
        }
        // 解析
        try {
            // 新的压缩请求标准
            ContentEncoding contentEncoding = ContentEncoding.support(request);
            if (Objects.nonNull(contentEncoding)) {
                if (!HeadReader.noContentLength(request)) {
                    String content = contentEncoding.decompress(request);
                    input = Optional.ofNullable(content).map(t -> {
                        try {
                            return contentType.transform(t);
                        } catch (Exception e) {
                            logger.error("contentType=" + contentType.getContentType() + "的请求转换失败: " + e.getMessage(),
                                    e);
                            return null;
                        }
                    }).orElse(null);
                }
            } else {
                // TODO 兼容老的压缩请求标准，待后续框架ins-swift-router版本>=2.0.17都升级完后去掉
                if (HeadReader.isAcceptEncodingRequest(request)) {
                    if (!HeadReader.noContentLength(request)) {
                        String content = GzipUtil.uncompress(request.getInputStream());
                        if (content != null && content.trim().length() != 0) {
                            if (ContentType.JSON == contentType) {
                                input = ContentType.JSON.transform(content);
                            }
                        }
                    }
                } else {
                    if (ContentType.FORM == contentType) {
                        input = ContentType.FORM.transform(request);
                    } else if (ContentType.JSON == contentType) {
                        // FIXME 标准协议，后续支持
                    }
                }
            }
        } catch (Exception e) {
            logger.error(
                    "读取" + request.getServletPath() + "的请求参数失败: " + e.getMessage() + ". 打印: " + printHeader(request),
                    e);
            throw new SysException(SystemEnum.INVALID, e);
        }
        if (Objects.isNull(input)) {
            logger.warn("读取" + request.getServletPath() + "的请求参数为空, 打印: " + printHeader(request));
        }
        return input;
    }

}
