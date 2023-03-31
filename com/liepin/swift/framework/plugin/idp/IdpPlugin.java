package com.liepin.swift.framework.plugin.idp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.dispatcher.handler.HandlerManager;
import com.liepin.dispatcher.handler.IDPHandler;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class IdpPlugin implements IPlugin<List<IDPHandler>> {

    private static final Logger logger = Logger.getLogger(IdpPlugin.class);

    private final List<IDPHandler> idpHandlers = new ArrayList<IDPHandler>();

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("IdpPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<IDPHandler>(applicationContext).scanObjects(new IdpObjectFilter()).forEach(i -> {
            idpHandlers.add(i);
            log.append("Added {" + i.getClass().getName()).append("} to IDPHandler\n");
        });
        logger.info(log.toString());
    }

    public void start() {
        // 预启动
        if (SwiftConfig.enableStartupPreload()) {
            HandlerManager.getInstance().init(idpHandlers);
            HandlerManager.getInstance().start();
        } else {
            new Thread(() -> {
                HandlerManager.getInstance().init(idpHandlers);
                HandlerManager.getInstance().start();
            }).start();
        }
    }

    @Override
    public void destroy() {
        HandlerManager.getInstance().destroy();
        idpHandlers.clear();
        logger.info("IdpPlugin destroy.");
    }

    @Override
    public List<IDPHandler> getObject() {
        return Collections.unmodifiableList(idpHandlers);
    }

    @Override
    public String name() {
        return "IDP加载";
    }

}
