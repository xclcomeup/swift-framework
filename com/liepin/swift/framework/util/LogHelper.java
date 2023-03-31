package com.liepin.swift.framework.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.framework.mvc.util.AttributeUtil;

public class LogHelper {

    private static final Logger logger = Logger.getLogger(LogHelper.class);

    public static void logError(Throwable actual, HttpServletRequest request) {
        if (actual instanceof BizException) {
            IMessageCode messageCode = (IMessageCode) actual;
            MonitorLogger.getInstance().log(message(request) + ", BizException [code=" + messageCode.code()
                    + ", message=" + messageCode.message() + "]", actual);
            // ((BizException) actual).commit();
        } else {
            logger.error(message(request), actual);
        }
    }

    private static String message(HttpServletRequest request) {
        return "HTTP request with URI[" + request.getRequestURI() + "] Handler handle failed, traceId="
                + ThreadLocalUtil.getInstance().getTraceId() + ignoreTag(request);
    }

    private static String ignoreTag(HttpServletRequest request) {
        if (AttributeUtil.hasErrorIgnore(request)) {
            return " #$# 【安全扫描，忽略】";
        }
        return "";
    }

}
