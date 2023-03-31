package com.liepin.swift.framework.mvc.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

@Deprecated
public class JsonUtil {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.getSerializationConfig().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        OBJECT_MAPPER.getJsonFactory().disable(JsonParser.Feature.INTERN_FIELD_NAMES);
    }

    public static Map<String, Object> inputStream2Map(InputStream inputStream) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(inputStream);
        Map<String, Object> inputMap = new HashMap<String, Object>();
        Iterator<String> fieldNames = node.getFieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode child = node.get(fieldName);
            if (child.isValueNode()) {
                inputMap.put(fieldName, child.getValueAsText());
            } else {
                inputMap.put(fieldName, child);
            }
        }
        return inputMap;
    }

}
