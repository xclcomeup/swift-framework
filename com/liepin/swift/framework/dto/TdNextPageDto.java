package com.liepin.swift.framework.dto;

import java.util.List;

/**
 * Td的含是否有下一页的PageDto，是 {@link TdBasePageDto} 的子类
 * 
 * @Project ins-td-platform
 * @Created 2015年12月24日
 * @Modified 2015年12月24日
 * @version 0.0.1
 * @author YangLijun yanglj@liepin.com
 * 
 */
public class TdNextPageDto<T> extends TdBasePageDto<T> {

    private static final long serialVersionUID = 9091671978854694690L;

    /** 是否有下一页 */
    private boolean hasNextPage = false;

    /** 列表展示样参数，特定业务场景，需要单独设置 */
    private String st;

    public TdNextPageDto() {
        super();
    }

    /**
     * @param datas
     * @param hasNextPage
     * @throws NullPointerException
     *             datas为null时
     */
    protected TdNextPageDto(List<T> datas, boolean hasNextPage) {
        super(datas);
        this.hasNextPage = hasNextPage;
    }

    /**
     * 
     * @param initialCapacity
     *            datas的初始化容量，在创建datas时初始化ArrayList的容量， 在特定场景需要关注这个参数时可设置，默认为
     *            {@link #DEFAULT_CAPACITY}
     */
    public TdNextPageDto(int initialCapacity) {
        super(initialCapacity);
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    /**
     * 创建基本的dto
     * 
     * @param datas
     * @param hasNextPage
     * @return
     */
    public static <T> TdNextPageDto<T> newInstance(List<T> datas, boolean hasNextPage) {
        return new TdNextPageDto<>(datas, hasNextPage);
    }

    /**
     * 创建NextPageDto，在上一个平台会返回totalCnt时使用
     * 
     * @param datas
     *            数据
     * @param currentPage
     *            当前页码
     * @param pageSize
     *            单页条数
     * @param totalCnt
     *            总记录数
     * @return
     */
    public static <T> TdNextPageDto<T> createDtoByTotalCnt(List<T> datas, int currentPage, int pageSize, int totalCnt) {
        boolean hasNext = pageSize * (currentPage + 1) < totalCnt;
        return new TdNextPageDto<>(datas, hasNext);
    }

    /**
     * 此方法是在无法获取或者获取总数代价很高时，没有总数时，计算是否有下一页，原理是 datas.size >= pageSize 则认为有下一页，
     * 在极端情况下，如最后一页刚好是 pageSize 条，则会多导致多拉取一次数据
     * 
     * @param datas
     * @param pageSize
     */
    public static <T> TdNextPageDto<T> createDtoWhenNoTotalCnt(List<T> datas, int pageSize) {
        return new TdNextPageDto<>(datas, datas.size() > pageSize);
    }

    /**
     * 这个方法基于，datas 总会带有一个尾巴 tail，即，原始 datas 是获取的条数的基础上多取一条，即取 pageSize+1 条，
     * 此方法会做两步操作 1. 截取正确的分页数量 pageSize 条记录 2. 计算出是否有下一页（如果size>pageSize)
     * 
     * @param datas
     * @param pageSize
     * @return
     */
    public static <T> TdNextPageDto<T> createDtoByTailDatas(List<T> datas, int pageSize) {
        boolean hasNext = datas.size() > pageSize;
        if (hasNext) {
            datas = datas.subList(0, pageSize);
        }
        return new TdNextPageDto<>(datas, hasNext);
    }

    public static <T> TdNextPageDto<T> createDtoByTailDatas(List<T> datas, boolean hasNext) {
        return new TdNextPageDto<>(datas, hasNext);
    }

    /**
     * 此方法是在无法获取或者获取总数代价很高时，没有总数时，计算是否有下一页， 与
     * {@link #createDtoWhenNoTotalCnt(List, int)} 的区别是，本方法认为传入的 datas
     * 在非最后一页也可能返回 datas.size < pageSize 的情况， 典型场景是，数据在传入前，进行过 filter
     * 操作，但是没有进行数据补齐导致，使用此方法的副作用就是，大部分场景下，分页拉取都会多取一次， 并且无法区分过滤至空的场景
     * 
     * @param datas
     * @return
     */
    public static <T> TdNextPageDto<T> createDtoEndByEmptyDatas(List<T> datas) {
        return new TdNextPageDto<T>(datas, !datas.isEmpty());
    }

    @Override
    public String toString() {
        return "TdNextPageDto [hasNextPage=" + hasNextPage + ", datas=" + datas + "]";
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

}
