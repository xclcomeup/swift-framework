package com.liepin.swift.framework.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.liepin.dao.binding.DaoBind;
import com.liepin.dao.binding.DaoContainer;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sharding.IShardValue;
import com.liepin.dao.sharding.IShardingTemplateDao;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;
import com.liepin.swift.framework.dao.query.QueryConditionBuilder;

public abstract class AbstractShardingEntityDaoProxy {

    protected static final Logger logger = Logger.getLogger(AbstractShardingEntityDaoProxy.class);

    private final Map<String, IShardingTemplateDao> daoMapper = new HashMap<>();

    @SuppressWarnings("unchecked")
    public AbstractShardingEntityDaoProxy(Class<? extends IEntity>... clazzs) {
        Set<String> dbNames = new HashSet<>();
        for (Class<? extends IEntity> clazz : clazzs) {
            DaoBind daoBind = DaoContainer.getDaoBind(clazz);
            String dbName = daoBind.getDbName();
            if (daoBind.getMetadata().isMultiDb()) {
                String[] array = dbName.split("\\|");
                dbNames.addAll(Arrays.asList(array));
            } else {
                dbNames.add(dbName);
            }
        }
        dbNames.forEach(dbName -> daoMapper.put(dbName, DaoTool.getShardingTemplateDao(dbName)));
    }

    protected IShardingTemplateDao getTemplateDao(String dbName) {
        return daoMapper.get(dbName);
    }

    @SuppressWarnings("unchecked")
    protected <T extends IEntity> List<T> mapping(List<IEntity> list) {
        List<T> ret = new ArrayList<T>();
        for (IEntity iEntity : list) {
            ret.add((T) iEntity);
        }
        return ret;
    }

    public Object selectOne(String dbName, String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).selectOne(ibatisID, whereMap, shardValue);
    }

    public Object selectOneFromMaster(String dbName, String ibatisID, Map<String, Object> whereMap,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectOneFromMaster(ibatisID, whereMap, shardValue);
    }

    public Object selectOneFromSlave(String dbName, String ibatisID, Map<String, Object> whereMap,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectOneFromSlave(ibatisID, whereMap, shardValue, index);
    }

    public List<?> selectList(String dbName, String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).selectList(ibatisID, whereMap, shardValue);
    }

    public List<?> selectListFromMaster(String dbName, String ibatisID, Map<String, Object> whereMap,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectListFromMaster(ibatisID, whereMap, shardValue);
    }

    public List<?> selectListFromSlave(String dbName, String ibatisID, Map<String, Object> whereMap,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectListFromSlave(ibatisID, whereMap, shardValue, index);
    }

    public int update(String dbName, String ibatisID, Map<String, Object> parameters, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).update(ibatisID, parameters, shardValue);
    }

    public int delete(String dbName, String ibatisID, Map<String, Object> parameters, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).delete(ibatisID, parameters, shardValue);
    }

    public <T extends IEntity> List<T> selectEntities(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntities(clazz, builder, shardValue);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesFromMaster(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromMaster(clazz, builder, shardValue);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesFromSlave(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue, int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromSlave(clazz, builder, shardValue, index);
        return mapping(list);
    }

    public <T extends IEntity> List<Long> selectPKs(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectPKs(clazz, builder, shardValue);
    }

    public <T extends IEntity> List<Long> selectPKsFromMaster(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectPKsFromMaster(clazz, builder, shardValue);
    }

    public <T extends IEntity> List<Long> selectPKsFromSlave(String dbName, Class<T> clazz, final ISelectSql builder,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectPKsFromSlave(clazz, builder, shardValue, index);
    }

    public <T extends IEntity> List<?> selectDistinctFromMaster(String dbName, Class<T> clazz, String field,
            ISelectSql selectSql, IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDistinctFromMaster(clazz, field, selectSql, shardValue);
    }

    public <T extends IEntity> List<?> selectDistinctFromSlave(String dbName, Class<T> clazz, String field,
            ISelectSql selectSql, IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectDistinctFromSlave(clazz, field, selectSql, shardValue, index);
    }

    public <T extends IEntity> List<?> selectDistinct(String dbName, Class<T> clazz, String field, ISelectSql selectSql,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDistinct(clazz, field, selectSql, shardValue);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryCondition(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntities(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(dbName, clazz, shardValue, builder));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryConditionFromMaster(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromMaster(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(dbName, clazz, shardValue, builder));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryConditionFromSlave(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue, int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromSlave(clazz, builder, shardValue, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(dbName, clazz, shardValue, builder, index));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<Long> selectPKsByQueryCondition(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKs(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(dbName, clazz, shardValue, builder));
        }
        return ids;
    }

    public <T extends IEntity> List<Long> selectPKsByQueryConditionFromMaster(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKsFromMaster(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(dbName, clazz, shardValue, builder));
        }
        return ids;
    }

    public <T extends IEntity> List<Long> selectPKsByQueryConditionFromSlave(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, IShardValue shardValue, int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKsFromSlave(clazz, builder, shardValue, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(dbName, clazz, shardValue, builder, index));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> T selectByPK(String dbName, Class<T> clazz, long id, IShardValue shardValue)
            throws DaoException {
        return (T) getTemplateDao(dbName).selectByPK(clazz, id, shardValue);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> T selectByPKFromMaster(String dbName, Class<T> clazz, long id, IShardValue shardValue)
            throws DaoException {
        return (T) getTemplateDao(dbName).selectByPKFromMaster(clazz, id, shardValue);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> T selectByPKFromSlave(String dbName, Class<T> clazz, long id, IShardValue shardValue,
            int index) throws DaoException {
        return (T) getTemplateDao(dbName).selectByPKFromSlave(clazz, id, shardValue, index);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKs(String dbName, Class<T> clazz, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKs(clazz, ids, shardValues);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsFromMaster(String dbName, Class<T> clazz, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsFromMaster(clazz, ids, shardValues);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsFromSlave(String dbName, Class<T> clazz, List<Long> ids,
            List<IShardValue> shardValues, int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsFromSlave(clazz, ids, shardValues, index);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndex(String dbName, Class<T> clazz, Object indexValue,
            IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndex(clazz, indexValue, shardValue);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndexFromMaster(String dbName, Class<T> clazz, Object indexValue,
            IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndexFromMaster(clazz, indexValue, shardValue);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndexFromSlave(String dbName, Class<T> clazz, Object indexValue,
            IShardValue shardValue, int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndexFromSlave(clazz, indexValue, shardValue, index);
        return mapping(list);
    }

    public IEntity[] selectDiffEntityByPK(String dbName, Class<? extends IEntity>[] clazzs, long id,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPK(clazzs, id, shardValue);
    }

    public IEntity[] selectDiffEntityByPKFromMaster(String dbName, Class<? extends IEntity>[] clazzs, long id,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPKFromMaster(clazzs, id, shardValue);
    }

    public IEntity[] selectDiffEntityByPKFromSlave(String dbName, Class<? extends IEntity>[] clazzs, long id,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPKFromSlave(clazzs, id, shardValue, index);
    }

    public List<IEntity[]> selectDiffEntityByPKs(String dbName, Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPKs(clazzs, ids, shardValues);
    }

    public List<IEntity[]> selectDiffEntityByPKsFromMaster(String dbName, Class<? extends IEntity>[] clazzs,
            List<Long> ids, List<IShardValue> shardValues) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPKsFromMaster(clazzs, ids, shardValues);
    }

    public List<IEntity[]> selectDiffEntityByPKsFromSlave(String dbName, Class<? extends IEntity>[] clazzs,
            List<Long> ids, List<IShardValue> shardValues, int index) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByPKsFromSlave(clazzs, ids, shardValues, index);
    }

    public List<IEntity>[] selectDiffEntityByIndex(String dbName, Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByIndex(clazzs, id, shardValue);
    }

    public List<IEntity>[] selectDiffEntityByIndexFromMaster(String dbName, Class<? extends IEntity>[] clazzs,
            Object id, IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByIndexFromMaster(clazzs, id, shardValue);
    }

    public List<IEntity>[] selectDiffEntityByIndexFromSlave(String dbName, Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao(dbName).selectDiffEntityByIndexFromSlave(clazzs, id, shardValue, index);
    }

    public <T extends IEntity> long insert(String dbName, T entity, IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).insert(entity, shardValue);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> void insertBatch(String dbName, List<T> entities, List<IShardValue> shardValues)
            throws DaoException {
        getTemplateDao(dbName).insertBatch((List<IEntity>) entities, shardValues);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> void insertBatch2(String dbName, List<T> entities, IShardValue shardValue)
            throws DaoException {
        getTemplateDao(dbName).insertBatch2((List<IEntity>) entities, shardValue);
    }

    public <T extends IEntity> int insertOrUpdate(String dbName, T entity, IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).insertOrUpdate(entity, shardValue);
    }

    /**
     * 返回的是entity的主键，而不是影响行数
     * 
     * @param dbName
     * @param entity
     * @param shardValue
     * @return
     * @throws DaoException
     */
    public <T extends IEntity> long update(String dbName, T entity, IShardValue shardValue) throws DaoException {
        getTemplateDao(dbName).update(entity, shardValue);
        return entity.pkValue();
    }

    /**
     * 返回的是影响行数
     * 
     * @param dbName
     * @param entity
     * @param shardValue
     * @return
     * @throws DaoException
     */
    public <T extends IEntity> int updateAndReturnRows(String dbName, T entity, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).update(entity, shardValue);
    }

    public <T extends IEntity> int update(String dbName, T entity, ISelectSql selectSql, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).update(entity, selectSql, shardValue);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> void updateBatch(String dbName, List<T> entities, List<IShardValue> shardValues)
            throws DaoException {
        getTemplateDao(dbName).updateBatch((List<IEntity>) entities, shardValues);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> void updateBatch2(String dbName, List<T> entities, IShardValue shardValue)
            throws DaoException {
        getTemplateDao(dbName).updateBatch2((List<IEntity>) entities, shardValue);
    }

    public <T extends IEntity> void delete(String dbName, Class<T> clazz, long id, IShardValue shardValue)
            throws DaoException {
        getTemplateDao(dbName).delete(clazz, id, shardValue);
    }

    public <T extends IEntity> void deleteBatch(String dbName, Class<T> clazz, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        getTemplateDao(dbName).deleteBatch(clazz, ids, shardValues);
    }

    public <T extends IEntity> void deleteBatch2(String dbName, Class<T> clazz, List<Long> ids, IShardValue shardValue)
            throws DaoException {
        getTemplateDao(dbName).deleteBatch2(clazz, ids, shardValue);
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName, shardValue);
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName, int delta,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName, delta, shardValue);
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName, float delta,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName, delta, shardValue);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName, shardValue);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName, int delta,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName, delta, shardValue);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName, float delta,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName, delta, shardValue);
    }

    public <T extends IEntity> long selectRowCount(String dbName, Class<T> clazz, IShardValue shardValue,
            final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCount(builder, shardValue);
    }

    public <T extends IEntity> long selectRowCountFromMaster(String dbName, Class<T> clazz, IShardValue shardValue,
            final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCountFromMaster(builder, shardValue);
    }

    public <T extends IEntity> long selectRowCountFromSlave(String dbName, Class<T> clazz, IShardValue shardValue,
            final OptimizeSelectSqlImpl builder, int index) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCountFromSlave(builder, shardValue, index);
    }

    public <T extends IEntity> boolean createTableIfAbsent(String dbName, Class<T> clazz, IShardValue shardValue,
            String createSql) throws DaoException {
        return getTemplateDao(dbName).createTableIfAbsent(clazz, shardValue, createSql);
    }

    public <T extends IEntity> boolean dropTable(String dbName, Class<T> clazz, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao(dbName).dropTable(clazz, shardValue);
    }

    public <T extends IEntity> Long save(String dbName, T entity, IShardValue shardValue) throws DaoException {
        if (entity.pkValue() == null) {
            return insert(dbName, entity, shardValue);
        } else {
            if (update(dbName, entity, shardValue) > 0) {
                return entity.pkValue();
            }
            return 0L;
        }
    }

}
