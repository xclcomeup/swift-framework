package com.liepin.swift.framework.limit;

import com.liepin.swift.core.util.ThreadLocalUtil;

public class LimitContextUtil {

    public static LimitContext create() {
        LimitContext context = new LimitContext();
        context.setUrl(ThreadLocalUtil.getInstance().getCurrentUrl());
        context.setOriginalIp(ThreadLocalUtil.getInstance().getOriginalIP());
        context.setCurrentUserId(ThreadLocalUtil.getInstance().getCurrentUserId());
        context.setInitClientId(ThreadLocalUtil.getInstance().getInitClientId());
        context.setLastClientId(ThreadLocalUtil.getInstance().getLastClientId());
        return context;
    }

}
