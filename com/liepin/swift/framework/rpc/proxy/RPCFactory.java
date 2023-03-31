package com.liepin.swift.framework.rpc.proxy;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.liepin.swift.framework.rpc.IRPCHandle;
import com.liepin.swift.framework.rpc.ServiceMetadata;
import com.liepin.swift.framework.rpc.ServiceMetadataManager;
import com.liepin.swift.framework.rpc.cache.RpcCache;
import com.liepin.swift.framework.rpc.limit.FallbackManager;

/**
 * 接口代理工场
 * 
 * @author yuanxl
 * 
 */
public class RPCFactory {

    private static final Map<Class<?>, Object> SERVICE_PROXY_CACHE = new HashMap<Class<?>, Object>();

    private static final Map<Class<?>, Object> ASYN_SERVICE_PROXY_CACHE = new HashMap<Class<?>, Object>();

    private static byte[] lock = new byte[0];

    static {
        // 预加载
        ServiceMetadataManager.getInstance().load();
        // FIXME 初始化抛错捕获，然后再抛
    }

    public static void initialize() {
        // nothing
    }

    /**
     * 获取接口代理类
     * 
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(final Class<T> clazz) {
        Object proxy = SERVICE_PROXY_CACHE.get(clazz);
        if (proxy == null) {
            synchronized (lock) {
                proxy = SERVICE_PROXY_CACHE.get(clazz);
                if (proxy == null) {
                    SERVICE_PROXY_CACHE.put(clazz, proxy = newProxy(clazz, false));
                }
            }
        }
        return (T) proxy;
    }

    /**
     * 设置自定义service处理类
     * <p>
     * 使用场景：Mock测试
     * 
     * @param <T>
     * @param clazz
     * @param t
     */
    public static <T> void setService(final Class<T> clazz, final T t) {
        SERVICE_PROXY_CACHE.put(clazz, t);
    }

    /**
     * 重置自定义service处理类
     * <p>
     * 使用场景：Mock测试恢复
     * 
     * @param clazz
     */
    public static void resetService(final Class<?> clazz) {
        SERVICE_PROXY_CACHE.remove(clazz);
    }

    /**
     * 获取异步接口代理类
     * 
     * @param <T>
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAsynService(final Class<T> clazz) {
        Object proxy = ASYN_SERVICE_PROXY_CACHE.get(clazz);
        if (proxy == null) {
            synchronized (lock) {
                proxy = ASYN_SERVICE_PROXY_CACHE.get(clazz);
                if (proxy == null) {
                    ASYN_SERVICE_PROXY_CACHE.put(clazz, proxy = newProxy(clazz, true));
                }
            }
        }
        return (T) proxy;
    }

    /**
     * 注入RPC接口所对应的降级对象
     * 
     * @param fallbackService
     */
    public static void addFallbackService(final Object fallbackService) {
        FallbackManager.addFallbackService(fallbackService);
    }

    private static Object newProxy(final Class<?> clazz, boolean isAsyn) {
        ServiceMetadata serviceMetadata = ServiceMetadataManager.getInstance().getServiceMetadata(clazz);
        if (serviceMetadata == null) {
            throw new RuntimeException("RPC工厂加载" + clazz.getName() + "类的接口信息失败, 因为类所在的客户端名称不标准或接口没有@SwiftService注解!");
        }
        IRPCHandle rpcHandle = ServiceMetadataManager.getInstance().getRPCHandle(clazz);
        if (rpcHandle == null) {
            throw new RuntimeException("RPC工厂加载" + clazz.getName() + "类的RPC包装失败, 因为类所在的客户端名称不标准或接口没有@SwiftService注解!");
        }
        ServiceInterceptor serviceInterceptor = new ServiceInterceptor(serviceMetadata, rpcHandle);
        RpcCache rpcCache = ServiceMetadataManager.getInstance().getClentCache(clazz);
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                (isAsyn) ? (new AsynServiceInterceptor(serviceInterceptor)) : ((rpcCache != null)
                        ? new CacheServiceInterceptor(serviceInterceptor, rpcCache) : serviceInterceptor));
    }

}
