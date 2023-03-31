package com.liepin.swift.framework.mvc.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.core.consts.Const;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.contentType.ContentType;
import com.liepin.swift.framework.util.IPUtil;
import com.liepin.swift.framework.util.UrlUtil;

public class RequestUtil {

    /**
     * 请求输入参数map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getInput(final HttpServletRequest request) {
        return (Map<String, Object>) request.getAttribute(ResultStatus.INPUT);
    }

    /**
     * 请求返回结果
     */
    public static Object getOutput(final HttpServletRequest request) {
        return request.getAttribute(ResultStatus.OUTPUT);
    }

    /**
     * 放入请求参数
     * 
     * @param request
     * @param input
     */
    public static void setInput(final HttpServletRequest request, Map<String, Object> input) {
        request.setAttribute(ResultStatus.INPUT, input);
    }

    /**
     * 放入返回结果
     * 
     * @param request
     * @param output
     */
    public static void setOutput(final HttpServletRequest request, final Object output) {
        request.setAttribute(ResultStatus.OUTPUT, output);
    }

    /**
     * 清理
     * 
     * @param request
     */
    public static void clean(final HttpServletRequest request) {
        request.removeAttribute(ResultStatus.INPUT);
        request.removeAttribute(ResultStatus.OUTPUT);
        AttributeUtil.clean(request);
    }

    @Deprecated
    public static Map<String, Object> getParameters(HttpServletRequest request) {
        Set<Entry<String, String[]>> set = request.getParameterMap().entrySet();
        Map<String, Object> inputMap = new LinkedHashMap<String, Object>();
        for (Entry<String, String[]> entry : set) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            if (value != null && value.length != 0) {
                if (value.length == 1) {
                    inputMap.put(key, value[0]);
                } else {
                    inputMap.put(key, Arrays.asList(value));
                }
            }
        }
        return inputMap;
    }

    /**
     * 判断是否Ajax请求
     * <p>
     * header标识 X-Requested-With: XMLHttpRequest<br>
     * 或者<br>
     * 请求带callback参数<br>
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isAjaxRequest(final HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (null != request.getParameter("callback"));
    }

    /**
     * 判断是否Json请求
     * <p>
     * uri以 .json结尾
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isJsonRequest(final HttpServletRequest request) {
        return request.getRequestURI().endsWith(Const.JSON_SUFFIX);
    }

    /**
     * 判断是否GW请求
     * <p>
     * uri以 /GW/开头
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isGWRequest(final HttpServletRequest request) {
        return request.getServletPath().startsWith(UrlUtil.getNamespace4GWAPI());
    }

    /**
     * 判断是否App请求
     * <p>
     * 判断标准:<br>
     * 1. {@link #isJsonRequest(HttpServletRequest)}<br>
     * 2. !{@link #isAjaxRequest(HttpServletRequest)}<br>
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isAppRequest(final HttpServletRequest request) {
        return (isJsonRequest(request) && !isAjaxRequest(request)) || HeadReader.isAppRequest(request);
    }

    /**
     * 是否压缩请求
     * <p>
     * header标识 accept-encoding: gzip
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isGzipRequest(final HttpServletRequest request) {
        String header = request.getHeader("accept-encoding");
        if (header != null && (header.toLowerCase().indexOf("gzip") != -1)) {
            return true;
        }
        return false;
    }

    /**
     * 是否wxa 微信小程序请求
     * <p>
     * header标识 X-Client-Type: wxa
     * 
     * @param request
     * @return
     */
    @Deprecated
    public static boolean isWxaRequest(final HttpServletRequest request) {
        String header = request.getHeader("X-Client-Type");
        return header != null && header.toLowerCase().equals("wxa");
    }

    /**
     * 判断是否内部请求
     * <p>
     * uri以 .do结尾
     * 
     * @param request
     * @return
     */
    public static boolean isInnerRequest(final HttpServletRequest request) {
        return request.getRequestURI().endsWith(".do");
    }

    /**
     * 判断压缩<br>
     * 带有压缩header标识，并且是RPC请求<br>
     * 带有压缩header标识，并且非RPC请求，并且url是.json结尾，并且非ajax请求，并且非wxa请求<br>
     * 
     * @param request
     * @param isRPC
     * @return
     */
    @Deprecated
    public static boolean needGzipHandle(final HttpServletRequest request, boolean isRPC) {
        return isGzipRequest(request)
                && (isRPC || (!isRPC && isJsonRequest(request) && !isAjaxRequest(request) && !isWxaRequest(request)));
    }

    /**
     * 获取内网远程地址真实ip地址
     * 
     * @param request
     * @return
     */
    public static String getClientIp(final HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * 获取根域名 http://www.liepin.com => liepin.com
     * 
     * @param domain
     * @return
     */
    public static String getRootDomain(final HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String uri = request.getRequestURI();
        String domain = url.delete(url.length() - uri.length(), url.length()).toString();
        int pos = domain.lastIndexOf(".");
        if (pos == -1) {
            return domain;
        }
        String temp = domain.substring(0, pos);
        int second = temp.lastIndexOf(".");
        if (second == -1) {
            int pos1 = domain.indexOf("://");
            if (pos1 != -1) {
                return domain.substring(pos1 + 3);
            }
            return domain;
        }
        return domain.substring(second + 1);
    }

    public static boolean checkInnerRequest(final HttpServletRequest request) {
        String ip = IPUtil.getIpAddr(request);
        if ("127.0.0.1".equals(ip)) {
            return true;
        }
        if (IPUtil.isPrivateIp(ip)) {
            return true;
        }
        return false;
    }

    /**
     * 请求头value格式: 使用(分号做分隔符),注意分号是英文分号(或者说是ascii码分号)
     * <p>
     * 示例: X-Alt-Gw: type=access;otherKey=otherValue;
     * 
     * @param value
     * @return
     */
    public static Map<String, String> parseHeaderXAltGw(String value) {
        return Optional.ofNullable(value).map(t -> {
            Map<String, String> data = new HashMap<>();
            String[] array = t.trim().split(";");
            for (String tmp : array) {
                String[] array1 = tmp.trim().split("=");
                if (array1.length == 2) {
                    data.put(array1[0], array1[1]);
                }
            }
            return data;
        }).orElse(null);
    }

    public static String parseHeaderXAltGw4Type(String value) {
        Map<String, String> kvs = parseHeaderXAltGw(value);
        return Optional.ofNullable(kvs).map(t -> t.get("type")).map(v -> {
            switch (v) {
                case "api":
                case "access":
                case "open":
                    return v;
                default:
                    return null;
            }
        }).orElse(null);
    }

    /**
     * 请求是否json格式
     * <p>
     * header参考：Content-Type: application/json;charset=UTF-8<br>
     * 
     * @param request
     * @return
     */
    public static boolean isJSONRequest(final HttpServletRequest request) {
        ContentType contentType = ContentType.support(request.getContentType());
        return Optional.ofNullable(contentType).map(c -> {
            return ContentType.JSON == c;
        }).orElse(Boolean.FALSE);
    }

    /**
     * 判断获取客户端希望接收的body数据类型是否json类型
     * 
     * @param request
     * @return
     */
    public static boolean isAccept4JsonRequest(final HttpServletRequest request) {
        String accept = HeadReader.getAccept(request);
        return Optional.ofNullable(accept).map(a -> {
            return a.trim().startsWith("application/json");
        }).orElse(Boolean.FALSE);
    }

    public static boolean isPostRequest(final HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }

    public static boolean isGetRequest(final HttpServletRequest request) {
        return "GET".equals(request.getMethod());
    }

}
