package com.liepin.swift.framework.mvc.eventInfo;

import java.util.Date;
import java.util.Map;

public interface Event {

    void setType(String value);

    String getType();// 请求类型

    void setName(String value);// 请求名称

    String getName();

    void setStatus(String value);

    String getStatus();

    void setInput(Map<String, Object> input);

    void setInput(String value);

    String getInput();

    void setOutput(String value);

    String getOutput();

    void begin();

    Date getStart();

    long getEclipse();

    void submit();// 结束

    String getActionPath();

    void setActionPath(String value);

    String getClientIP();

    void setClientIP(String value);

}
