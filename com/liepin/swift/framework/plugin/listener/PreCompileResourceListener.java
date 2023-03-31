package com.liepin.swift.framework.plugin.listener;

import org.springframework.stereotype.Service;

import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.resource.ResourcePlugin;

@Service
public class PreCompileResourceListener implements IAfterListener {

    @Override
    public void onApplicationEvent() {
        PluginContext.get().getPlugin(ResourcePlugin.class).loadResource();
    }

}
