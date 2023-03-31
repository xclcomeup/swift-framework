package com.liepin.swift.framework.bundle.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liepin.cache.redis.conf.ConfShardRedisCacheClientBean;
import com.liepin.cache.spy.conf.ConfMemcachedClientSpyBean;

@Configuration
public class SwiftCacheInitializer {

    // <bean id="redisShardClient"
    // class="com.liepin.cache.redis.conf.ConfShardRedisCacheClientBean"
    // destroy-method="destroy">
    // </bean>

    @Bean(destroyMethod = "destroy")
    public ConfShardRedisCacheClientBean redisShardClient() {
        return new ConfShardRedisCacheClientBean();
    }


    // <bean id="memcachedClient"
    // class="com.liepin.cache.spy.conf.ConfMemcachedClientSpyBean"
    // destroy-method="destroy">
    // </bean>

    @Bean(destroyMethod = "destroy")
    public ConfMemcachedClientSpyBean memcachedClient() {
        return new ConfMemcachedClientSpyBean();
    }

}
