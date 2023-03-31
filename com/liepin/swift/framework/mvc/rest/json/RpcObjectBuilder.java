package com.liepin.swift.framework.mvc.rest.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.liepin.common.json.JsonUtil;
import com.liepin.swift.framework.mvc.ResultStatus;

public class RpcObjectBuilder {

    public static String response(ResultStatus rs) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(ResultStatus.status(), rs.getStatus());
        map.put(ResultStatus.message(), rs.getMessage());
        map.put(ResultStatus.data(), rs.getData() != null ? rs.getData() : Collections.emptyMap());
        return JsonUtil.toJson(map);
    }

}
