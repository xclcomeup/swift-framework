package com.liepin.swift.framework.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootContextLoader;

import com.liepin.swift.SwiftApplication;
import com.liepin.swift.core.spring.context.SwiftAnnotationConfigServletWebServerApplicationContext;
import com.liepin.swift.framework.boot.listener.initializer.ApplicationListenerInitializer;
import com.liepin.swift.framework.log.initializer.SwiftLogInitializer;

public class SwiftApplicationUnit extends SpringBootContextLoader {

    @Override
    protected SpringApplication getSpringApplication() {
        try {
            SpringApplication springApplication = new SpringApplication(SwiftApplication.class);
            springApplication.addListeners(ApplicationListenerInitializer.initialize());
            springApplication.setApplicationContextClass(SwiftAnnotationConfigServletWebServerApplicationContext.class);
            return springApplication;
        } catch (Throwable e) {
            System.out.println(SwiftLogInitializer.printFailMessage() + "\n" + e);
            throw e;
        }
    }

}
