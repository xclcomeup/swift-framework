package com.liepin.swift.framework.rpc.router;

import com.liepin.router.handler.AbstractExceptionHandler;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.framework.rpc.PlatformResult;

public class CommonExceptionHandler extends AbstractExceptionHandler<PlatformResult<?>> {

    @SuppressWarnings("rawtypes")
    @Override
    public PlatformResult<?> handle(Exception e) {
        // TODO 超时统一汇总日志
        PlatformResult<?> pr = new PlatformResult(SystemEnum.TIMEOUT);
        pr.setThrowable(e);
        return pr;
    }

}
