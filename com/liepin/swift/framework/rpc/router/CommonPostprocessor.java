package com.liepin.swift.framework.rpc.router;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.type.TypeFactory;

import com.liepin.common.json.JsonUtil;
import com.liepin.router.handler.AbstractPostprocessor;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.rpc.PlatformResult;
import com.liepin.swift.framework.util.RpcProtocol;

public class CommonPostprocessor extends AbstractPostprocessor<PlatformResult<?>> {

    private static final Logger logger = Logger.getLogger(CommonPostprocessor.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    public CommonPostprocessor() {
        objectMapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        objectMapper.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.getJsonFactory().disable(JsonParser.Feature.INTERN_FIELD_NAMES);// 避免触发的String.intern(),
                                                                                     // 导致内存持续增加
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public PlatformResult<?> postprocess(String json, Map<String, String> headers, Class<?>... returnClass) {
        if (!StringUtils.isNotBlank(json)) {
            return new PlatformResult(SystemEnum.INVALID.code(), "httpclient post result is error, json is null", null);
        }

        Map<String, String> map = JsonUtil.getRootJson(json);
        if (map == null) {
            return new PlatformResult(SystemEnum.INVALID.code(), "httpclient post map is null json:" + json, null);
        }

        String status = map.get(ResultStatus.status());
        String message = map.get(ResultStatus.message());
        String data = map.get(ResultStatus.data());

        PlatformResult result = new PlatformResult();
        // 兼容老框架
        result.setCode((status != null) ? status : SystemEnum.OK.code());
        result.setMessage(message);
        result.setErrorType(RpcProtocol.getErrorTypeHeader(headers));
        result.setBizCode(RpcProtocol.getBizCodeHeader(headers));

        if ("{}".equals(data)) {
            return result;
        }

        Class outputDataClass = returnClass[0];
        if (outputDataClass.equals(String.class)) {
            result.setData(data);
        } else {
            Object dataValue = null;
            try {
                if (outputDataClass.isEnum()) {
                    dataValue = this.objectMapper.readValue("\"" + data + "\"", outputDataClass);
                    if (dataValue == null) {
                        logger.error("class=" + outputDataClass + ", 请求=" + json + ", transaform fail");
                    }
                } else {
                    if (returnClass.length == 1)
                        dataValue = this.objectMapper.readValue(data, outputDataClass);
                    else {
                        Class[] paraCalss = new Class[returnClass.length - 1];
                        System.arraycopy(returnClass, 1, paraCalss, 0, returnClass.length - 1);
                        dataValue = this.objectMapper.readValue(data,
                                TypeFactory.parametricType(outputDataClass, paraCalss));
                    }
                }
                result.setData(dataValue);
            } catch (Exception e) {
                return new PlatformResult(SystemEnum.INVALID.code(), e.getMessage(), null);
            }
        }
        return result;
    }

}
