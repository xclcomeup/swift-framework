package com.liepin.swift.framework.rpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.liepin.common.asyn4j.core.Asyn4jContext;
import com.liepin.common.asyn4j.face.AsynService;
import com.liepin.common.asyn4j.face.AsynServiceImpl;
import com.liepin.common.conf.PropUtil;
import com.liepin.swift.core.spring.bean.DefaultThreadLocalHandler;

public class AsynServiceInterceptor implements InvocationHandler {

    private AsynService asynService;
    private ServiceInterceptor serviceInterceptor;
    private ServiceInterceptor asynServiceInterceptor;

    public AsynServiceInterceptor(ServiceInterceptor serviceInterceptor) {
        this.serviceInterceptor = serviceInterceptor;
        // 启动异步方法服务
        int queueCapacity = PropUtil.getInstance().getInt("rpc.asyn.queueCapacity", 100000);
        long addJobWaitTime = PropUtil.getInstance().getLong("rpc.asyn.addJobWaitTime", 2L);
        int jobThreadNum = PropUtil.getInstance().getInt("rpc.asyn.jobThreadNum", 10);
        this.asynService = AsynServiceImpl.newInstance("RPC-ASYN", queueCapacity, addJobWaitTime, jobThreadNum,
                Asyn4jContext.CALLBACK_THREAD_NUM, Asyn4jContext.CLOSE_SERVICE_WAITTIME);
        ((Asyn4jContext) asynService).setThreadLocalHandler(new DefaultThreadLocalHandler());
        this.asynService.init();
        this.asynServiceInterceptor = (ServiceInterceptor) asynService.getProxy(serviceInterceptor);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        asynServiceInterceptor.invoke(proxy, method, args);
        return null;
    }

    @Override
    public String toString() {
        return "asyn|" + serviceInterceptor.toString();
    }

}
