package com.liepin.swift.framework.mvc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;

public class WebApplicationContextHolder {

    private static WebApplicationContext applicationContext = null;

    public static void setApplicationContext(WebApplicationContext applicationContext) {
        WebApplicationContextHolder.applicationContext = applicationContext;
    }

    public static WebApplicationContext getWebApplicationContext() {
        return applicationContext;
    }

    /***
     * 根据一个bean的id获取配置文件中相应的bean
     * 
     * @param beanName
     * @return
     * @throws BeansException
     */
    public static Object getBean(String beanName) throws BeansException {
        return applicationContext.getBean(beanName);
    }

    /***
     * 类似于getBean(String beanName)只是在参数中提供了需要返回到的类型。
     * 
     * @param beanName
     * @param requiredType
     * @return
     * @throws BeansException
     */
    public static <T> T getBean(String beanName, Class<T> requiredType) throws BeansException {
        return (T) applicationContext.getBean(beanName, requiredType);
    }

    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return (T) applicationContext.getBean(requiredType);
    }

    /**
     * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
     * 
     * @param beanName
     * @return boolean
     */
    public static boolean containsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    /**
     * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。
     * 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
     * 
     * @param beanName
     * @return boolean
     * @throws NoSuchBeanDefinitionException
     */
    public static boolean isSingleton(String beanName) throws NoSuchBeanDefinitionException {
        return applicationContext.isSingleton(beanName);
    }

    /**
     * @param beanName
     * @return Class 注册对象的类型
     * @throws NoSuchBeanDefinitionException
     */
    @SuppressWarnings("rawtypes")
    public static Class getType(String beanName) throws NoSuchBeanDefinitionException {
        return applicationContext.getType(beanName);
    }

    /**
     * 如果给定的bean名字在bean定义中有别名，则返回这些别名
     * 
     * @param beanName
     * @return
     * @throws NoSuchBeanDefinitionException
     */
    public static String[] getAliases(String beanName) throws NoSuchBeanDefinitionException {
        return applicationContext.getAliases(beanName);
    }

}
