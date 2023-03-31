package com.liepin.swift.framework.rpc;

import java.io.Serializable;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.framework.util.RpcProtocol.ErrorType;

/**
 * 接口返回包装类
 * 
 * @author yuanxl
 * 
 * @param <T>
 */
public class PlatformResult<T> implements Serializable {

    private static final long serialVersionUID = -7801072556594162830L;

    private String code = SystemEnum.OK.code();
    private String message;
    private T data;
    private Throwable throwable;

    private ErrorType errorType;
    private String bizCode;// 业务异常

    public PlatformResult() {
    }

    public PlatformResult(String code, String message) {
        this(code, message, null);
    }

    public PlatformResult(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public PlatformResult(IMessageCode messageCode) {
        this(messageCode.code(), messageCode.message(), null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isOK() {
        return SystemEnum.OK.code().equals(code);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    @Override
    public String toString() {
        return "PlatformResult [code=" + code + ", message=" + message + ", data=" + data + ", throwable=" + throwable
                + ", errorType=" + errorType + ", bizCode=" + bizCode + "]";
    }

}
