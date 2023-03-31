package com.liepin.swift.framework.bundle.asyn;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liepin.swift.core.spring.bean.AsynServiceFactoryBean;

@Configuration
public class SwiftAsynServiceInitializer {

    @Bean(destroyMethod = "destroy")
    public AsynServiceFactoryBean asynService() throws Exception {
        return new AsynServiceFactoryBean();
    }

}
