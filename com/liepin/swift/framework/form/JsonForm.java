package com.liepin.swift.framework.form;

/**
 * ajax、json返回数据结构
 * 
 * @author yuanxl
 * @date 2015-5-6 上午10:24:44
 */
public class JsonForm {

    public static final int FLAG_SUCCESS = 1;
    public static final int FLAG_FAIL = 0;

    private Object data;

    private int flag = FLAG_SUCCESS;

    private String code;

    private String msg;

    public JsonForm() {
    }

    public static JsonForm build() {
        return new JsonForm();
    }

    public Object getData() {
        return data;
    }

    public int getFlag() {
        return flag;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public JsonForm data(Object data) {
        setData(data);
        return this;
    }

    public JsonForm flag(int flag) {
        setFlag(flag);
        return this;
    }

    public JsonForm code(String code) {
        setCode(code);
        return this;
    }

    public JsonForm msg(String msg) {
        setMsg(msg);
        return this;
    }

    @Override
    public String toString() {
        return "JsonForm [data=" + data + ", flag=" + flag + ", code=" + code + ", msg=" + msg + "]";
    }

}
