package com.liepin.swift.framework.plugin.resolver;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.framework.mvc.resolver.DefaultExceptionResolver;
import com.liepin.swift.framework.mvc.resolver.IExceptionResolver;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class ExceptionResolverPlugin implements IPlugin<IExceptionResolver> {

    private static final Logger logger = Logger.getLogger(ExceptionResolverPlugin.class);

    private IExceptionResolver exceptionResolver;

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ExceptionResolverPlugin init.");
        StringBuilder log = new StringBuilder();
        List<Object> list = new PluginScan<>(applicationContext).scanObjects(new ExceptionResolverObjectFilter());
        if (list.isEmpty()) {
            this.exceptionResolver = new DefaultExceptionResolver();
        } else {
            if (list.size() != 1) {
                throw new RuntimeException("创建了多个IExceptionResolver");
            }
            this.exceptionResolver = (IExceptionResolver) list.get(0);
        }
        log.append("Added {" + exceptionResolver.getClass().getName()).append("} to IExceptionResolver\n");
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        logger.info("ExceptionResolverPlugin destroy.");
    }

    @Override
    public IExceptionResolver getObject() {
        return this.exceptionResolver;
    }

    @Override
    public String name() {
        return "扫描异常分解器";
    }

}
