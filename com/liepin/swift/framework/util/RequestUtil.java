package com.liepin.swift.framework.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.core.consts.Const;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.util.AttributeUtil;

/**
 * 
 * @author yuanxl
 *         <p>
 *         替换类：{@link com.liepin.swift.framework.mvc.util.RequestUtil}
 */
@Deprecated
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
    public static boolean isJsonRequest(final HttpServletRequest request) {
        return request.getRequestURI().endsWith(Const.JSON_SUFFIX);
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
    public static boolean isAppRequest(final HttpServletRequest request) {
        return isJsonRequest(request) && !isAjaxRequest(request);
    }

    /**
     * 是否压缩请求
     * <p>
     * header标识 accept-encoding: gzip
     * 
     * @param request
     * @return
     */
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
    public static boolean isWxaRequest(final HttpServletRequest request) {
        String header = request.getHeader("X-Client-Type");
        return header != null && header.toLowerCase().equals("wxa");
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

}
