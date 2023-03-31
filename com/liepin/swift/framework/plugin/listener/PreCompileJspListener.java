package com.liepin.swift.framework.plugin.listener;

import org.springframework.stereotype.Service;

import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.jsp.JspPlugin;

@Service
public class PreCompileJspListener implements IAfterListener {

    @Override
    public void onApplicationEvent() {
        PluginContext.get().getPlugin(JspPlugin.class).compileJsp();
    }

}
