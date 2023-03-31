package com.liepin.swift.framework.monitor.cat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.swift.core.exception.BizException;

public class CatAdvice implements MethodInterceptor, InvocationHandler {

    public CatAdvice() {
        // 目前使用的是MethodInterceptor
    }

    /********************** MethodInterceptor ****************************/
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String className = invocation.getThis().getClass().getName();
        String methodName = invocation.getMethod().getName();
        Transaction t = Cat.newTransaction("Biz", className + "." + methodName);
        Object result = null;
        try {
            result = invocation.proceed();// 必须调用此方法，否则后续处理终断
            t.setStatus(Message.SUCCESS);
        } catch (BizException e) {
            t.setStatus(Message.SUCCESS); // BizException不加到cat中
            throw e;
        } catch (Throwable e) {
            Cat.logError(e);
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }

        return result;
    }

    /********************* InvocationHandler **********************/
    // 被代理类的实例
    Object obj = null;

    // 将被代理者的实例传进动态代理类的构造函数中
    public CatAdvice(Object obj) {
        this.obj = obj;
    }

    /**
     * 覆盖InvocationHandler接口中的invoke()方法
     * 
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String className = obj.getClass().getName();
        String methodName = method.getName();
        Transaction t = Cat.newTransaction("Biz", className + "." + methodName);
        Object result = null;
        try {
            result = method.invoke(this.obj, args);
            t.setStatus(Message.SUCCESS);
        } catch (Throwable e) {
            // BizException不加到cat中
            if (e instanceof BizException) {
                t.setStatus(Message.SUCCESS);
            } else {
                Cat.logError(e);
                t.setStatus(e);
            }
            throw e;
        } finally {
            t.complete();
        }
        return result;
    }

}
