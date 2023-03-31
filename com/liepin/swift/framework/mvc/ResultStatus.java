package com.liepin.swift.framework.mvc;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.IMessageCode;

public class ResultStatus {

    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String DATA = "data";

    public static String status() {
        return STATUS;
    }

    public static String message() {
        return MESSAGE;
    }

    public static String data() {
        return DATA;
    }

    public static final String INPUT = "input"; // 输入请求
    public static final String OUTPUT = "output"; // 返回请求

    private String status;
    private String message;
    private Object data;

    private String bizCode;

    public ResultStatus(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public ResultStatus(IMessageCode messageCode) {
        this.status = messageCode.code();
        this.message = messageCode.message();
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public ResultStatus setData(Object data) {
        this.data = data;
        return this;
    }
    
    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    @Override
    public String toString() {
        return String.format("ResultStatus [status=%s, message=%s]", status, message);
    }

    public static ResultStatus ok() {
        return new ResultStatus(SystemEnum.OK.code(), SystemEnum.OK.message());
    }

    public static ResultStatus unknown() {
        return new ResultStatus(SystemEnum.UNKNOWN.code(), SystemEnum.UNKNOWN.message());
    }

    public static ResultStatus unknown(String message) {
        return new ResultStatus(SystemEnum.UNKNOWN.code(), message);
    }

}
