package com.liepin.swift.framework.rpc.cache;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.liepin.common.collections.LRUSoftCache;
import com.liepin.common.conf.ProjectId;
import com.liepin.common.datastructure.ThreadLocalRandom;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class RpcCache {

    private static final int DEFAULT_CAPACITY = 1024 * 16 * 10;
    private final Map<Method, LRUSoftCache<String, Object>> cache = new HashMap<>();

    private static int capacity;

    static {
        capacity = readCapacity();
    }

    public RpcCache(final String serviceName) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                StringBuilder sb = new StringBuilder("RPCCache " + serviceName + " info list: ");
                for (Map.Entry<Method, LRUSoftCache<String, Object>> entry : cache.entrySet()) {
                    sb.append("\n").append(entry.getValue().toString());
                }
                MonitorLogger.getInstance().log(sb.toString());
            }

        }, (1 + ThreadLocalRandom.current().nextInt(5)) * 1000, 24 * 3600 * 1000);
    }

    public void build(Method method) {
        LRUSoftCache<String, Object> lruSoftCache = new LRUSoftCache<>(method.getName(), capacity);
        cache.put(method, lruSoftCache);
    }

    public boolean useCache(Method method) {
        return cache.containsKey(method);
    }

    public Object getCache(Method method, Object[] args) {
        LRUSoftCache<String, Object> lruSoftCache = cache.get(method);
        if (lruSoftCache != null) {
            return lruSoftCache.get(getKey(args));
        }
        return null;
    }

    public void setCache(Method method, Object[] args, Object result) {
        LRUSoftCache<String, Object> lruSoftCache = cache.get(method);
        if (lruSoftCache != null) {
            lruSoftCache.put(getKey(args), result);
        }
    }

    private String getKey(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                sb.append("$!$");
            }
            Object arg = args[i];
            sb.append((arg != null) ? arg.toString() : "NULL");
        }
        return sb.toString();
    }

    private static int readCapacity() {
        Integer capacity = readSelfCapacity();
        if (capacity == null) {
            capacity = readDefaultCapacity();
            if (capacity == null) {
                capacity = DEFAULT_CAPACITY;
            }
        }
        return capacity;
    }

    private static Integer readSelfCapacity() {
        Map<String, Object> data = ZookeeperFactory.useServerZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                "/rpc/client/requestCache/" + ProjectId.getProjectName());
        if (data != null) {
            return (Integer) data.get("retention_capacity");
        }
        return null;
    }

    private static Integer readDefaultCapacity() {
        Map<String, Object> data = ZookeeperFactory.useServerZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                "/rpc/client/requestCache");
        if (data != null) {
            return (Integer) data.get("retention_capacity_default");
        }
        return null;
    }

}
