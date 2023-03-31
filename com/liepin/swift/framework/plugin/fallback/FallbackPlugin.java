package com.liepin.swift.framework.plugin.fallback;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.rpc.limit.FallbackManager;

public class FallbackPlugin implements IPlugin<Void> {

    private static final Logger logger = Logger.getLogger(FallbackPlugin.class);

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("FallbackPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<Object>(applicationContext).scanObjects(new FallbackObjectFilter()).forEach(o -> {
            FallbackManager.addFallbackService(o).forEach(uri -> {
                log.append("Added {" + uri).append("} to Fallback\n");
            });
        });
        logger.info(log.toString());
    }

    @Override
    public void destroy() {

    }

    @Override
    public Void getObject() {
        return null;
    }

    @Override
    public String name() {
        return "客户端RPC降级加载";
    }

}
