package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.common.json.JsonUtil;
import com.liepin.common.other.StringUtil;
import com.liepin.swift.core.bean.FscpHeader;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.RequestUtil;

public class HeaderFilter extends GenericFilter {

    @SuppressWarnings("deprecation")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 接入网关
        // 获取前端服务端通信协议
        FscpHeader fscpHeader = FscpHeader.build(request);
        ThreadLocalUtil.getInstance().setFscpHeader(fscpHeader);

        // 上下文注入
        // 注入clienId
        Optional.ofNullable(fscpHeader.getxFscpStdInfoBeanBean()).map(t -> t.getClientId()).ifPresent(clientId -> {
            ThreadLocalUtil.getInstance().setClientId(new String[] { clientId });
        });
        // 注入deviceUuid
        Optional.ofNullable(fscpHeader.getxFscpStdInfoBeanBean()).map(t -> t.getDeviceUuid()).ifPresent(deviceUuid -> {
            ThreadLocalUtil.getInstance().setDeviceUuid(deviceUuid);
        });
        // 注入traceId
        Optional.ofNullable(fscpHeader.getxFscpTraceId()).ifPresent(traceId -> {
            TraceIdUtil.trace(traceId);
        });

        // 网关鉴权信息传递协议
        Map<String, Object> principal = Optional.ofNullable(HeadReader.Gw.getAltPrincipal(request)).map(t -> {
            return JsonUtil.json2map(t);
        }).orElse(Collections.emptyMap());
        ThreadLocalUtil.getInstance().set(Const.GW_ACCESS_PRINCIPAL, principal);
        // 注入currentUserId
        Optional.ofNullable((String) principal.get("gw-prl-user-id")).ifPresent(userId -> {
            if (StringUtil.string2int(userId, -1) < 0) {
                throw new SysException(SystemEnum.INVALID_CURRENTUSERID);
            }
            ThreadLocalUtil.getInstance().setCurrentUserId(userId);
        });

        // 请求是否来自网关
        Optional.ofNullable(RequestUtil.parseHeaderXAltGw4Type(HeadReader.Gw.getAltGw(request))).ifPresent(t -> {
            ThreadLocalUtil.getInstance().setRequestFromGw(t);
        });

        // 用户流量灰度标记
        Optional.ofNullable(HeadReader.Gw.getGcId(request)).ifPresent(gcId -> {
            ThreadLocalUtil.getInstance().setFlowGrayId(gcId);
        });

        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
