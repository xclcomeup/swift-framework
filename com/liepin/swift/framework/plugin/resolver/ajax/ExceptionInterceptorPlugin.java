package com.liepin.swift.framework.plugin.resolver.ajax;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.framework.mvc.resolver.IExceptionInterceptor;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class ExceptionInterceptorPlugin implements IPlugin<IExceptionInterceptor> {

    private static final Logger logger = Logger.getLogger(ExceptionInterceptorPlugin.class);

    private IExceptionInterceptor exceptionInterceptor;

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ExceptionInterceptorPlugin init.");
        StringBuilder log = new StringBuilder();
        List<IExceptionInterceptor> list = new PluginScan<IExceptionInterceptor>(applicationContext)
                .scanObjects(new ExceptionInterceptorObjectFilter());
        if (list.isEmpty()) {
            return;
        }
        this.exceptionInterceptor = list.get(0);
        if (list.size() > 1) {
            logger.warn(
                    "配置了多个ExceptionInterceptor: " + list + ", 注意仅只加载一个: " + exceptionInterceptor.getClass().getName());
        }
        log.append("Added {" + exceptionInterceptor.getClass().getName()).append("} to ExceptionInterceptor\n");
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        logger.info("ExceptionInterceptorPlugin destroy.");
    }

    @Override
    public IExceptionInterceptor getObject() {
        return this.exceptionInterceptor;
    }

    @Override
    public String name() {
        return "请求异常拦截器加载";
    }

}
