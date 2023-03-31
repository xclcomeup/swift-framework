package com.liepin.swift.framework.plugin.dao.jpa;

import com.liepin.dao.jpa.repository.SwiftJpaRepository;
import com.liepin.swift.framework.plugin.IClassFilter;

public class JpaRepositoryClassFilter implements IClassFilter {

    @Override
    public boolean test(Class<?> clazz) {
        return clazz.isInterface() && SwiftJpaRepository.class.isAssignableFrom(clazz);
    }

    @Override
    public String path() {
        return "com.liepin.**.dao.**";
    }

    @Override
    public boolean isContainJar() {
        return false;
    }

}
