package com.liepin.swift.framework.mvc.initializer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;

@Configuration
public class SwiftWebMvcConfigurerAdapter implements WebMvcConfigurer {

    private static final Logger logger = Logger.getLogger(SwiftWebMvcConfigurerAdapter.class);

    @Autowired(required = false)
    private List<WebArgumentResolver> webArgumentResolvers;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        logger.info("加载：" + (Objects.isNull(webArgumentResolvers) ? 0 : webArgumentResolvers.size())
                + " 个WebArgumentResolver");
        Optional.ofNullable(webArgumentResolvers).ifPresent(list -> {
            list.forEach((WebArgumentResolver t) -> {
                resolvers.add(new ServletWebArgumentResolverAdapter(t));
            });
        });
    }

}
