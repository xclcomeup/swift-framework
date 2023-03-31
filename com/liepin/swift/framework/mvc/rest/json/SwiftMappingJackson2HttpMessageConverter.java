package com.liepin.swift.framework.mvc.rest.json;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.liepin.common.json.ExtendStdDateFormat;

/**
 * 生效，但会出现2个MappingJackson2HttpMessageConverter，自定义的优先级高
 * 
 * @author yuanxl
 *
 */
@Configuration
public class SwiftMappingJackson2HttpMessageConverter {

    @Bean
    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();

        // 去除null object
        // objectMapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);老代码
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 字段不存在时不报异常
        // objectMapper.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES,
        // false);老代码
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 升级1.8.8版本后timestamp类型必须format，而默认没有支持那么完美，故需要扩展一些时间格式
        // objectMapper.getDeserializationConfig().setDateFormat(ExtendStdDateFormat.instance);老代码

        objectMapper.setDateFormat(ExtendStdDateFormat.instance);

        // 避免触发的String.intern(),导致内存持续增加
        // objectMapper.getJsonFactory().disable(JsonParser.Feature.INTERN_FIELD_NAMES);老代码
        objectMapper.getFactory().disable(Feature.INTERN_FIELD_NAMES);

        jsonConverter.setObjectMapper(objectMapper);

        return jsonConverter;
    }

}
