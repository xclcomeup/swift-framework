package com.liepin.swift.framework.dto;

import java.util.List;

/**
 * T 范型不能继承IEntity
 * 
 * @author yuanxl
 * @date 2015-5-8 上午10:27:38
 * @param <T>
 */
public class QueryResultsDto<T> {

    private List<T> list;

    private long totalCount;

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> ts) {
        this.list = ts;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

}
