package com.liepin.swift.framework.plugin.controller;

import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import com.liepin.swift.framework.plugin.IObjectFilter;
import com.liepin.swift.framework.util.ObjectUtil;

public class ControllerObjectFilter implements IObjectFilter {

    @Override
    public boolean test(Object o) {
        Object actual = ObjectUtil.getActual(o);
        return Objects.nonNull(actual.getClass().getAnnotation(Controller.class))
                || Objects.nonNull(actual.getClass().getAnnotation(RestController.class));
    }

}
