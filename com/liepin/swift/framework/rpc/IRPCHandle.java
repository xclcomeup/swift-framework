package com.liepin.swift.framework.rpc;

import java.util.LinkedHashMap;

import com.liepin.swift.core.annotation.Timeout;

/**
 * RPC接口
 * 
 * @author yuanxl
 * 
 */
public interface IRPCHandle {

    /**
     * 请求
     * 
     * @param uri
     * @param data
     * @param timeout
     * @param returnClass
     * @return
     */
    public PlatformResult<?> invoke(String uri, LinkedHashMap<String, Object> data, Timeout timeout,
            Class<?>... returnClass);

    /**
     * 压缩请求
     * 
     * @param uri
     * @param data
     * @param returnClass
     * @return
     */
    public PlatformResult<?> invokeCompress(String uri, LinkedHashMap<String, Object> data, Class<?>... returnClass);

}
