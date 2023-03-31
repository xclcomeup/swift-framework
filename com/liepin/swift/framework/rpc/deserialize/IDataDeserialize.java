package com.liepin.swift.framework.rpc.deserialize;

public interface IDataDeserialize<T> {

    /**
     * 使用场景
     * <p>
     * 序列化数据 反序列化为 Service接口方法型参值<br>
     * 序列化数据 反序列化为 Service接口方法返回值<br>
     * 
     * @param t
     * @param parametrized
     * @param parameterClasses
     * @return
     */
    public Object deserialize(T t, Class<?> parametrized, Class<?>... parameterClasses);

}
