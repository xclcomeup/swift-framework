package com.liepin.swift.framework.mvc.impl;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.liepin.common.conf.SystemUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.other.StringUtil;
import com.liepin.swift.core.bean.RpcContext;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceId;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.mvc.filter.AbstractPreprocessor;

public class RPCPreprocessor extends AbstractPreprocessor {

    @SuppressWarnings("deprecation")
    @Override
    public void preprocess(Map<String, Object> inputMap, HttpServletRequest req) throws BizException {
        // 判空
        if (inputMap == null || inputMap.isEmpty()) {
            throw new SysException(SystemEnum.INVALID);
        }

        // CURRENT_USER_ID
        String currentUserIdStr = (String) inputMap.get(Const.CURRENT_USER_ID);
        if (StringUtil.string2int(currentUserIdStr, -1) < 0) {
            throw new SysException(SystemEnum.MISS_CURRENTUSERID);
        }
        // 放入threadLoacl里
        ThreadLocalUtil.getInstance().setCurrentUserId(currentUserIdStr);

        // CLIENT_IDS
        String clientIdsStr = (String) inputMap.get(Const.CLIENT_IDS);
        if (clientIdsStr == null || clientIdsStr.length() == 0) {
            throw new SysException(SystemEnum.MISS_CLIENTIDS);
        }
        String[] clientIds = clientIdsStr.split(",");
        ThreadLocalUtil.getInstance().setClientId(clientIds);

        // ORIGINAL_IP
        String originalIP = (String) inputMap.get(Const.ORIGINAL_IP);
        if (originalIP != null) {
            ThreadLocalUtil.getInstance().setOriginalIP(originalIP);
        }

        // biInfo
        String biInfo = (String) inputMap.get(Const.BI_INFO);
        if (biInfo != null) {
            ThreadLocalUtil.getInstance().setBIInfo(biInfo);
        }

        // deviceUuid
        String deviceUuid = (String) inputMap.get(Const.DEVICE_UUID);
        if (deviceUuid != null) {
            ThreadLocalUtil.getInstance().setDeviceUuid(deviceUuid);
        }

        // traceId
        String traceId = (String) inputMap.get(Const.TRACEID);
        TraceId currentTrackId = TraceIdUtil.trace(traceId);
        if (traceId == null) {
            inputMap.put(Const.TRACEID, currentTrackId.toString());// 显示在eventinfo日志
        }

        // time river
        String timeRiverStr = (String) inputMap.get(Const.TIME_RIVER);
        if (timeRiverStr != null && timeRiverStr.trim().length() != 0) {
            try {
                RpcContext rpcContext = new RpcContext(timeRiverStr);
                rpcContext.record();// 记录框架接收RPC请求时间
                inputMap.put(Const.TIME_RIVER, rpcContext.getTimePlan());// 显示在eventinfo日志
            } catch (Exception e) {
            }
        }

        // initiateUrl
        String initiateUrl = (String) inputMap.get(Const.INITIATE_URL);
        if (initiateUrl == null) {
            inputMap.put(Const.INITIATE_URL, "");// eventinfo日志可以显示
        } else {
            ThreadLocalUtil.getInstance().setInitiateUrl(initiateUrl);
        }

        // extend
        String extend = (String) inputMap.get(Const.TRANSMIT_EXTEND);
        if (extend != null) {
            ThreadLocalUtil.getInstance().setExtend(JsonUtil.json2map(extend));
        }

        // version
        String version = (String) inputMap.get(Const.VERSION);
        if (version != null) {
            ThreadLocalUtil.getInstance().setVersion(version);
        }

        // area
        String area = (String) inputMap.get(Const.AREA);
        if (area == null || area.trim().length() == 0) {
            area = SystemUtil.getLogicAreaStr();
        }
        ThreadLocalUtil.getInstance().setArea(area);

        // rootDomain
        String rootDomain = (String) inputMap.get(Const.ROOT_DOMAIN);
        if (rootDomain == null || rootDomain.trim().length() == 0) {
            inputMap.put(Const.ROOT_DOMAIN, "");// eventinfo日志可以显示
        } else {
            ThreadLocalUtil.getInstance().setRootDomain(rootDomain);
        }

        // grayId
        String grayId = (String) inputMap.get(Const.FLOW_GRAY_ID);
        Optional.ofNullable(grayId).ifPresent(t -> ThreadLocalUtil.getInstance().setFlowGrayId((t)));

        // auth
        // String uri = req.getServletPath();
        // String clientId = clientIds[clientIds.length - 1];// 取最后一位
        // TODO 在ins-auth 增加服务层级调用控制
        // boolean isAuthed = auth.isAuthed(Integer.parseInt(clientId),
        // requestClientIp, uri);
        // if (!isAuthed) {
        // throw new SysException(SystemCode.Auth);
        // }
    }

}
