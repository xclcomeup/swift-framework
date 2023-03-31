package com.liepin.swift.framework.rpc.deserialize;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean.ParamBean;
import com.liepin.swift.framework.util.TypeUtil;

public class MethodValueDeserialize {

    private IDataDeserialize<String> dataDeserialize = new JsonDataDeserialize();

    private final boolean judgeSwitch = PropUtil.getInstance().getBoolean("interface.parameter.judge.switch", false);

    public Object[] fromJson(String json, LinkedHashMap<String, ParamBean> paramMap) throws BizException {
        Map<String, String> dataMap = JsonUtil.getRootJson(json);
        if (Objects.isNull(dataMap)) {
            throw new BizException(SystemEnum.PARAMETER_EXCEPTION);
        }
        Object[] argObjs = new Object[paramMap.size()];
        int i = 0;
        for (Map.Entry<String, ParamBean> entry : paramMap.entrySet()) {
            String argName = entry.getKey();
            ParamBean paramBean = entry.getValue();
            String valueJson = dataMap.get(argName);
            Object object = null;
            if (valueJson != null) {
                object = dataDeserialize.deserialize(valueJson, paramBean.parametrized, paramBean.parameterClasses);
                baseTypeOfNullableThrow(object, paramBean.parametrized);
            } else {
                if (judgeSwitch) {
                    // FIXME 结构优化
                    // 如果参数是非_开头的表示必填字段
                    if (!argName.startsWith("_")) {
                        throw new BizException(SystemEnum.LACK_REQUIRED_FIELD);
                    }
                }
            }
            argObjs[i++] = object;
        }
        return argObjs;
    }

    public Object[] fromMap(Map<String, Object> dataMap, LinkedHashMap<String, ParamBean> paramMap) {
        Object[] argObjs = new Object[paramMap.size()];
        int i = 0;
        for (Map.Entry<String, ParamBean> entry : paramMap.entrySet()) {
            String argName = entry.getKey();
            Object object = dataMap.get(argName);
            argObjs[i++] = object;
        }
        return argObjs;
    }

    public Object[] fromMapAndPackage(Map<String, Object> dataMap, LinkedHashMap<String, ParamBean> paramMap)
            throws Exception {
        if (paramMap.size() > 0 && dataMap == null) {
            throw new BizException(SystemEnum.INVALID);
        }
        Object[] argObjs = new Object[paramMap.size()];
        int i = 0;
        for (Map.Entry<String, ParamBean> entry : paramMap.entrySet()) {
            String argName = entry.getKey();
            ParamBean paramBean = entry.getValue();
            Object value = dataMap.get(argName);
            argObjs[i++] = TypeUtil.getObject(value, paramBean);
        }
        return argObjs;
    }

    private void baseTypeOfNullableThrow(Object value, Class<?> type) throws BizException {
        if (type == boolean.class || type == int.class || type == byte.class || type == short.class
                || type == long.class || type == float.class || type == double.class || type == char.class) {
            Optional.ofNullable(value).orElseThrow(() -> new BizException(SystemEnum.PARAMETER_EXCEPTION));
        }
    }

}
