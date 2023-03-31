package com.liepin.swift.framework.form;

public class JsonFailForm extends JsonForm {

    public JsonFailForm() {
        setFail();
    }
    
    public void setFail() {
        setFlag(FLAG_FAIL);
    }

}
