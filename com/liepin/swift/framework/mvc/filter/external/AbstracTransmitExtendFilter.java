package com.liepin.swift.framework.mvc.filter.external;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.util.ThreadLocalUtil;

/**
 * 自定义服务透传扩展字段拦截器
 * 
 * @author yuanxl
 * @date 2016-4-13 下午05:35:05
 */
public abstract class AbstracTransmitExtendFilter extends ExternalFilter {

    @Override
    public boolean external(final HttpServletRequest request, final HttpServletResponse response) throws BizException {
        Map<String, Object> transmitExtendParams = getTransmitExtendParams(request, response);
        ThreadLocalUtil.getInstance().setExtend(transmitExtendParams);
        return true;
    }

    /**
     * 需要透传键值
     * 
     * @param request
     * @return
     */
    public abstract Map<String, Object> getTransmitExtendParams(final HttpServletRequest request,
            final HttpServletResponse response) throws BizException;

}
