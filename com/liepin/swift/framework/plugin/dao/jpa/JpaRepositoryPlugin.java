package com.liepin.swift.framework.plugin.dao.jpa;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.dao.jpa.binding.JpaContainer;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class JpaRepositoryPlugin implements IPlugin<Object> {

    private static final Logger logger = Logger.getLogger(JpaRepositoryPlugin.class);

    public JpaRepositoryPlugin() {
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("JpaRepositoryPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<>().scanClazzes(new JpaRepositoryClassFilter()).forEach(clazz -> {
            JpaContainer.injectRepository(clazz);
            log.append("Added {" + clazz.getName()).append("} to DaoJpaRepository\n");
        });
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        // nothing
    }

    @Override
    public Object getObject() {
        return null;
    }

    @Override
    public String name() {
        return "JpaRepository类加载";
    }

}
