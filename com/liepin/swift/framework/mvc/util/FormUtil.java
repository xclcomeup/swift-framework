package com.liepin.swift.framework.mvc.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class FormUtil {

    private static final Logger logger = Logger.getLogger(FormUtil.class);

    public static Map<String, Object> form2Map(HttpServletRequest req) {
        Set<Entry<String, String[]>> set = req.getParameterMap().entrySet();
        Map<String, Object> inputMap = new HashMap<String, Object>();
        for (Entry<String, String[]> entry : set) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            if (value == null || value.length == 0) {
                logger.debug("get(" + key + ") is null or empty.");
            } else {
                if (value.length == 1) {
                    inputMap.put(key, value[0]);
                } else {
                    inputMap.put(key, Arrays.asList(value));
                }
            }
        }
        return inputMap;
    }

}
