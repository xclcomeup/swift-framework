package com.liepin.swift.framework.rpc.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SwiftServiceAutowiredProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<? extends Object> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SwiftServiceAutowired.class) && field.getType().isInterface()) {
                if (Modifier.isPrivate(field.getModifiers())) {
                    field.setAccessible(true);
                }
                try {
                    Object fieldObj = field.get(bean);
                    if (fieldObj == null) {
                        fieldObj = RPCFactory.getService(field.getType());
                        field.set(bean, fieldObj);
                    }
                } catch (Throwable e) {
                    throw new RuntimeException("beanName=" + beanName + ", field=" + field.getName() + "|"
                            + field.getType() + " Autowired注解方式注入RPC接口失败: " + e.getMessage(), e);
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
