package com.liepin.swift.framework.dto;

import java.util.Collections;
import java.util.List;

/**
 * Td的含数据总数的PageDto，是 {@link TdBasePageDto} 的子类
 * 
 * @Project ins-td-platform
 * @Created 2015年12月24日
 * @Modified 2015年12月24日
 * @version 0.0.1
 * @author YangLijun yanglj@liepin.com
 * 
 */
public class TdCountPageDto<T> extends TdBasePageDto<T> {

    private static final long serialVersionUID = 2240181171897337494L;

    /** 数据总数 */
    private int totalCnt = 0;

    public TdCountPageDto() {
        super();
    }

    /**
     * @param datas
     * @throws NullPointerException
     *             datas为null时
     */
    protected TdCountPageDto(List<T> datas) {
        super(datas);
    }

    /**
     * @param datas
     * @param totalCnt
     * @throws NullPointerException
     *             datas为null时
     */
    protected TdCountPageDto(List<T> datas, int totalCnt) {
        super(datas);
        this.totalCnt = totalCnt;
    }

    /**
     * 根据datas和cnt创建实例
     * 
     * @param datas
     *            如果传入null，自动转化为 emptyList
     * @param totalCnt
     * @return
     */
    public static <T> TdCountPageDto<T> newInstance(List<T> datas, int totalCnt) {
        datas = null == datas ? Collections.<T> emptyList() : datas;
        return new TdCountPageDto<T>(datas, totalCnt);
    }

    /**
     * 
     * @param initialCapacity
     *            datas的初始化容量，在创建datas时初始化ArrayList的容量，在特定场景需要关注这个参数时可设置，默认为
     *            {@link #DEFAULT_CAPACITY}
     */
    public TdCountPageDto(int initialCapacity) {
        super(initialCapacity);
    }

    public int getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(int totalCnt) {
        this.totalCnt = totalCnt;
    }

    @Override
    public String toString() {
        return "TdCountPageDto [totalCnt=" + totalCnt + ", datas=" + datas + "]";
    }

}
