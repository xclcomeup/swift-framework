package com.liepin.swift.framework.mvc.rest.json;

import java.util.Collections;
import java.util.Optional;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.framework.form.JsonFailForm;
import com.liepin.swift.framework.form.JsonForm;

public class AjaxObjectBuilder {

    public static JsonForm toSuccess(Object obj) {
        return (obj instanceof JsonForm) ? (JsonForm) obj
                : JsonForm.build().data(Optional.ofNullable(obj).orElse((Object) Collections.emptyMap()));
    }

    public static JsonFailForm toFail(Throwable actual) {
        JsonFailForm form = new JsonFailForm();
        fillFail(form, (actual instanceof IMessageCode) ? (IMessageCode) actual : SystemEnum.UNKNOWN);
        return form;
    }

    public static JsonFailForm toFail(SystemEnum systemEnum) {
        JsonFailForm jsonForm = new JsonFailForm();
        jsonForm.setCode(systemEnum.code());
        jsonForm.setMsg(systemEnum.message());
        return jsonForm;
    }

    private static void fillFail(final JsonForm jsonForm, IMessageCode messageCode) {
        jsonForm.code(messageCode.code()).msg((SystemEnum.UNKNOWN.code().equals(messageCode.code()))
                ? SystemEnum.UNKNOWN.message() : messageCode.message());
    }

}
