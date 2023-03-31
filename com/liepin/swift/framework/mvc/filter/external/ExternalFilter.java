package com.liepin.swift.framework.mvc.filter.external;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;

import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.framework.mvc.filter.GenericFilter;

/**
 * 外部扩展过滤器
 * <p>
 * 注意过滤器优先级：框架默认的2个过滤器CatFilter、SwiftFilter分别是0，1，不要配置比这优先级要高<br>
 * 
 * @author yuanxl
 * 
 */
public abstract class ExternalFilter extends GenericFilter implements Ordered {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            if (external(request, response)) {
                filterChain.doFilter(request, response);
            }
        } finally {
            windup(request, response);
        }
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

    /**
     * 外部执行接口
     * 
     * @param request
     * @param response
     * @return 控制是否跳出过滤 true:继续链式执行 | false:跳出链式执行
     */
    public abstract boolean external(final HttpServletRequest request, final HttpServletResponse response)
            throws BizException;

    /**
     * 收尾方法，必要时实现类扩展
     * 
     * @param request
     * @param response
     */
    protected void windup(final HttpServletRequest request, final HttpServletResponse response) throws BizException {
        // For subclasses: do nothing by default.
    }

    /**
     * 外部扩展过滤器执行优先级
     * <p>
     * 数字越小优先级越高<br>
     * 备注：框架默认的2个过滤器CatFilter、SwiftFilter分别是0，1，不要配置比这优先级要高<br>
     * <p>
     * 预留拦截器执行顺序的扩展方法，默认无序
     * 
     * @return
     */
    public int priority() {
        return 0;
    }

    /**
     * 保持同步
     */
    @Override
    public int getOrder() {
        return priority();
    }

}
