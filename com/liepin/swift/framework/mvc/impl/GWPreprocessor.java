package com.liepin.swift.framework.mvc.impl;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.liepin.common.json.JsonUtil;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.UrlUtil;

public class GWPreprocessor extends RPCPreprocessor {

    @Override
    public void preprocess(Map<String, Object> inputMap, HttpServletRequest request) throws BizException {
        super.preprocess(inputMap, request);

        // API网关
        // 初始化网关客户端信息信息传递参数
        String gwClientInfoStr = request.getHeader("X-Gw-Client-Info".toLowerCase());
        gwClientInfoStr = Optional.ofNullable(gwClientInfoStr).map(t -> {
            try {
                return URLDecoder.decode(t, "UTF-8");
            } catch (Exception e) {
                return t;
            }
        }).orElse(null);
        inputMap.put(Const.GW_CLIENT_INFO, gwClientInfoStr);// event日志显示用

        // 兼容deviceUuid
        if ("".equals(ThreadLocalUtil.getInstance().getDeviceUuid())) {
            // 如果为空，从api网关获取
            Optional.ofNullable(gwClientInfoStr).map(t -> JsonUtil.json2map(t)).map(k -> (String) k.get("deviceUuid"))
                    .ifPresent(o -> ThreadLocalUtil.getInstance().setDeviceUuid(o));
        }

        // 请求是否来自网关
        String xAltGw = request.getHeader("X-Alt-Gw");
        Optional.ofNullable(RequestUtil.parseHeaderXAltGw4Type(xAltGw)).ifPresent(t -> {
            ThreadLocalUtil.getInstance().setRequestFromGw(t);
        });

        // 初始化流量灰度标记
        Optional.ofNullable(HeadReader.Gw.getGcId(request)).ifPresent(gcId -> {
            ThreadLocalUtil.getInstance().setFlowGrayId(gcId);// 透传
            inputMap.put(Const.FLOW_GRAY_ID, gcId);// event日志显示用
        });

        String apiKey = logRequestServiceName(request);

        Map<String, String> headers = HeadReader.getAll(request);

        String principalStr = headers.get("X-Alt-Principal".toLowerCase());
        Map<String, Object> principal = (principalStr != null) ? JsonUtil.json2map(principalStr)
                : Collections.emptyMap();

        ThreadLocalUtil.getInstance().set(Const.GATEWAY_KEY, apiKey);
        ThreadLocalUtil.getInstance().set(Const.GATEWAY_PRINCIPAL, principal);
        ThreadLocalUtil.getInstance().set(Const.GATEWAY_HEADERS, headers);
        ThreadLocalUtil.getInstance().set(Const.GATEWAY_COOKIE, request.getCookies());

    }

    private String logRequestServiceName(HttpServletRequest req) {
        return UrlUtil.removeNamespace(req.getServletPath(), UrlUtil.getNamespace4GWAPI());
    }

}
