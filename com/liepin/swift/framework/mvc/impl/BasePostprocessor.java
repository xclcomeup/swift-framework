package com.liepin.swift.framework.mvc.impl;

import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.framework.mvc.filter.AbstractPostprocessor;

@Deprecated
public class BasePostprocessor extends AbstractPostprocessor {

    /**
     * must do try catch
     */
    @Override
    public void postprocess(HttpServletRequest req) {
        // 清理
        // 外面有 ThreadLocalUtil.getInstance().remove();

        // 已迁移 不打印biInfo和cat数据
        // Map<String, Object> attribute = (Map<String, Object>)
        // req.getAttribute(ResultStatus.INPUT);
        // if (attribute != null) {
        // attribute.remove(Const.BI_INFO);
        // attribute.remove(Const.CAT);
        // }
    }

}
