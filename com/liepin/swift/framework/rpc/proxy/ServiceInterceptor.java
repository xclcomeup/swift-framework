package com.liepin.swift.framework.rpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.common.conf.ProjectIdMap;
import com.liepin.router.ServiceInitializedException;
import com.liepin.router.loadbalance.ServerDisabledException;
import com.liepin.swift.core.bean.RpcContext;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.boot.SwiftApplicationContext;
import com.liepin.swift.framework.monitor.call.RepeatCallCollecter;
import com.liepin.swift.framework.monitor.cross.CallFailureCollecter;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherBean;
import com.liepin.swift.framework.rpc.IRPCHandle;
import com.liepin.swift.framework.rpc.PlatformResult;
import com.liepin.swift.framework.rpc.ServiceMetadata;
import com.liepin.swift.framework.rpc.limit.FallbackManager;
import com.liepin.swift.framework.rpc.limit.HystrixConfigurationCurator;
import com.liepin.swift.framework.util.CommonUtil;
import com.liepin.swift.framework.util.RpcProtocol.ErrorType;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType;

/**
 * Service层代理
 * 
 * @author yuanxl
 * 
 */
public class ServiceInterceptor implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(ServiceInterceptor.class);

    private IRPCHandle rpcHandle;

    private ServiceMetadata serviceMetadata;

    private static volatile boolean onceHappen = false;

    /**
     * 禁止外部使用，仅内部异步代理使用
     */
    public ServiceInterceptor() {
    }

    public ServiceInterceptor(ServiceMetadata serviceMetadata, IRPCHandle rpcHandle) {
        this.serviceMetadata = serviceMetadata;
        this.rpcHandle = rpcHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 服务启动过程中服务调用检查
        // only once
        checkStartupServiceCall();

        // rpc起始时间埋点
        RpcContext.current().launch();
        TraceIdUtil.nextTraceId();

        HystrixCommandCallWrapper hystrixCommandCallWrapper = null;
        String uri = serviceMetadata.getSimpleUri(method);
        DispatcherBean fallbackService = FallbackManager.getFallbackService(uri);
        if (fallbackService != null) {
            hystrixCommandCallWrapper = new HystrixCommandCallAndFallbackWrapper(method, args,
                    ThreadLocalUtil.getInstance().getAll(), fallbackService);
        } else {
            hystrixCommandCallWrapper = new HystrixCommandCallWrapper(method, args);
        }
        Transaction t = Cat.newTransaction("Hystrix", uri);
        Object execute = null;
        SysException sysException = null;
        try {
            execute = hystrixCommandCallWrapper.execute();
            t.setStatus(Message.SUCCESS);
        } catch (HystrixRuntimeException e) {
            // 无fallback时抛，java.lang.UnsupportedOperationException: No fallback
            // available.

            // fallback逻辑异常
            if (fallbackService != null && e.getFallbackException() instanceof SysException) {
                sysException = (SysException) e.getFallbackException();// 抛fallback逻辑代码异常
                log(uri, args, sysException);
                t.setStatus(sysException);
                Cat.logError(sysException);
                throw sysException;
            }

            t.setStatus(e.getCause());
            Cat.logError(e.getCause());

            if (FailureType.TIMEOUT == e.getFailureType()) {
                sysException = new SysException(SystemEnum.SERVICE_TIMEOUT, e.getCause());
                log(uri, args, sysException);
                throw sysException;
            }
            if (FailureType.SHORTCIRCUIT == e.getFailureType()) {
                sysException = new SysException(SystemEnum.SERVICE_SHORTCIRCUIT, e.getCause());
                log(uri, args, sysException);
                throw sysException;
            }
            if (FailureType.REJECTED_SEMAPHORE_EXECUTION == e.getFailureType()
                    || FailureType.REJECTED_SEMAPHORE_FALLBACK == e.getFailureType()) {
                sysException = new SysException(SystemEnum.SERVICE_REJECTED, e.getCause());
                log(uri, args, sysException);
                throw sysException;
            }
            if (FailureType.COMMAND_EXCEPTION == e.getFailureType()) {
                if (e.getCause() instanceof SysException) {
                    // 系统异常 SysException
                    sysException = (SysException) e.getCause();
                    log(uri, args, sysException);
                    throw sysException;
                } else if (e.getCause() instanceof ServerDisabledException) {
                    // 服务不可用异常
                    ServerDisabledException serverDisabledException = (ServerDisabledException) e.getCause();
                    // sysException = new
                    // SysException(SystemEnum.SERVICE_UNAVAILABLE,
                    // serverDisabledException);
                    sysException = new SysException(SystemEnum.SERVICE_UNAVAILABLE.code(),
                            serverDisabledException.getMessage(), serverDisabledException);
                    log(uri, args, SystemEnum.SERVICE_UNAVAILABLE.code(), SystemEnum.SERVICE_UNAVAILABLE.message(),
                            serverDisabledException);
                    throw sysException;
                } else {
                    // no even happen
                    throw sysException = new SysException(e.getCause());
                }
            }
            // 观察什么情况会抛，伏笔
            sysException = new SysException(SystemEnum.UNKNOWN.code(),
                    "服务限流降级熔断处理中未捕获的异常：" + e.getFailureType() + ", " + e.getMessage(), e.getCause());
            log(uri, args, sysException);
            throw sysException;
        } catch (HystrixBadRequestException e) {
            if (e.getCause() instanceof BizException) {
                // 业务异常 BizException
                log(uri, args, (BizException) e.getCause());
                t.setStatus(Message.SUCCESS);
                throw e.getCause();
            } else if (e.getCause() instanceof ServiceInitializedException) {
                // 启动或停止异常
                t.setStatus(e.getCause());
                Cat.logError(e.getCause());
                SysException sysException2 = new SysException(SystemEnum.SERVICE_INITIAL_END, e.getCause());
                log(uri, args, sysException2);
                throw sysException2;
            }
            // no even happen
            throw e.getCause();
        } finally {
            // 调用非业务异常记录
            if (sysException != null) {
                CallFailureCollecter.getInstance().record(ProjectIdMap.clientId(serviceMetadata.getProjectName()),
                        RpcContext.current().getIp(), sysException.code(), RpcContext.current().getTimePlan());
            }
            t.complete();
        }
        return execute;
    }

    private class HystrixCommandCallAndFallbackWrapper extends HystrixCommandCallWrapper {

        private DispatcherBean fallbackService;
        private Map<String, Object> threadLocalContext;

        public HystrixCommandCallAndFallbackWrapper(Method method, Object[] args,
                Map<String, Object> threadLocalContext, DispatcherBean fallbackService) {
            super(method, args);
            this.fallbackService = fallbackService;
            this.threadLocalContext = threadLocalContext;
        }

        @Override
        protected Object getFallback() {
            String uri = serviceMetadata.getSimpleUri(method);
            Optional.ofNullable(threadLocalContext).ifPresent(ThreadLocalUtil.getInstance()::set);
            Transaction t = Cat.newTransaction("Hystrix.Fallback", uri);
            Object ret = null;
            try {
                ret = fallbackService.invoke(args);
                t.setStatus(Message.SUCCESS);
            } catch (Throwable e) {
                t.setStatus(e);
                Cat.logError(e);
                throw new SysException(SystemEnum.SERVICE_FALLBACK_FAIL, e);
            } finally {
                t.complete();
            }
            Optional.ofNullable(threadLocalContext).ifPresent(x -> ThreadLocalUtil.getInstance().remove());
            return ret;
        }

    }

    private class HystrixCommandCallWrapper extends HystrixCommand<Object> {

        protected Method method;
        protected Object[] args;

        public HystrixCommandCallWrapper(Method method, Object[] args) {
            super(HystrixConfigurationCurator.getInstance().getClientHystrixCommandSetter(
                    serviceMetadata.getProjectName(), serviceMetadata.getHystrixCommandKey(method, args),
                    serviceMetadata.isTimeout(method)));
            this.method = method;
            this.args = args;
        }

        @Override
        protected Object run() throws Exception {
            // 经过Hystrix后节点
            RpcContext.current().record();

            // 获取接口uri地址
            String uri = null;
            // 包装接口请求参数
            LinkedHashMap<String, Object> data = null;
            // Date time = new Date();
            try {
                uri = serviceMetadata.getUri(method);
                data = serviceMetadata.packaging(method, args);

                // 获取接口返回类class, 顺序outputDataClass, elementClass
                PlatformResult<?> pr;
                if (serviceMetadata.isCompress(method)) {
                    pr = rpcHandle.invokeCompress(uri, data, serviceMetadata.getReturnClass(method));
                } else {
                    // 超时控制
                    pr = rpcHandle.invoke(uri, data, serviceMetadata.getTimeout(method),
                            serviceMetadata.getReturnClass(method));
                }
                // 正常返回结果
                if (pr.isOK()) {
                    return pr.getData();
                }

                // 如果有异常往外抛
                if (Objects.nonNull(pr.getErrorType())) {
                    if (ErrorType.biz == pr.getErrorType()) {
                        throw new BizException(pr.getBizCode(), pr.getMessage());
                    } else {
                        throw new SysException(pr.getCode(), pr.getMessage(), pr.getThrowable());
                    }
                } else {
                    // 兼容
                    if (BizException.isBizException(pr.getCode())) {
                        BizException bizException = new BizException(pr.getCode(), pr.getMessage());
                        bizException.recover();
                        throw bizException;
                    } else {
                        throw new SysException(pr.getCode(), pr.getMessage(), pr.getThrowable());
                    }
                }
            } catch (Throwable t) {
                if (t instanceof BizException) {
                    // 包装成HystrixBadRequestException异常，不触发熔断降级
                    throw new HystrixBadRequestException("BizException2HystrixBadRequestException", t);
                } else if (t instanceof SysException) {
                    throw (SysException) t;
                } else if (t instanceof ServerDisabledException) {
                    throw (ServerDisabledException) t;
                } else if (t instanceof ServiceInitializedException) {
                    throw new HystrixBadRequestException("ServiceInitializedException2HystrixBadRequestException", t);
                } else {
                    throw new SysException(SystemEnum.UNKNOWN, t);
                }
            } finally {
                // 埋点统计重复调用
                // 只统计雨燕项目
                // if (ConfUtil.isSwiftProject()) {
                RepeatCallCollecter.getInstance().collect(serviceMetadata.getProjectName(), uri, data);
                // }
                // cross统计
                // CrossCollecter.getInstance().call(serviceMetadata.getProjectName(),
                // time);
            }
        }

    }

    @Override
    public String toString() {
        return serviceMetadata.getProjectName() + ":" + serviceMetadata.getServiceName();
    }

    private void log(String url, Object[] args, BizException bizException) {
        StringBuilder builder = new StringBuilder();
        builder.append("call app=").append(serviceMetadata.getProjectName());
        builder.append(", ip=").append(RpcContext.current().getIp());
        builder.append(", url=").append(url);
        builder.append(", code=").append(bizException.code());
        builder.append(", message=").append(bizException.message());
        builder.append(", time=").append(RpcContext.current().opTime()).append("ms");
        builder.append(", param=").append(CommonUtil.intercept(Arrays.toString(args), 100));
        builder.append("\n");
        builder.append(BizException.class.getName()).append(": ").append(bizException.getMessage());
        builder.append(getStackTraceMgr(10));
        MonitorLogger.getInstance().log(builder.toString());
    }

    private void log(String url, Object[] args, SysException sysException) {
        if (SystemEnum.UNKNOWN.code().equals(sysException.code())
                || SystemEnum.SERVICE_FALLBACK_FAIL.code().equals(sysException.code())) {
            log1(url, args, sysException);
        } else if (SystemEnum.SERVICE_UNAVAILABLE.code().equals(sysException.code())) {
            log(url, args, SystemEnum.SERVICE_UNAVAILABLE.code(), SystemEnum.SERVICE_UNAVAILABLE.message(),
                    sysException);
        } else {
            log(url, args, sysException.code(), sysException.message(), sysException);
        }
    }

    private void log(String url, Object[] args, String code, String message, Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append("call app=").append(serviceMetadata.getProjectName());
        builder.append(", ip=").append(RpcContext.current().getIp());
        builder.append(", url=").append(url);
        builder.append(", traceId=").append(TraceIdUtil.getTraceId());
        builder.append(", code=").append(code);
        builder.append(", message=").append(message);
        builder.append(", time=").append(RpcContext.current().opTime()).append("ms");
        builder.append(", param=").append(CommonUtil.intercept(Arrays.toString(args), 100));
        builder.append("\n");
        builder.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage());
        builder.append(getStackTraceMgr(Integer.MAX_VALUE));
        logger.error(builder.toString());
    }

    private void log1(String url, Object[] args, SysException sysException) {
        StringBuilder builder = new StringBuilder();
        builder.append("call app=").append(serviceMetadata.getProjectName());
        builder.append(", ip=").append(RpcContext.current().getIp());
        builder.append(", url=").append(url);
        builder.append(", traceId=").append(TraceIdUtil.getTraceId());
        builder.append(", code=").append(sysException.code());
        builder.append(", message=").append(sysException.message());
        builder.append(", time=").append(RpcContext.current().opTime()).append("ms");
        builder.append(", param=").append(CommonUtil.intercept(Arrays.toString(args), 100));
        logger.error(builder.toString(), sysException);
    }

    private String getStackTraceMgr(int lineCnt) {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        StackTraceElement ste;
        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < stes.length; i++) {
            ste = stes[i];
            if (ste.getLineNumber() <= 0) {
                continue;
            }
            if (!ste.getClassName().startsWith("com.liepin")) {
                continue;
            }
            if (ste.getClassName().startsWith("com.liepin.swift.") || ste.getClassName().startsWith("com.liepin.cache.")
                    || ste.getClassName().startsWith("com.liepin.dao.")
                    || ste.getClassName().startsWith("com.liepin.router.")) {
                continue;
            }
            sb.append("\n\tat ");
            sb.append(ste.getClassName());
            sb.append(".");
            sb.append(ste.getMethodName());
            sb.append("(");
            sb.append(ste.getFileName());
            sb.append(":");
            sb.append(ste.getLineNumber());
            sb.append(")");
            if (cnt++ >= lineCnt) {
                break;
            }
        }
        return sb.toString();
    }

    private void checkStartupServiceCall() {
        if (onceHappen == false) {
            onceHappen = true;
            if (!SwiftApplicationContext.initialized()) {
                reveal();
            }
        }
    }

    private void reveal() {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        StackTraceElement ste;
        StringBuilder log = new StringBuilder("Illegal behavior：启动过程中发现服务调用@\"" + toString() + "\"，调用链如下: \n");
        for (int i = 0; i < stes.length; i++) {
            ste = stes[i];
            if (ste.getLineNumber() <= 0) {
                continue;
            }
            if (!ste.getClassName().startsWith("com.liepin")) {
                continue;
            }
            if (ste.getClassName().startsWith("com.liepin.swift.") || ste.getClassName().startsWith("com.liepin.cache.")
                    || ste.getClassName().startsWith("com.liepin.dao.")
                    || ste.getClassName().startsWith("com.liepin.router.")) {
                continue;
            }
            log.append("         at ");
            log.append(ste.getClassName());
            log.append(".");
            log.append(ste.getMethodName());
            log.append("(");
            log.append(ste.getFileName());
            log.append(":");
            log.append(ste.getLineNumber());
            log.append(")");
            log.append("\r\n");
        }
        logger.warn(log.toString());
    }

}
