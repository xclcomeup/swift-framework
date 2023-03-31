package com.liepin.swift.framework.limit.rules.processor.fallback2s;

import java.util.HashMap;
import java.util.Map;

import com.liepin.swift.framework.mvc.dispatcher.DispatcherBean;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.service.ServicePlugin;

public class FallbackProvider {

    private final Map<String, FallbackHandler> handlers = new HashMap<>();

    private static final FallbackProvider instance = new FallbackProvider();

    private FallbackProvider() {
        init();
    }

    public static FallbackProvider get() {
        return instance;
    }

    private void init() {
        ServicePlugin plugin = PluginContext.get().getPlugin(ServicePlugin.class);
        plugin.getFallbackHandlerMap().forEach((String t, DispatcherBean bean) -> {
            handlers.put(t, new FallbackHandler(bean.target, bean.method));
        });
    }

    public FallbackHandler getFallbackHandler(String url) {
        return handlers.get(url);
    }

}
