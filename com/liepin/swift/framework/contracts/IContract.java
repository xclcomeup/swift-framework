package com.liepin.swift.framework.contracts;

import com.liepin.swift.core.exception.SysException;

public interface IContract {

    /**
     * 检查
     * <p>
     * 
     * @throw RuntimeException
     */
    public void review() throws SysException;

}
