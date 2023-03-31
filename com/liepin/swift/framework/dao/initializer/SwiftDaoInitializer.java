package com.liepin.swift.framework.dao.initializer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liepin.common.conf.SystemUtil;
import com.liepin.common.datastructure.Pair;
import com.liepin.dao.autobox.AutoboxMysqlTemplateDaoImpl;
import com.liepin.dao.autobox.AutoboxShardingTemplateDaoImpl;
import com.liepin.dao.datasource.conf.DataSourceReader;
import com.liepin.dao.datasource.conf.Resource;
import com.liepin.dao.datasource.conf.ResourceLink;
import com.liepin.dao.sharding.manager.table.Strategy;
import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.dao.IEntityDao;
import com.liepin.swift.framework.dao.IShardingEntityDao;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.dao.DaoEntityPlugin;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

@Configuration
public class SwiftDaoInitializer {

    private static final Logger logger = Logger.getLogger(SwiftDaoInitializer.class);

    private static final String DATASOURCE_NAME = DataSource.class.getName();
    private static final String FACTORY_NAME = "org.apache.tomcat.jdbc.pool.DataSourceFactory";
    private static final String DRIVER_CALSS_NAME = "com.mysql.cj.jdbc.Driver";// org.gjt.mm.mysql.Driver,com.mysql.jdbc.Driver,com.mysql.cj.jdbc.Driver
    private static final String MYBATIS_PATTERN = "classpath:mybatis/*-SqlMap.xml";
    private static final String MYBATIS_CONFIG_PATTERN = "classpath:mybatis/*-Config.xml";

    private static final DaoConfig daoConfigDefault = new DaoConfig();

    // private File contextFile;
    private String daoContext;
    private boolean isNeed;

    public SwiftDaoInitializer() {
        String localDaoContext = readLocalDaoContext();
        if (localDaoContext != null) {
            this.daoContext = localDaoContext;
        } else {
            this.daoContext = DataSourceReader.readDaoContext();
        }
        this.isNeed = daoContext != null;
        if (isNeed) {
            // 数据库访问ip授权校验
            ipAuth();
        }
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {

            @Override
            protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
                if (isNeed) {
                    tomcat.enableNaming();
                }

                return super.getTomcatWebServer(tomcat);
            }

            @Override
            protected void postProcessContext(Context context) {
                if (isNeed) {
                    Pair<List<Resource>, List<ResourceLink>> daoContextConfig = DataSourceReader
                            .getDaoContextConfig(daoContext);

                    List<Resource> resources = daoContextConfig.getFirst();
                    List<ResourceLink> resourceLinks = daoContextConfig.getSecond();
                    Map<String, ContextResource> contextResourceMap = new HashMap<>(resources.size());

                    resources.forEach((Resource resource) -> {
                        DaoConfig daoConfig = new DaoConfig(resource.getJndiName());
                        ContextResource contextResource = new ContextResource();
                        contextResource.setName(resource.getJndiName());
                        contextResource.setType(DATASOURCE_NAME);
                        contextResource.setProperty("factory", FACTORY_NAME);
                        contextResource.setProperty("driverClassName", DRIVER_CALSS_NAME);
                        if (resource.getDriverClassName() != null) {
                            contextResource.setProperty("driverClassName", resource.getDriverClassName());
                        }
                        contextResource.setProperty("url", resource.getUrl() + daoConfig.getUrl());
                        contextResource.setProperty("username", resource.getUsername());
                        contextResource.setProperty("password", resource.getPassword());
                        extend(contextResource, daoConfig);
                        context.getNamingResources().addResource(contextResource);
                        contextResourceMap.put(resource.getJndiName(), contextResource);
                    });
                    // 补齐差异
                    resourceLinks.forEach((ResourceLink resourceLink) -> {
                        String name = resourceLink.getName();
                        if (!name.startsWith("jdbc/")) {
                            name = "jdbc/" + name;
                        }
                        if (!contextResourceMap.containsKey(name)) {
                            ContextResource transform = transform(contextResourceMap.get(resourceLink.getGlobal()),
                                    name);
                            context.getNamingResources().addResource(transform);
                        }
                    });

                    // resourceLinks.forEach((ResourceLink resourceLink) -> {
                    // ContextResourceLink link = new ContextResourceLink();
                    // link.setGlobal(resourceLink.getGlobal());
                    // link.setName(resourceLink.getName());
                    // link.setType(DATASOURCE_NAME);
                    // context.getNamingResources().addResourceLink(link);
                    // });
                }
                super.postProcessContext(context);
            }

        };
        return tomcat;
    }

    public static void initialize() {
        DaoEntityPlugin daoPlugin = PluginContext.get().loadPlugin(DaoEntityPlugin.class);
        // 是否对entity和数据源配置一致性进行加载
        Set<String> dbNamesInDataSources = (!SwiftConfig.enableEntityAndDataSourceConsistencyLoad())
                ? DataSourceReader.readDao4DbNames() : null;

        /**
         * <bean id="ins.templatedao.test" class=
         * "com.liepin.dao.autobox.AutoboxMysqlTemplateDaoImpl" init-method=
         * "init" destroy-method="destroy">
         * <property name="dataSourceType" value="JNDI" />
         * <property name="dbName" value="test" />
         * <property name="slaves" value="ro:0-1" /> <!--
         * <property name="slaves" value="rw" /> --> <!--
         * <property name="mapperLocations" value=
         * "classpath:mybatis/*-SqlMap.xml" /> --> </bean>
         * 
         */
        Set<String> defaultDbInfos = daoPlugin.getGeneralDbInfos();
        // 不开启校验，忽略模式
        Optional.ofNullable(dbNamesInDataSources).ifPresent(t -> {
            Iterator<String> iterator = defaultDbInfos.iterator();
            while (iterator.hasNext()) {
                String dbName = iterator.next();
                if (!t.contains(dbName)) {
                    iterator.remove();
                    logger.warn("SwiftDaoInitializer 单库模式：entity存在使用" + dbName + "数据库，但数据源没有配置，忽略加载");
                }
            }
        });

        boolean mybatisFlag = hadMybatis();
        org.springframework.core.io.Resource confResource = getMybatisConfResource();
        defaultDbInfos.forEach(dbName -> {
            String beanName = IEntityDao.INS_TEMPLETEDAO_PREFIX + dbName;
            Map<String, Object> properties = new HashMap<>();
            properties.put("dataSourceType", "JNDI");
            properties.put("dbName", dbName);
            properties.put("slaves", daoConfigDefault.getSlaves());
            if (mybatisFlag) {
                properties.put("mapperLocations", MYBATIS_PATTERN);// 规范
            }
            if (Objects.nonNull(confResource)) {
                properties.put("configLocation", confResource);
            }
            SpringContextUtil.setBean(beanName, AutoboxMysqlTemplateDaoImpl.class, "init", "destroy", properties);
            logger.info("SwiftDaoInitializer ioc dao instance: " + beanName);
        });

        /**
         * <bean id="ins.templatedao.sharding.im_liepin" class=
         * "com.liepin.dao.autobox.AutoboxShardingTemplateDaoImpl" init-method=
         * "init"> <property name="dataSourceType" value="JNDI" />
         * <property name="dbName" value="im_liepin" />
         * <property name="schemas"> <list> <value>user</value>
         * <value>message</value> </list> </property>
         * <!--<property name="shardingTableStrategy"> <map>
         * <entry key="xxxEntity" value=
         * "com.liepin.dao.sharding.manager.table.CommonStrategy" /> </map>
         * </property>-->
         * <property name="mapperLocations" value="classpath:*-SqlMap.xml" />
         * <property name="slaves" value="ro" /> </bean>
         */
        Map<String, Set<String>> shardDbInfos = daoPlugin.getShardDbInfos();
        Map<String, Map<String, Class<? extends Strategy>>> shardTableInfos = daoPlugin.getShardTableInfos();
        Map<String, Pair<Set<String>, Map<String, Class<? extends Strategy>>>> tmp = new HashMap<>();
        Set<String> dbNames = new HashSet<>();
        dbNames.addAll(shardDbInfos.keySet());
        dbNames.addAll(shardTableInfos.keySet());
        for (String dbName : dbNames) {
            Set<String> first = shardDbInfos.get(dbName);
            Map<String, Class<? extends Strategy>> second = shardTableInfos.get(dbName);
            Pair<Set<String>, Map<String, Class<? extends Strategy>>> pair = new Pair<Set<String>, Map<String, Class<? extends Strategy>>>(
                    first, second);
            tmp.put(dbName, pair);
        }
        // 不开启校验，忽略模式
        Optional.ofNullable(dbNamesInDataSources).ifPresent(t -> {
            Iterator<String> iterator = tmp.keySet().iterator();
            while (iterator.hasNext()) {
                String dbName = iterator.next();
                boolean remove = true;
                for (String dbNamesInDataSource : t) {
                    if (dbNamesInDataSource.contains(dbName)) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    iterator.remove();
                    logger.warn("SwiftDaoInitializer 分库分表模式：entity存在使用" + dbName + "数据库，但数据源没有配置，忽略加载");
                }
            }
        });

        tmp.forEach((String dbName, Pair<Set<String>, Map<String, Class<? extends Strategy>>> pair) -> {
            String beanName = IShardingEntityDao.INS_SHARDING_TEMPLETEDAO_PREFIX + dbName;
            Map<String, Object> properties = new HashMap<>();
            properties.put("dataSourceType", "JNDI");
            properties.put("dbName", dbName);
            if (pair.getFirst() != null) {
                properties.put("schemas", new ArrayList<>(pair.getFirst()));
            }
            if (pair.getSecond() != null) {
                properties.put("shardingTableStrategy", pair.getSecond());
            }
            properties.put("slaves", daoConfigDefault.getSlaves());
            if (mybatisFlag) {
                properties.put("mapperLocations", MYBATIS_PATTERN);// 规范
            }
            if (Objects.nonNull(confResource)) {
                properties.put("configLocation", confResource);
            }
            SpringContextUtil.setBean(beanName, AutoboxShardingTemplateDaoImpl.class, "init", "destroy", properties);
            logger.info("SwiftDaoInitializer ioc shard dao instance: " + beanName);
        });
    }

    private void extend(final ContextResource contextResource, final DaoConfig daoConfig) {
        contextResource.setProperty("initialSize", daoConfig.getInitialSize());
        contextResource.setProperty("minIdle", daoConfig.getMinIdle());
        contextResource.setProperty("maxIdle", daoConfig.getMaxIdle());
        contextResource.setProperty("maxWait", daoConfig.getMaxWait());
        contextResource.setProperty("maxActive", daoConfig.getMaxActive());
        contextResource.setProperty("removeAbandoned", daoConfig.getRemoveAbandoned());
        contextResource.setProperty("removeAbandonedTimeout", daoConfig.getRemoveAbandonedTimeout());
        contextResource.setProperty("logAbandoned", daoConfig.getLogAbandoned());
        contextResource.setProperty("validationQuery", daoConfig.getValidationQuery());
        contextResource.setProperty("testWhileIdle", daoConfig.getTestWhileIdle());
        contextResource.setProperty("testOnBorrow", daoConfig.getTestOnBorrow());
        contextResource.setProperty("testOnReturn", daoConfig.getTestOnReturn());
        contextResource.setProperty("timeBetweenEvictionRunsMillis", daoConfig.getTimeBetweenEvictionRunsMillis());
        contextResource.setProperty("validationInterval", daoConfig.getValidationInterval());
        contextResource.setProperty("validationQueryTimeout", daoConfig.getValidationQueryTimeout());
        contextResource.setProperty("minEvictableIdleTimeMillis", daoConfig.getMinEvictableIdleTimeMillis());
    }

    private String readLocalDaoContext() {
        String daoContextFile = System.getProperty("daoContext");
        if (daoContextFile != null && daoContextFile.trim().length() != 0) {
            try (FileInputStream fis = new FileInputStream(daoContextFile);) {
                return IOUtils.toString(fis);
            } catch (IOException e) {
            }
        }
        return null;
    }

    private static boolean hadMybatis() {
        org.springframework.core.io.Resource[] resources = SpringContextUtil.getResources(MYBATIS_PATTERN);
        return resources != null && resources.length > 0;
    }

    private static org.springframework.core.io.Resource getMybatisConfResource() {
        org.springframework.core.io.Resource[] resources = SpringContextUtil.getResources(MYBATIS_CONFIG_PATTERN);
        if (resources == null || resources.length == 0) {
            return null;
        }
        if (resources.length == 1) {
            return resources[0];
        } else {
            throw new RuntimeException("出现多个mybatis configuration文件: 匹配规则" + MYBATIS_CONFIG_PATTERN);
        }
    }

    private ContextResource transform(final ContextResource source, String name) {
        ContextResource target = new ContextResource();
        DaoConfig daoConfig = new DaoConfig(name);
        target.setName(name);
        target.setType(DATASOURCE_NAME);
        target.setProperty("factory", FACTORY_NAME);
        target.setProperty("driverClassName", DRIVER_CALSS_NAME);
        target.setProperty("url", source.getProperty("url"));
        target.setProperty("username", source.getProperty("username"));
        target.setProperty("password", source.getProperty("password"));
        extend(target, daoConfig);
        // 临时强制修改，降低数据库连接数，待resourceLink支持后再去掉.一般发生在分库场景
        target.setProperty("initialSize", "2");
        target.setProperty("minIdle", "2");
        return target;
    }

    private void ipAuth() {
        if (SystemUtil.isOffline()) {
            return;
        }
        if (!SwiftConfig.enableDaoIpAuth()) {
            return;
        }
        Map<String, Object> map = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                "/middleware/mysql/ipAuth");
        if (map == null || map.isEmpty()) {
            return;
        }
        String ip = SystemUtil.getInNetworkIp();

        boolean match = map.keySet().stream().anyMatch(t -> {
            return ip.startsWith(getNetmask(t));
        });
        if (!match) {
            throw new RuntimeException("Ip=" + ip + " 未授权访问Mysql数据库!");
        }
    }

    private String getNetmask(String value) {
        if (value.endsWith("/24")) {
            // 参考 10.161.215.0/24
            return value.substring(0, value.lastIndexOf("0/24"));
        }
        if (value.endsWith("/16")) {
            // 参考 10.110.0.0/16
            return value.substring(0, value.lastIndexOf("0.0/16"));
        }
        return value;
    }

}
