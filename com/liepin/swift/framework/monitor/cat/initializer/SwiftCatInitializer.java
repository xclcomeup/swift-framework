package com.liepin.swift.framework.monitor.cat.initializer;

import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liepin.swift.framework.monitor.cat.BeanClassAutoProxyCreator;
import com.liepin.swift.framework.monitor.cat.CatAdvice;

@Configuration
public class SwiftCatInitializer {
    
    /**=================== AOP CAT 拦截 ===================**/     
//    <bean id="catAdvice" class="com.liepin.swift.framework.monitor.cat.CatAdvice" />
//     
//    <bean id="catAdvisor" class="org.springframework.aop.support.DefaultPointcutAdvisor">
//        <property name="advice" ref="catAdvice" />
//    </bean>
//
//    <bean id="catAutoProxy"
//        class="com.liepin.swift.framework.monitor.cat.BeanClassAutoProxyCreator">
//        <property name="classPatterns">
//            <list>
//                <value>com.liepin.*.service.*</value>
//                <value>com.liepin.*.biz.*</value>
//            </list>
//        </property>
//        <property name="interceptorNames">
//            <list>
//                <value>catAdvisor</value>
//            </list>
//        </property>
//    </bean>
     
    @Bean
    public BeanClassAutoProxyCreator catAutoProxy() {
        CatAdvice catAdvice = new CatAdvice();

        DefaultPointcutAdvisor catAdvisor = new DefaultPointcutAdvisor();
        catAdvisor.setAdvice(catAdvice);

        BeanClassAutoProxyCreator catAutoProxy = new BeanClassAutoProxyCreator();
        catAutoProxy.setClassPatterns(new String[] { "com.liepin.*.service.*", "com.liepin.*.biz.*" });
        // catAutoProxy.setInterceptorNames("catAdvisor");
        catAutoProxy.setAdvisor(catAdvisor);
        return catAutoProxy;
    }
    
}
