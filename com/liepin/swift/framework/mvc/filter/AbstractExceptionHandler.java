package com.liepin.swift.framework.mvc.filter;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.util.RequestUtil;

public abstract class AbstractExceptionHandler {

    private final static Logger logger = Logger.getLogger(AbstractExceptionHandler.class);

    private static ExceptionKind configKind = ExceptionKind
            .valueAs(PropUtil.getInstance().get(Const.LOG_PARAM_WHEN_EXCEPTION));

    /**
     * 异常处理方法 各个业务可以根据自己定义的Excepiton覆盖此方法，实现自己的Exception处理. 默认返回UNKNOWN
     * 
     * @param request
     * @param throwable
     * @return
     */
    public ResultStatus handle(HttpServletRequest request, Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
        return ResultStatus.unknown(throwable.getMessage());
    }

    /**
     * 
     * @param request
     * @param kind
     * @return
     */
    protected String printRequest(HttpServletRequest request, ExceptionKind kind) {
        if (Objects.isNull(configKind)) {
            return "";
        }
        if (ExceptionKind.asALL(configKind) || (ExceptionKind.asBIZ(configKind) && ExceptionKind.asBIZ(kind))
                || (ExceptionKind.asDAO(configKind) && ExceptionKind.asDAO(kind))) {
            return " input=" + JsonUtil.toJson(RequestUtil.getInput(request)) + " traceId="
                    + ThreadLocalUtil.getInstance().getTraceId();
        }
        return "";
    }

    protected void errorLog(final HttpServletRequest request, final Throwable actual, ExceptionKind kind) {
        errorLog(request, actual, kind, "");
    }

    protected void errorLog4Uncaught(final HttpServletRequest request, final Throwable actual, ExceptionKind kind) {
        errorLog(request, actual, kind, "Uncaught Exception: ");
    }

    private void errorLog(final HttpServletRequest request, final Throwable actual, ExceptionKind kind, String tag) {
        logger.error(
                tag + "traceId=" + TraceIdUtil.getTraceId() + " " + actual.getMessage() + printRequest(request, kind),
                actual);
    }

    protected void bizLog(final HttpServletRequest request, IMessageCode messageCode) {
        MonitorLogger.getInstance()
                .log("servletPath=" + request.getServletPath() + " BizException [code=" + messageCode.code()
                        + ", message=" + messageCode.message() + "]" + printRequest(request, ExceptionKind.BIZ),
                        (BizException) messageCode);
    }

    protected enum ExceptionKind {
        BIZ, DAO, OTHER, ALL;

        public static ExceptionKind valueAs(String value) {
            if (Objects.isNull(value)) {
                return null;
            }
            switch (value) {
                case "BIZ":
                    return BIZ;
                case "DAO":
                    return DAO;
                case "OTHER":
                    return OTHER;
                case "ALL":
                    return ALL;
                default:
                    return null;
            }
        }

        public static boolean asALL(ExceptionKind value) {
            return ALL == value;
        }

        public static boolean asBIZ(ExceptionKind value) {
            return BIZ == value;
        }

        public static boolean asDAO(ExceptionKind value) {
            return DAO == value;
        }

    }

}
