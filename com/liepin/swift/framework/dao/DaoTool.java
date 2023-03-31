package com.liepin.swift.framework.dao;

import org.apache.log4j.Logger;

import com.liepin.dao.IMysqlTemplateDao;
import com.liepin.dao.ITemplateDao;
import com.liepin.dao.binding.DaoContainer;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sharding.IShardValue;
import com.liepin.dao.sharding.IShardingTemplateDao;
import com.liepin.swift.core.util.SpringContextUtil;

public class DaoTool {

    private static final Logger logger = Logger.getLogger(DaoTool.class);

    public static IShardingTemplateDao getShardingTemplateDao(Class<? extends IEntity> clazz) {
        String dbName = DaoContainer.getDaoBind(clazz).getDbName();
        return getShardingTemplateDao(dbName);
    }

    public static IShardingTemplateDao getShardingTemplateDao(String dbName) {
        return SpringContextUtil.getBean(IShardingEntityDao.INS_SHARDING_TEMPLETEDAO_PREFIX + dbName,
                IShardingTemplateDao.class);
    }

    public static ITemplateDao getTemplateDao(Class<? extends IEntity> clazz) {
        String dbName = DaoContainer.getDaoBind(clazz).getDbName();
        return getTemplateDao(dbName);
    }

    public static ITemplateDao getTemplateDao(String dbName) {
        return SpringContextUtil.getBean(IEntityDao.INS_TEMPLETEDAO_PREFIX + dbName, IMysqlTemplateDao.class);
    }

    public static void removeCache(Class<? extends IEntity> clazz, long id) {
        ITemplateDao templateDao = getTemplateDao(clazz);
        try {
            templateDao.removePKCache(clazz, id);
        } catch (DaoException e) {
            logger.error("清理一级缓存失败: clazz=" + clazz + ", id=" + id, e);
        }
    }

    public static void setCache(IEntity entity) {
        ITemplateDao templateDao = getTemplateDao(entity.getClass());
        try {
            templateDao.setPKCache(entity);
        } catch (Exception e) {
            logger.error("设置一级缓存失败: clazz=" + entity.getClass() + ", id=" + entity.pkValue(), e);
        }
    }

    public static Boolean addIfAbsentCache(IEntity entity) {
        ITemplateDao templateDao = getTemplateDao(entity.getClass());
        try {
            return templateDao.addIfAbsentPKCache(entity);
        } catch (Exception e) {
            logger.error("设置一级缓存失败: clazz=" + entity.getClass() + ", id=" + entity.pkValue(), e);
            return null;
        }
    }

    @SafeVarargs
    public static void removeCache4Sharding(long id, IShardValue shardValue, Class<? extends IEntity>... classes) {
        for (int i = 0; i < classes.length; i++) {
            Class<? extends IEntity> clazz = classes[i];
            IShardingTemplateDao dao = getShardingTemplateDao(clazz);
            try {
                dao.removePKCache(clazz, id, shardValue);
            } catch (DaoException e) {
                logger.error("清理一级缓存失败: clazz=" + clazz + ", objectId=" + shardValue.objectId() + ", id=" + id, e);
            }
        }
    }

    public static void setCache4Sharding(IEntity entity, IShardValue shardValue) {
        IShardingTemplateDao dao = getShardingTemplateDao(entity.getClass());
        try {
            dao.setPKCache(entity, shardValue);
        } catch (Exception e) {
            logger.error("清理一级缓存失败: clazz=" + entity.getClass() + ", objectId=" + shardValue.objectId() + ", id="
                    + entity.pkValue(), e);
        }
    }

    public static Boolean addIfAbsentCache4Sharding(IEntity entity, IShardValue shardValue) {
        IShardingTemplateDao dao = getShardingTemplateDao(entity.getClass());
        try {
            return dao.addIfAbsentPKCache(entity, shardValue);
        } catch (Exception e) {
            logger.error("清理一级缓存失败: clazz=" + entity.getClass() + ", objectId=" + shardValue.objectId() + ", id="
                    + entity.pkValue(), e);
            return null;
        }
    }

}
