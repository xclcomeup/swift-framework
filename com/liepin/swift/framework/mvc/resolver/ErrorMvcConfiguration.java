//package com.liepin.swift.framework.mvc.resolver;
//
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.boot.autoconfigure.web.ServerProperties;
//import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
//import org.springframework.boot.web.servlet.error.ErrorAttributes;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ErrorMvcConfiguration {
//
//    private final ServerProperties serverProperties;
//
//    public ErrorMvcConfiguration(ServerProperties serverProperties) {
//        this.serverProperties = serverProperties;
//    }
//
//    @Bean
//    public SwiftBasicErrorController errorController(ErrorAttributes errorAttributes,
//            ObjectProvider<ErrorViewResolver> errorViewResolvers) {
//        return new SwiftBasicErrorController(errorAttributes, serverProperties.getError(),
//                errorViewResolvers.orderedStream().collect(Collectors.toList()));
//    }
//
//}
