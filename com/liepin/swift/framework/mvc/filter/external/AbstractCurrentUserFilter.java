package com.liepin.swift.framework.mvc.filter.external;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.util.ThreadLocalUtil;

/**
 * 获取当前访问者user_id和user_kind的继承拦截器
 * 
 * @author yuanxl
 * 
 */
public abstract class AbstractCurrentUserFilter extends ExternalFilter {

    @SuppressWarnings("deprecation")
    @Override
    public boolean external(final HttpServletRequest request, final HttpServletResponse response) throws BizException {
        Optional.ofNullable(getCurrentUserProperty(request, response)).ifPresent(t -> {
            ThreadLocalUtil.getInstance().setCurrentUserId(t.getCurrentUserId());
        });
        return true;
    }

    /**
     * 获取当前访问者用户id
     * 
     * @param request
     * @return
     */
    public abstract CurrentProperty getCurrentUserProperty(final HttpServletRequest request,
            final HttpServletResponse response) throws BizException;

    public static final class CurrentProperty {
        private String currentUserId;
        @Deprecated
        private String currentUserkind;

        public CurrentProperty(String currentUserId, @Deprecated String currentUserkind) {
            this.currentUserId = currentUserId;
            this.currentUserkind = currentUserkind;
        }

        public String getCurrentUserId() {
            return currentUserId;
        }

        /**
         * 注意：即将下线
         * 
         * @return
         */
        @Deprecated
        public String getCurrentUserkind() {
            return currentUserkind;
        }

    }

}
