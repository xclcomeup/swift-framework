package com.liepin.swift.framework.mvc.impl;

import javax.servlet.http.HttpServletRequest;

import com.liepin.dao.exception.DaoException;
import com.liepin.dao.mongodb.MongodbException;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.filter.AbstractExceptionHandler;

/**
 * 异常处理类
 * <p>
 * 目前处理以下4类异常
 * 
 * @see BizException
 * @see SysException
 * @see DaoException
 * @see MongodbException
 * 
 * @author yuanxl
 * 
 */
public class RPCExceptionHandler extends AbstractExceptionHandler {

    @Override
    public ResultStatus handle(HttpServletRequest request, Throwable actual) {
        ResultStatus rs = ResultStatus.unknown();
        if (actual instanceof IMessageCode) {
            IMessageCode messageCode = (IMessageCode) actual;
            if (actual instanceof BizException) {
                bizLog(request, messageCode);
                String bizCode = messageCode.code();
                ((BizException) actual).commit();// 兼容
                rs = new ResultStatus(messageCode);
                rs.setBizCode(bizCode);
            } else {
                errorLog(request, actual, (actual instanceof DaoException || actual instanceof MongodbException)
                        ? ExceptionKind.DAO : ExceptionKind.OTHER);
                rs = new ResultStatus(messageCode);
            }
        } else {
            // 未知异常
            errorLog4Uncaught(request, actual, ExceptionKind.OTHER);
        }
        return rs;
    }

}
