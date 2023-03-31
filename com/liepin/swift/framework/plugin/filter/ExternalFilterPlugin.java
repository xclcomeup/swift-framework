package com.liepin.swift.framework.plugin.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.framework.mvc.filter.external.ExternalFilter;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

/**
 * 过滤器组件
 * 
 * @author yuanxl
 * 
 */
public class ExternalFilterPlugin implements IPlugin<List<ExternalFilter>> {

    private static final Logger logger = Logger.getLogger(ExternalFilterPlugin.class);

    private final List<ExternalFilter> filters = new ArrayList<ExternalFilter>();

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ExternalFilterPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<ExternalFilter>(applicationContext).scanObjects(new ExternalFilterObjectFilter()).forEach(e -> {
            filters.add(e);
            log.append("Added {" + e.getClass().getName()).append("} to ExternalFilter\n");
        });
        Collections.sort(filters, new FilterComparator());
        logger.info(log.toString());
    }

    @Override
    public void destroy() {
        filters.clear();
        logger.info("ExternalFilterPlugin destroy.");
    }

    @Override
    public List<ExternalFilter> getObject() {
        return Collections.unmodifiableList(filters);
    }

    private class FilterComparator implements Comparator<ExternalFilter> {

        @Override
        public int compare(ExternalFilter o1, ExternalFilter o2) {
            if (o1.priority() == o2.priority()) {
                return 0;
            }
            return (o1.priority() > o2.priority()) ? 1 : -1;
        }

    }

    @Override
    public String name() {
        return "外部过滤器加载";
    }

}
