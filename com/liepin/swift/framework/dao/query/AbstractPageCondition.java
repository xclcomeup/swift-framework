package com.liepin.swift.framework.dao.query;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class AbstractPageCondition implements Serializable {

    protected int curPage = 0; // 当前页面

    protected int pageSize = 15; // 每页总条数

    protected int startPosition = 0; // 起始数据

    protected int maxRows = pageSize; // 最大数据

    protected long totalRows = 0; // 总记录数

    protected boolean isPaged = true; // 是否进行分页

    protected boolean hasCount = false;

    public static final int MAX_PAGE_SIZE = 10000;

    public void calculate() {
        if (isPaged) {
            startPosition = curPage == 0 ? startPosition = 0 : (curPage) * pageSize;
            maxRows = pageSize;
        } else {
            maxRows = MAX_PAGE_SIZE;
        }
    }

    public int getCurPage() {
        return curPage;
    }

    public void setCurPage(int curPage) {
        this.curPage = curPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public boolean isPaged() {
        return isPaged;
    }

    public void setPaged(boolean isPaged) {
        this.isPaged = isPaged;
    }

    public boolean isHasCount() {
        return hasCount;
    }

    public void setHasCount(boolean hasCount) {
        this.hasCount = hasCount;
    }

}
