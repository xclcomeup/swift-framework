package com.liepin.swift.framework.monitor.cat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.apache.log4j.Logger;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.util.PatternMatchUtils;

@SuppressWarnings("serial")
public class BeanClassAutoProxyCreator extends AbstractAutoProxyCreator {

    private static final Logger logger = Logger.getLogger(BeanClassAutoProxyCreator.class);

    private static final String AOP_NONE = "none";
    private static final String AOP_ORIGINAL = "original";
    private static final String AOP_SPRING = "spring";
    private static final String AOP_CGLIB = "cglib";

    private String[] classPatterns = new String[0];
    private String aopType = AOP_CGLIB;// AOP_SPRING;//AOP_CGLIB;
    private Advisor advisor;

    public BeanClassAutoProxyCreator() {
        // 默认 cglib
        setProxyTargetClass(true);
    }

    public void setAdvisor(Advisor advisor) {
        this.advisor = advisor;
    }

    @Override
    protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
        return new Advisor[] { GlobalAdvisorAdapterRegistry.getInstance().wrap(advisor) };
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
        // 如果不用AOP
        if (aopType == null || aopType.equals(AOP_NONE)) {
            return DO_NOT_PROXY;
        }
        if (classPatterns == null || classPatterns.length == 0) {
            return DO_NOT_PROXY;
        }
        if (PatternMatchUtils.simpleMatch(classPatterns, beanClass.getName())) {
            return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
        } else {
            return DO_NOT_PROXY;
        }
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        if (PatternMatchUtils.simpleMatch(classPatterns, beanClass.getName())) {
            return false;
        }
        return true;
    }

    protected Object createProxy(Class<?> beanClass, String beanName, Object[] specificInterceptors,
            TargetSource targetSource) {
        if (!aopType.equals(AOP_ORIGINAL)) {
            return super.createProxy(beanClass, beanName, specificInterceptors, targetSource);
        }
        ClassLoader loader = beanClass.getClassLoader();
        Class<?>[] interfaces = beanClass.getInterfaces();
        InvocationHandler handler = null;
        try {
            handler = new CatAdvice(targetSource.getTarget());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public String[] getClassPatterns() {
        return classPatterns;
    }

    public void setClassPatterns(String[] classPatterns) {
        this.classPatterns = classPatterns;
    }

    public String getAopType() {
        return aopType;
    }

    public void setAopType(String aopType) {
        this.aopType = aopType;
        // 设为spring 动态代理
        if (AOP_SPRING.equals(aopType)) {
            setProxyTargetClass(false);
        }
    }

}
