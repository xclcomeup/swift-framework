package com.liepin.swift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ImportResource;

import com.liepin.swift.core.spring.context.SwiftAnnotationConfigServletWebServerApplicationContext;
import com.liepin.swift.framework.boot.listener.initializer.ApplicationListenerInitializer;
import com.liepin.swift.framework.log.StartLog;
import com.liepin.swift.framework.log.initializer.SwiftLogInitializer;


// @EnableAutoConfiguration
// @Configuration
// @ComponentScan
// @EnableWebMvc
@ServletComponentScan(basePackages = "com.liepin.swift")
@SpringBootApplication(scanBasePackages = { "com.liepin.swift", // for 框架
        "com.liepin.**.controller", // for 业务接入
        "com.liepin.**.service", // for 业务接入
        "com.liepin.**.gateway", // for 业务接入
        "com.liepin.**.biz", // for 业务接入
        "com.liepin.**.dao", // for 业务接入
        "com.liepin.**.idp", // for 业务接入
        "com.liepin.**.schedule", // for 业务接入
        "com.liepin.**.filter", // for 业务接入
        "com.liepin.**.resolver", // for 业务接入
        "com.liepin.**.listener", // for 业务接入
        "com.liepin.**.fallback", // for 业务接入
        "com.liepin.**.inject", // for 业务接入
        "com.liepin.**.support.**" // for 业务扩展bean接入
}, exclude = { DataSourceAutoConfiguration.class, MongoAutoConfiguration.class, QuartzAutoConfiguration.class,
        SolrAutoConfiguration.class, FreeMarkerAutoConfiguration.class, MultipartAutoConfiguration.class,
        RestClientAutoConfiguration.class })
@ImportResource({ "classpath:springcustom-*.xml" }) // 暂时兼容（走注解方式）
public class SwiftApplication {

    public static void main(String[] args) {
        try {
            StartLog.reset();
            SpringApplication springApplication = new SpringApplication(SwiftApplication.class);
            springApplication.addListeners(ApplicationListenerInitializer.initialize());
            springApplication.setApplicationContextClass(SwiftAnnotationConfigServletWebServerApplicationContext.class);
            springApplication.run(args);
        } catch (Throwable e) {
            StartLog.err(SwiftLogInitializer.printFailMessage() + "\n" + e);
            throw e;
        }
    }

}
