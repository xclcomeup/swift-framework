package com.liepin.swift.framework.rpc.deserialize;

import com.liepin.common.json.JsonUtil;


public class JsonDataDeserialize implements IDataDeserialize<String> {

    @Override
    public Object deserialize(String t, Class<?> parametrized, Class<?>... parameterClasses) {
        return JsonUtil.json2objectDepth(t, parametrized, parameterClasses);
    }

}
