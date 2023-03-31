package com.liepin.swift.framework.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.liepin.swift.framework.dto.BaseDto;

/**
 * Td的分页dto基类，这个类是抽象类，里面包含了最基本的存储数据的datas列表，并含基本的方法
 * 
 * @Project ins-td-platform
 * @Created 2015年12月24日
 * @Modified 2015年12月24日
 * @version 0.0.1
 * @author YangLijun yanglj@liepin.com
 * 
 */
public abstract class TdBasePageDto<T> extends BaseDto {

    private static final long serialVersionUID = 3006812052312191957L;
    
    /**
     * Default initial capacity. {@value #DEFAULT_CAPACITY}
     */
    private static final int DEFAULT_CAPACITY = 10;
    
    private final int initialCapacity;
    
    /** 数据列表 */
    protected List<T> datas = Collections.emptyList();

    public TdBasePageDto() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @param datas
     * @throws NullPointerException datas为null时
     */
    public TdBasePageDto(List<T> datas) {
        this(DEFAULT_CAPACITY);
        this.setDatas(datas);
    }
    
    /**
     * 
     * @param initialCapacity datas的初始化容量，在创建datas时初始化ArrayList的容量，在特定场景需要关注这个参数时可设置，默认为 {@link #DEFAULT_CAPACITY}
     */
    public TdBasePageDto(int initialCapacity) {
        super();
        this.initialCapacity = initialCapacity;
    }

    public List<T> getDatas() {
        return datas;
    }

    /**
     * 
     * @param datas
     * @throws NullPointerException
     *             datas为null时
     */
    public void setDatas(List<T> datas) {
        if (datas == null) {
            throw new NullPointerException();
        }
        this.datas = datas;
    }

    /**
     * 方便的为datas添加data的方法
     * @param data 添加的data，不可以为null
     * @return
     * @throws NullPointerException data为null时
     */
    public TdBasePageDto<T> addData(T data) {
        if (data == null) {
            throw new NullPointerException();
        }
        List<T> list = this.datas;
        if(Collections.emptyList() == list) {
            list = new ArrayList<T>(initialCapacity);
            this.datas = list;
        }
        list.add(data);
        return this;
    }
    
    public static final <T> TdNextPageDto<T> createEmptyNextPageDto() {
        return new TdNextPageDto<T>();
    }
    
    public static final <T> TdCountPageDto<T> createEmptyCountPageDto() {
        return new TdCountPageDto<T>();
    }
    

    @Override
    public String toString() {
        return "TdBasePageDto [datas=" + datas + "]";
    }
    
}
