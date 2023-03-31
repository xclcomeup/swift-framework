package com.liepin.swift.framework.mvc.filter.handler;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.filter.external.DefaultCharacterEncodingFilter;
import com.liepin.swift.framework.mvc.filter.external.ExternalFilter;
import com.liepin.swift.framework.mvc.filter.inner.CharacterEncodingFilter;
import com.liepin.swift.framework.mvc.filter.inner.HeaderFilter;
import com.liepin.swift.framework.mvc.filter.inner.IntranetAllowedFilter;
import com.liepin.swift.framework.mvc.filter.inner.MvcInputParamFilter;
import com.liepin.swift.framework.mvc.filter.inner.OriginalIpFilter;
import com.liepin.swift.framework.mvc.filter.inner.RpcInputParamFilter;
import com.liepin.swift.framework.mvc.filter.inner.XssFilter;
import com.liepin.swift.framework.mvc.impl.GWExceptionHandler;
import com.liepin.swift.framework.mvc.impl.GWPreprocessor;
import com.liepin.swift.framework.mvc.impl.RPCExceptionHandler;
import com.liepin.swift.framework.mvc.impl.RPCPreprocessor;
import com.liepin.swift.framework.mvc.resolver.IExceptionInterceptor;
import com.liepin.swift.framework.mvc.resolver.IExceptionResolver;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.filter.ExternalFilterPlugin;
import com.liepin.swift.framework.plugin.resolver.ExceptionResolverPlugin;
import com.liepin.swift.framework.plugin.resolver.ajax.ExceptionInterceptorPlugin;

public class FilterHandlerManager {

    private List<FilterHandler> filterHandlers = new ArrayList<FilterHandler>();
    private PageFilterHandler pageFilterHandler;

    public FilterHandlerManager() {

    }

    public void initFilterHandler() {
        // 初始内部过滤器
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        // FIXME SignFilter signFilter = new SignFilter();
        // OriginalIpFilter originalIpFilter = new OriginalIpFilter();
        // XssFilter xssFilter = new XssFilter();
        // HeaderFilter headerFilter = new HeaderFilter();

        // 加载异常分解器
        IExceptionResolver exceptionResolver = PluginContext.get().loadPlugin(ExceptionResolverPlugin.class)
                .getObject();
        // 加载异常拦截器
        IExceptionInterceptor exceptionInterceptor = PluginContext.get().loadPlugin(ExceptionInterceptorPlugin.class)
                .getObject();

        // 第一优先查找处理器：框架内部请求
        List<GenericFilter> innerFilterChains = new ArrayList<>();
        innerFilterChains.add(characterEncodingFilter);
        this.filterHandlers.add(new InnerFilterHandler(innerFilterChains));

        // 第二优先查找处理器：静态资源文件请求
        this.filterHandlers.add(new StaticResourceFilterHandler());

        // 第三优先查找处理器：RPC请求
        List<Filter> rpcFilterChains = new ArrayList<Filter>();// RPC过滤器,注意有先后顺序
        rpcFilterChains.add(characterEncodingFilter);
        rpcFilterChains.add(new RpcInputParamFilter());
        rpcFilterChains.add(new IntranetAllowedFilter());// 不放第一位是因为输出事件日志需要显示入参
        RpcFilterHandler rpcFilterHandler = new RpcFilterHandler(rpcFilterChains);
        rpcFilterHandler.setPreprocessor(new RPCPreprocessor()).setExceptionHandler(new RPCExceptionHandler())
                .setExceptionInterceptor(exceptionInterceptor);
        this.filterHandlers.add(rpcFilterHandler);

        // 第四优先查找处理器：GW请求
        List<Filter> gwFilterChains = new ArrayList<Filter>();// RPC过滤器,注意有先后顺序
        gwFilterChains.add(characterEncodingFilter);
        gwFilterChains.add(new XssFilter(true));// 只对尖括号转义，忽略双引号转义
        gwFilterChains.add(new RpcInputParamFilter());
        gwFilterChains.add(new IntranetAllowedFilter());// 不放第一位是因为输出事件日志需要显示入参
        GWFilterHandler gwFilterHandler = new GWFilterHandler(gwFilterChains);
        gwFilterHandler.setPreprocessor(new GWPreprocessor()).setExceptionHandler(new GWExceptionHandler())
                .setExceptionInterceptor(exceptionInterceptor);
        this.filterHandlers.add(gwFilterHandler);

        // 第五优先查找处理器：JSON请求
        // 读取外部自定义的过滤器
        List<GenericFilter> externalFilterChains = new ArrayList<GenericFilter>();
        List<ExternalFilter> externalFilters = PluginContext.get().loadPlugin(ExternalFilterPlugin.class).getObject();
        // 添加必备的过滤器，放头部
        externalFilterChains.add(characterEncodingFilter);
        externalFilterChains.add(new HeaderFilter());
        externalFilterChains.add(new XssFilter());
        externalFilterChains.add(new MvcInputParamFilter());
        externalFilterChains.add(new OriginalIpFilter());
        for (GenericFilter externalFilter : externalFilters) {
            if (DefaultCharacterEncodingFilter.class.isAssignableFrom(externalFilter.getClass())) {
                externalFilterChains.add(1, externalFilter);
            } else {
                externalFilterChains.add(externalFilter);
            }
        }
        JsonFilterHandler jsonFilterHandler = new JsonFilterHandler(externalFilterChains);
        jsonFilterHandler.setExceptionInterceptor(exceptionInterceptor).setExceptionResolver(exceptionResolver);
        this.filterHandlers.add(jsonFilterHandler);

        // AppFilterHandler appFilterHandler = new
        // AppFilterHandler(externalFilterChains);
        // appFilterHandler.setExceptionInterceptor(exceptionInterceptor);
        // this.filterHandlers.add(appFilterHandler);
        // this.filterHandlers.add(new WxaFilterHandler(externalFilterChains));

        // 兜底处理器：普通页面请求
        this.pageFilterHandler = new PageFilterHandler(externalFilterChains);
        this.pageFilterHandler.setExceptionResolver(exceptionResolver);
    }

    public FilterHandler getFilterHandler(final HttpServletRequest request) {
        for (FilterHandler fh : this.filterHandlers) {
            if (fh.supports(request)) {
                return fh;
            }
        }
        return pageFilterHandler;
    }

}
