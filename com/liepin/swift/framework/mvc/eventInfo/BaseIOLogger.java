package com.liepin.swift.framework.mvc.eventInfo;

public class BaseIOLogger extends AbstractIOLogger {

    @Override
    public String filter(String output) {
        return EscapeText.confuseChars(output);
    }

}
