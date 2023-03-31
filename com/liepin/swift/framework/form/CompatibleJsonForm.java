package com.liepin.swift.framework.form;

@Deprecated
public class CompatibleJsonForm extends JsonForm {

    // 兼容部分
    private Object status = 0;
    private String message = "OK";

    public CompatibleJsonForm() {
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CompatibleJsonForm [status=" + status + ", message=" + message + "]";
    }

}
