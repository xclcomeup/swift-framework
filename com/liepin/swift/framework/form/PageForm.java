package com.liepin.swift.framework.form;

import java.util.List;

/**
 * 包含页面的翻信息和当前列表的组合form。一般给ajax请求时作为返回值用。
 * 
 * @author yangxf
 * 
 * @param T
 *            表示翻页数据的类型,T范型不能继承IEntity和BaseDto
 */
@SuppressWarnings("serial")
public final class PageForm<T> extends BaseForm {

	private List<T> list;

	private int totalCount;

	private int crrentPage;

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public int getCrrentPage() {
		return crrentPage;
	}

	public void setCrrentPage(int crrentPage) {
		this.crrentPage = crrentPage;
	}

}
