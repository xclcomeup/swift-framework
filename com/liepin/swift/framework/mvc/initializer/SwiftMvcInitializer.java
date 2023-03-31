package com.liepin.swift.framework.mvc.initializer;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.catalina.ssi.SSIServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;

import com.dianping.cat.servlet.CatFilter;
import com.liepin.swift.framework.mvc.dispatcher.SwiftDispatcherServlet;
import com.liepin.swift.framework.mvc.filter.SwiftFilter;
import com.liepin.swift.framework.mvc.http.HystrixMetricsStreamServletWrapper;
import com.liepin.swift.framework.mvc.upload.XssCommonsMultipartResolver;

@Configuration
public class SwiftMvcInitializer {

    //@Bean
//    public ServletListenerRegistrationBean<ServletContextListener> swiftContextLoaderListener(){
//        ServletListenerRegistrationBean<ServletContextListener> listenerBean = new ServletListenerRegistrationBean<>();
//        listenerBean.setListener(new SwiftContextEventStart());
//        listenerBean.setOrder(0);
//        return listenerBean;
//    }
    
//    @Bean
//    public InitParameterConfiguringServletContextInitializer initParamsInitializer() {
//        Map<String, String> contextParams = new HashMap<>();
//        contextParams.put("org.apache.myfaces.AUTO_SCROLL", "true");
//        return new InitParameterConfiguringServletContextInitializer(contextParams);
//    }
    
//    <!-- cat 监控 -->
//    <!-- 注：如果项目是对外不提供URL访问，比如GroupService，仅仅提供RPC服务，则不需要 -->
//    <filter>
//        <filter-name>cat-filter</filter-name>
//        <filter-class>com.dianping.cat.servlet.CatFilter</filter-class>
//        <init-param>
//            <param-name>exclude</param-name>
//            <param-value>/sse/dashboard.do</param-value>
//        </init-param>
//    </filter>
//    <filter-mapping>
//        <filter-name>cat-filter</filter-name>
//        <url-pattern>/*</url-pattern>
//        <dispatcher>REQUEST</dispatcher>
//        <dispatcher>FORWARD</dispatcher>
//    </filter-mapping>
    
    @Bean
    public FilterRegistrationBean<Filter> catFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<Filter>();
        registration.setFilter(new CatFilter());
        registration.addInitParameter("exclude", "/sse/dashboard.do");
        registration.addUrlPatterns("/*");
        registration.setName("catFiler");
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
        registration.setOrder(0);
        return registration;
    }
    
//    <filter>
//    <filter-name>swiftFilter</filter-name>
//    <filter-class>com.liepin.swift.framework.mvc.filter.SwiftFilter</filter-class>
//<!--        <init-param>
//        对以下指定后缀参数进行排除过滤，默认自带排除静态资源后缀 
//        <param-name>exclude</param-name>
//        <param-value>.shtml;.html;.htm;.css;.js;.gif;.png;.jpeg;.jpg;.bmp;.ico;.txt;.xml</param-value>
//    </init-param> -->
//</filter>
//
//<filter-mapping>
//    <filter-name>swiftFilter</filter-name>
//    <url-pattern>/*</url-pattern>
//    <dispatcher>REQUEST</dispatcher>
//    <dispatcher>FORWARD</dispatcher>
//</filter-mapping>
    
    @Bean
    public FilterRegistrationBean<Filter> swiftFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<Filter>();
        registration.setFilter(new SwiftFilter());
        registration.addUrlPatterns("/*");
        registration.setName("swiftFiler");
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
        registration.setOrder(1);
        return registration;
    }
    
//    <bean id="multipartResolver"
//            class="com.liepin.swift.framework.mvc.upload.XssCommonsMultipartResolver">
//            <property name="defaultEncoding" value="utf-8"></property>
//            <!-- 默认无限制上传文件大小，可以从config.properties里配置${maxUploadSize} 修改 -->
//            <property name="maxInMemorySize" value="1024000"></property>
//        </bean>
    
    @Bean
    //@Order(1)
    public MultipartResolver multipartResolver() {
        XssCommonsMultipartResolver multipartResolver = new XssCommonsMultipartResolver();
        multipartResolver.setDefaultEncoding("utf-8");
        multipartResolver.setMaxInMemorySize(1024000);
        // 默认无限制上传文件大小，可以从config.properties里配置${maxUploadSize} 修改
        // multipartResolver.setMaxUploadSize(maxUploadSize);
        return multipartResolver;
    }
    
    // TODO SwiftDispatcherServlet
//    <servlet>
//    <servlet-name>swiftDispatcher</servlet-name>
//    <servlet-class>com.liepin.swift.framework.mvc.dispatcher.SwiftDispatcherServlet</servlet-class>
//    <load-on-startup>1</load-on-startup>
//    <init-param>
//        <param-name>contextConfigLocation</param-name>
//        <param-value>classpath*:spring-mvc.xml</param-value>
//    </init-param>
//</servlet>
//
//<servlet-mapping>
//    <servlet-name>swiftDispatcher</servlet-name>
//    <url-pattern>/</url-pattern>
//</servlet-mapping>
    
//        @Autowired
//        private ApplicationContext applicationContext;
    
      @Bean
      public ServletRegistrationBean<Servlet> swiftDispatcherServletRegistration() {
          //注解扫描上下文
          //AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
          //base package
          //applicationContext.scan("com.liepin");
          //通过构造函数指定dispatcherServlet的上下文
          //DispatcherServlet rest_dispatcherServlet = new DispatcherServlet(applicationContext);
          
          SwiftDispatcherServlet swiftDispatcherServlet = new SwiftDispatcherServlet();
          //swiftDispatcherServlet.setApplicationContext(applicationContext);
          
          ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<Servlet>();
          registration.setServlet(swiftDispatcherServlet);
          registration.addInitParameter("contextConfigLocation", "classpath*:spring-mvc.xml");
          registration.setLoadOnStartup(1);
          registration.addUrlMappings("/");
          registration.setName("dispatcherServlet");
          return registration;
      }
    
//    <servlet>
//    <description></description>
//    <display-name>HystrixMetricsStreamServlet</display-name>
//    <servlet-name>HystrixMetricsStreamServlet</servlet-name>
//    <servlet-class>com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet</servlet-class>
//    <load-on-startup>1</load-on-startup>
//</servlet>
//<servlet-mapping>
//    <servlet-name>HystrixMetricsStreamServlet</servlet-name>
//    <url-pattern>/hystrix.stream</url-pattern>
//</servlet-mapping>
    
    @Bean
    public ServletRegistrationBean<Servlet> hystrixMetricsStreamServletRegistration() {
        ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<Servlet>();
        registration.setName("hystrixMetricsStreamServlet");
        registration.setServlet(new HystrixMetricsStreamServletWrapper());
        registration.setLoadOnStartup(1);
        registration.addUrlMappings("/hystrix.stream");
        return registration; 
    }
    
    @ConditionalOnProperty(name = "servlet.ssi.enable", havingValue = "true")
    @Bean
    public ServletRegistrationBean<Servlet> ssiServletRegistration() {
        ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<Servlet>();
        registration.setName("ssiServlet");
        registration.setServlet(new SSIServlet());
        registration.addInitParameter("buffered", "true");
        registration.addInitParameter("debug", "0");
        registration.addInitParameter("expires", "666");
        registration.addInitParameter("isVirtualWebappRelative", "false");
        registration.setLoadOnStartup(4);
        registration.addUrlMappings("*.shtml");
        return registration;
    }
    
}
