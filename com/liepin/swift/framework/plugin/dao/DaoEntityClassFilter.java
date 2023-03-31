package com.liepin.swift.framework.plugin.dao;

import com.liepin.dao.entity.BaseEntity;
import com.liepin.dao.entity.IEntity;
import com.liepin.swift.framework.plugin.IClassFilter;

public class DaoEntityClassFilter implements IClassFilter {

    @Override
    public boolean test(Class<?> clazz) {
        if (!IEntity.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (BaseEntity.class == clazz) {
            return false;
        }
        return true;
    }
    
    @Override
    public String path() {
        return "com.liepin.**.dao.**.entity.**";
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

}
