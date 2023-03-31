package com.liepin.swift.framework.rpc.router;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.router.Router;
import com.liepin.swift.core.annotation.Timeout;
import com.liepin.swift.framework.rpc.IRPCHandle;
import com.liepin.swift.framework.rpc.PlatformResult;
import com.liepin.swift.framework.util.UrlUtil;

/**
 * RPC ins-router实现
 * 
 * @author yuanxl
 * 
 */
public class RouterRPCHandle implements IRPCHandle {

    private Router router;
    @SuppressWarnings("unused")
    private String projectName;

    private Map<String, Router> proxyConfig = new HashMap<String, Router>();

    public RouterRPCHandle(String projectName) {
        this(projectName, null);
    }

    public RouterRPCHandle(String projectName, String clientVersion) {
        this.router = RouterHelper.getInstance(projectName, clientVersion);
        this.projectName = projectName;
        initProxy(projectName);
    }

    @SuppressWarnings("unused")
    private String getServiceName() {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        int len = stes.length;
        String className = this.getClass().getName();
        String result = null;
        for (int i = 2; i < len; i++) {
            StackTraceElement ste = stes[i];
            if (ste.getClassName().equals(className)) {
                result = className + ":" + ste.getMethodName();
            } else if (result != null) {
                return result;
            }
        }
        return result != null ? result : (className + ":unknown");
    }

    private String getServiceName(String uri) {
        String path = UrlUtil.compile(uri, UrlUtil.getNamespace4API());
        String[] array = UrlUtil.uncompile(path);
        return (array.length == 2) ? array[0] + ":" + array[1] : "unknow";
    }

    @Override
    public PlatformResult<?> invoke(String uri, LinkedHashMap<String, Object> data, Timeout timeout,
            Class<?>... returnClass) {
        Transaction t = Cat.newTransaction("Call", getServiceName(uri));
        if (data == null) {
            t.addData("null");
        } else {
            t.addData(data.toString());
        }
        PlatformResult<?> result = null;
        Router router = proxy(uri);
        try {
            t.setStatus(Message.SUCCESS);
            if (Timeout.NOTOVERTIME == timeout) {
                result = router.requestUnWithTimeout(uri, data, null, returnClass);
            } else {
                result = router.requestWithTimeout(uri, data, null, returnClass);
            }
            return result;
        } catch (RuntimeException e) {
            Cat.logError(e);
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

    @Override
    public PlatformResult<?> invokeCompress(String uri, LinkedHashMap<String, Object> data, Class<?>... returnClass) {
        Transaction t = Cat.newTransaction("Call", getServiceName(uri));
        PlatformResult<?> result = null;
        Router router = proxy(uri);
        try {
            t.setStatus(Message.SUCCESS);
            result = router.gzipRequest(uri, data, null, returnClass);
            return result;
        } catch (RuntimeException e) {
            Cat.logError(e);
            t.setStatus(e);
            throw e;
        } finally {
            t.complete();
        }
    }

    /**
     * 自身接口代理到其他项目功能
     * <p>
     * 老的配置方法：<br>
     * router ins-iks-platform autoloading {<br>
     * &nbsp;&nbsp;&nbsp; http 10.10.103.100:7250;<br>
     * &nbsp;&nbsp;&nbsp; proxy /RPC/IKwscanService/scanFast
     * ins-example-internal;<br>
     * }<br>
     * <p>
     * 暂时不支持，未来重新设计这块<br>
     * 
     * @param projectName
     */
    private void initProxy(String projectName) {
        // FIXME 暂时不支持，未来重新设计这块
        // Set<String> proxys = ClusterUtil.getClusters(projectName, "proxy");
        // for (String proxy : proxys) {
        // String[] array = proxy.split(" ");
        // proxyConfig.put(array[0], RouterHelper.getInstance(array[1]));
        // }
    }

    private Router proxy(String uri) {
        Router tmp = proxyConfig.get(uri);
        return (tmp == null) ? router : tmp;
    }

}
