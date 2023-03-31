//package com.liepin.swift.framework.test;
//
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.junit.runner.RunWith;
//import org.springframework.aop.framework.AdvisedSupport;
//import org.springframework.aop.framework.AopProxy;
//import org.springframework.aop.support.AopUtils;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import com.liepin.swift.core.util.SpringContextUtil;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath*:applicationContext.xml" })
//public abstract class BaseUnit {
//
//    /**
//     * 使用Mock对象替换真实对象
//     * 
//     * @param targetClass 测试接口
//     * @param mockObject mock对象
//     */
//    protected void useMock(Class<?> targetClass, Object mockObject) {
//        Object targetBean = null;
//        try {
//            targetBean = SpringContextUtil.getBean(targetClass);
//        } catch (Exception e) {
//            throw new RuntimeException("no bean found class=" + targetClass, e);
//        }
//
//        try {
//            if (AopUtils.isAopProxy(targetBean)) {
//                if (AopUtils.isCglibProxy(targetBean)) {
//                    targetBean = getCglibProxyTargetObject(targetBean);
//                } else if (AopUtils.isJdkDynamicProxy(targetBean)) {
//                    targetBean = getJdkDynamicProxyTargetObject(targetBean);
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("get proxy target fail", e);
//        }
//
//        List<Field> fields = new ArrayList<Field>();
//        Class<?> superClazz = targetBean.getClass();
//        while (superClazz != null && superClazz != Object.class) {
//            fields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
//            superClazz = superClazz.getSuperclass();
//        }
//        for (Field field : fields) {
//            if (field.getType() == mockObject.getClass() || field.getType().isAssignableFrom(mockObject.getClass())) {
//                field.setAccessible(true);
//                try {
//                    field.set(targetBean, mockObject);
//                } catch (Exception e) {
//                    throw new RuntimeException("set mock object fail", e);
//                }
//            }
//        }
//    }
//
//    private Object getCglibProxyTargetObject(Object proxy) throws Exception {
//        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
//        h.setAccessible(true);
//        Object dynamicAdvisedInterceptor = h.get(proxy);
//        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
//        advised.setAccessible(true);
//        return ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
//    }
//
//    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
//        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
//        h.setAccessible(true);
//        AopProxy aopProxy = (AopProxy) h.get(proxy);
//        Field advised = aopProxy.getClass().getDeclaredField("advised");
//        advised.setAccessible(true);
//        return ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
//    }
//
//}
