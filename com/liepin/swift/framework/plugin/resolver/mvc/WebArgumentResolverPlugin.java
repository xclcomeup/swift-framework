package com.liepin.swift.framework.plugin.resolver.mvc;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.support.WebArgumentResolver;

import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.plugin.IPlugin;

/**
 * 已迁移 {@link SwiftWebMvcConfigurerAdapter}
 * @author yuanxl
 *
 */
@Deprecated
public class WebArgumentResolverPlugin implements IPlugin<WebArgumentResolver> {

    private static final Logger logger = Logger.getLogger(WebArgumentResolverPlugin.class);

    private WebArgumentResolverClassScan webArgumentResolverClassScan;

    private WebArgumentResolver WebArgumentResolver;

    public WebArgumentResolverPlugin() {
        this.webArgumentResolverClassScan = new WebArgumentResolverClassScan();
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        StringBuilder log = new StringBuilder();
        List<Class<?>> list = webArgumentResolverClassScan.scan();
        if (list.size() > 0) {
            Class<?> clazz = list.get(0);
            this.WebArgumentResolver = (WebArgumentResolver) SpringContextUtil.getBean(clazz);
            log.append("Added {" + clazz.getName()).append("} to webArgumentResolver\n");
        }
        logger.info("WebArgumentResolverPlugin init.");
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        logger.info("WebArgumentResolverPlugin destroy.");
    }

    @Override
    public WebArgumentResolver getObject() {
        return this.WebArgumentResolver;
    }

    @Override
    public String name() {
        return null;
    }

}
