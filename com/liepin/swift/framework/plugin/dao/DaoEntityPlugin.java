package com.liepin.swift.framework.plugin.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.dao.binding.DaoContainer;
import com.liepin.dao.datasource.magic.DatasourceSwitchManager;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.entity.annotation.EntityInfo;
import com.liepin.dao.entity.metadata.TableMove;
import com.liepin.dao.sharding.ShardingStrategyFactory;
import com.liepin.dao.sharding.manager.table.NoneStrategy;
import com.liepin.dao.sharding.manager.table.Strategy;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.PluginScan;

public class DaoEntityPlugin implements IPlugin<IEntity> {

    private static final Logger logger = Logger.getLogger(DaoEntityPlugin.class);

    private final Set<String> generalDbInfos = new HashSet<>();
    private final Map<String, Set<String>> shardDbInfos = new HashMap<>();
    private final Map<String, Map<String, Class<? extends Strategy>>> shardTableInfos = new HashMap<>();

    public DaoEntityPlugin() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("DaoEntityPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<IEntity>().scanClazzes(new DaoEntityClassFilter()).forEach(clazz -> {
            EntityInfo entityInfo = clazz.getAnnotation(EntityInfo.class);
            if (entityInfo != null) {
                String shardingSchema = entityInfo.shardingSchema();
                String[] dbNameArray = entityInfo.dbName().split("\\|");

                // 支持表迁移，但不支持同一entity支持多库模式
                if (dbNameArray.length == 1) {
                    String[] array2 = TableMove.split(dbNameArray[0]);
                    if (array2.length == 2) {
                        dbNameArray = array2;
                    }
                }

                for (String dbName : dbNameArray) {
                    if (!"".equals(shardingSchema)) {
                        // 库sharding
                        Set<String> schemas = shardDbInfos.get(dbName);
                        if (schemas == null) {
                            shardDbInfos.put(dbName, schemas = new HashSet<>());
                        }
                        schemas.add(shardingSchema);
                    } else if (NoneStrategy.class != entityInfo.shardingTableStrategy()) {
                        // 表sharding
                        Map<String, Class<? extends Strategy>> map = shardTableInfos.get(dbName);
                        if (map == null) {
                            shardTableInfos.put(dbName, map = new HashMap<>());
                        }
                        map.put(clazz.getSimpleName().toLowerCase(), entityInfo.shardingTableStrategy());
                        try {
                            ShardingStrategyFactory.register(clazz.getSimpleName().toLowerCase(),
                                    entityInfo.shardingTableStrategy());
                        } catch (Exception e) {
                            throw new RuntimeException("加载Dao分表策略，Entity=" + clazz.getSimpleName() + ", 分表策略="
                                    + entityInfo.shardingTableStrategy() + "失败, 原因：策略类实例化异常!", e);
                        }
                    } else {
                        // 不分库不分表
                        generalDbInfos.add(dbName);
                    }
                }
                DaoContainer.inject((Class<? extends IEntity>) clazz);
                log.append("Added {" + clazz.getName()).append("} to DaoEntity\n");
            }
        });
        logger.info(log.toString());
        // 加载数据源动态监听
        DatasourceSwitchManager.getInstance().init();
    }

    /**
     * 返回非sharding数据库名列表
     * 
     * @return
     */
    public Set<String> getGeneralDbInfos() {
        return generalDbInfos;
    }

    /**
     * 返回sharding数据库及schema对应列表
     * 
     * @return
     */
    public Map<String, Set<String>> getShardDbInfos() {
        return shardDbInfos;
    }

    /**
     * 返回sharding表策略
     * 
     * @return
     */
    public Map<String, Map<String, Class<? extends Strategy>>> getShardTableInfos() {
        return shardTableInfos;
    }

    @Override
    public void destroy() {
        // nothing
    }

    @Override
    public IEntity getObject() {
        // nothing
        return null;
    }

    @Override
    public String name() {
        return "Entity类加载";
    }

}
