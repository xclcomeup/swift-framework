package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.util.RequestUtil;

@Deprecated
public class AppFilter extends GenericFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, Object> inputMap = RequestUtil.getInput(request);
        // 判空
        if (inputMap == null || inputMap.isEmpty()) {
            throw new BizException(SystemEnum.INVALID);
        }

        Object clientIdObj = inputMap.get("client_id");
        if (clientIdObj != null) {
            // 非网关模式
            if (clientIdObj instanceof String) {
                ThreadLocalUtil.getInstance().setClientId(new String[] { (String) clientIdObj });
            } else if (clientIdObj instanceof Integer) {
                ThreadLocalUtil.getInstance().setClientId(new String[] { ((Integer) clientIdObj).toString() });
            }
        }

        // deviceUuid
        String deviceUuid = (String) inputMap.get(Const.DEVICE_UUID);
        if (deviceUuid == null) {
            deviceUuid = (String) inputMap.get("device_uuid");// 兼容app
        }
        if (deviceUuid != null) {
            // 非网关模式
            ThreadLocalUtil.getInstance().setDeviceUuid(deviceUuid);
        } else {
            // 兼容网关模式
            Optional.ofNullable(ThreadLocalUtil.getInstance().getFscpHeader()).map(t -> t.getxFscpStdInfoBeanBean())
                    .map(k -> k.getDeviceUuid()).ifPresent(duId -> {
                        if (!inputMap.containsKey("device_uuid")) {
                            inputMap.put("device_uuid", duId);// event日志显示用
                        }
                    });
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

}
