package com.liepin.swift.framework.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.liepin.dao.ITemplateDao;
import com.liepin.dao.binding.DaoBind;
import com.liepin.dao.binding.DaoContainer;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;
import com.liepin.swift.framework.dao.query.QueryConditionBuilder;

public abstract class AbstractEntityDaoProxy {

    protected static final Logger logger = Logger.getLogger(AbstractEntityDaoProxy.class);

    private final Map<String, ITemplateDao> daoMapper = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected AbstractEntityDaoProxy(Class<? extends IEntity>... clazzs) {
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
        dbNames.forEach(dbName -> daoMapper.put(dbName, DaoTool.getTemplateDao(dbName)));
    }

    protected ITemplateDao getTemplateDao(String dbName) {
        return daoMapper.get(dbName);
    }

    @SuppressWarnings({ "unchecked" })
    protected <T extends IEntity> List<T> mapping(List<IEntity> list) {
        List<T> ret = new ArrayList<T>();
        for (IEntity iEntity : list) {
            ret.add((T) iEntity);
        }
        return ret;
    }

    public int update(String dbName, String statement, Object parameter) throws DaoException {
        return getTemplateDao(dbName).update(statement, parameter);
    }

    public int delete(String dbName, String statement, Object parameter) throws DaoException {
        return getTemplateDao(dbName).delete(statement, parameter);
    }

    public int insert(String dbName, String statement, Object parameter) throws DaoException {
        return getTemplateDao(dbName).insert(statement, parameter);
    }

    public Object selectOne(String dbName, String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao(dbName).selectOne(ibatisID, whereMap);
    }

    public Object selectOneFromMaster(String dbName, String ibatisID, Map<String, Object> whereMap)
            throws DaoException {
        return getTemplateDao(dbName).selectOneFromMaster(ibatisID, whereMap);
    }

    public Object selectOneFromSlave(String dbName, String ibatisID, Map<String, Object> whereMap, int index)
            throws DaoException {
        return getTemplateDao(dbName).selectOneFromSlave(ibatisID, whereMap, index);
    }

    public List<?> selectList(String dbName, String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao(dbName).selectList(ibatisID, whereMap);
    }

    public List<?> selectListFromMaster(String dbName, String ibatisID, Map<String, Object> whereMap)
            throws DaoException {
        return getTemplateDao(dbName).selectListFromMaster(ibatisID, whereMap);
    }

    public List<?> selectListFromSlave(String dbName, String ibatisID, Map<String, Object> whereMap, int index)
            throws DaoException {
        return getTemplateDao(dbName).selectListFromSlave(ibatisID, whereMap, index);
    }

    public <T extends IEntity> List<T> selectEntities(String dbName, Class<T> clazz, final ISelectSql builder)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntities(clazz, builder);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesFromMaster(String dbName, Class<T> clazz, final ISelectSql builder)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromMaster(clazz, builder);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesFromSlave(String dbName, Class<T> clazz, final ISelectSql builder,
            int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromSlave(clazz, builder, index);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesInDbFromMaster(String dbName, Class<T> clazz,
            final ISelectSql selectSql) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesInDbFromMaster(clazz, selectSql);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesInDbFromSlave(String dbName, Class<T> clazz,
            final ISelectSql selectSql, int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesInDbFromSlave(clazz, selectSql, index);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesInDb(String dbName, Class<T> clazz, final ISelectSql selectSql)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesInDb(clazz, selectSql);
        return mapping(list);
    }

    public <T extends IEntity> List<Long> selectPKs(String dbName, Class<T> clazz, final ISelectSql builder)
            throws DaoException {
        return getTemplateDao(dbName).selectPKs(clazz, builder);
    }

    public <T extends IEntity> List<Long> selectPKsFromMaster(String dbName, Class<T> clazz, final ISelectSql builder)
            throws DaoException {
        return getTemplateDao(dbName).selectPKsFromMaster(clazz, builder);
    }

    public <T extends IEntity> List<Long> selectPKsFromSlave(String dbName, Class<T> clazz, final ISelectSql builder,
            int index) throws DaoException {
        return getTemplateDao(dbName).selectPKsFromSlave(clazz, builder, index);
    }

    public <T extends IEntity> List<?> selectDistinctFromMaster(String dbName, Class<T> clazz, String field,
            ISelectSql selectSql) throws DaoException {
        return getTemplateDao(dbName).selectDistinctFromMaster(clazz, field, selectSql);
    }

    public <T extends IEntity> List<?> selectDistinctFromSlave(String dbName, Class<T> clazz, String field,
            ISelectSql selectSql, int index) throws DaoException {
        return getTemplateDao(dbName).selectDistinctFromSlave(clazz, field, selectSql, index);
    }

    public <T extends IEntity> List<?> selectDistinct(String dbName, Class<T> clazz, String field, ISelectSql selectSql)
            throws DaoException {
        return getTemplateDao(dbName).selectDistinct(clazz, field, selectSql);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryCondition(String dbName, Class<T> clazz,
            final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntities(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(dbName, clazz, builder));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryConditionFromMaster(String dbName, Class<T> clazz,
            final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromMaster(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(dbName, clazz, builder));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByQueryConditionFromSlave(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesFromSlave(clazz, builder, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(dbName, clazz, builder, index));
        }
        return mapping(list);
    }

    public <T extends IEntity> List<Long> selectPKsByQueryCondition(String dbName, Class<T> clazz,
            final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKs(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(dbName, clazz, builder));
        }
        return ids;
    }

    public <T extends IEntity> List<Long> selectPKsByQueryConditionFromMaster(String dbName, Class<T> clazz,
            final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKsFromMaster(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(dbName, clazz, builder));
        }
        return ids;
    }

    public <T extends IEntity> List<Long> selectPKsByQueryConditionFromSlave(String dbName, Class<T> clazz,
            final QueryCondition queryCondition, int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao(dbName).selectPKsFromSlave(clazz, builder, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(dbName, clazz, builder, index));
        }
        return ids;
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends IEntity> T selectByPK(String dbName, Class<T> clazz, long id) throws DaoException {
        return (T) getTemplateDao(dbName).selectByPK(clazz, id);
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends IEntity> T selectByPKFromMaster(String dbName, Class<T> clazz, long id) throws DaoException {
        return (T) getTemplateDao(dbName).selectByPKFromMaster(clazz, id);
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends IEntity> T selectByPKFromSlave(String dbName, Class<T> clazz, long id, int index)
            throws DaoException {
        return (T) getTemplateDao(dbName).selectByPKFromSlave(clazz, id, index);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKs(String dbName, Class<T> clazz, List<Long> ids)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKs(clazz, ids);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsFromMaster(String dbName, Class<T> clazz, List<Long> ids)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsFromMaster(clazz, ids);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsFromSlave(String dbName, Class<T> clazz, List<Long> ids,
            int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsFromSlave(clazz, ids, index);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsWithSortFromMaster(String dbName, Class<T> clazz,
            List<Long> ids) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsWithSortFromMaster(clazz, ids);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsWithSortFromSlave(String dbName, Class<T> clazz,
            List<Long> ids, int index) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsWithSortFromSlave(clazz, ids, index);
        return mapping(list);
    }

    public <T extends IEntity> List<T> selectEntitiesByPKsWithSort(String dbName, Class<T> clazz, List<Long> ids)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectEntitiesByPKsWithSort(clazz, ids);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndex(String dbName, Class<T> clazz, Object id) throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndex(clazz, id);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndexFromMaster(String dbName, Class<T> clazz, Object id)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndexFromMaster(clazz, id);
        return mapping(list);
    }

    @Deprecated
    public <T extends IEntity> List<T> selectByIndexFromSlave(String dbName, Class<T> clazz, Object id, int index)
            throws DaoException {
        List<IEntity> list = getTemplateDao(dbName).selectByIndexFromSlave(clazz, id, index);
        return mapping(list);
    }

    public <T extends IEntity> long selectRowCount(String dbName, Class<T> clazz, final OptimizeSelectSqlImpl builder)
            throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCount(builder);
    }

    public <T extends IEntity> long selectRowCountFromMaster(String dbName, Class<T> clazz,
            final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCountFromMaster(builder);
    }

    public <T extends IEntity> long selectRowCountFromSlave(String dbName, Class<T> clazz,
            final OptimizeSelectSqlImpl builder, int index) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao(dbName).selectRowCountFromSlave(builder, index);
    }

    public <T extends IEntity> long insert(String dbName, T entity) throws DaoException {
        return getTemplateDao(dbName).insert(entity);
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends IEntity> List<Long> insertBatch(String dbName, List<T> entities) throws DaoException {
        return getTemplateDao(dbName).insertBatch((List<IEntity>) entities);
    }

    public <T extends IEntity> int insertOrUpdate(String dbName, T entity) throws DaoException {
        return getTemplateDao(dbName).insertOrUpdate(entity);
    }

    public long generatedId(String dbName, String tableName) throws DaoException {
        return getTemplateDao(dbName).generatedId(tableName);
    }

    public <T extends IEntity> int update(String dbName, T entity) throws DaoException {
        return getTemplateDao(dbName).update(entity);
    }

    public <T extends IEntity> int update(String dbName, T entity, ISelectSql selectSql) throws DaoException {
        return getTemplateDao(dbName).update(entity, selectSql);
    }

    @SuppressWarnings("unchecked")
    public <T extends IEntity> List<Integer> updateBatch(String dbName, List<T> entities) throws DaoException {
        int[] ids = getTemplateDao(dbName).updateBatch((List<IEntity>) entities);
        List<Integer> _ids = new ArrayList<Integer>();
        for (int i : ids) {
            _ids.add(i);
        }
        return _ids;
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName) throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName);
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName, int delta)
            throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName, delta);
    }

    public <T extends IEntity> int incr(String dbName, Class<T> clazz, long id, String fieldName, float delta)
            throws DaoException {
        return getTemplateDao(dbName).incr(clazz, id, fieldName, delta);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName) throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName, int delta)
            throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName, delta);
    }

    public <T extends IEntity> int decr(String dbName, Class<T> clazz, long id, String fieldName, float delta)
            throws DaoException {
        return getTemplateDao(dbName).decr(clazz, id, fieldName, delta);
    }

    public <T extends IEntity> void delete(String dbName, Class<T> clazz, long id) throws DaoException {
        getTemplateDao(dbName).delete(clazz, id);
    }

    public <T extends IEntity> void deleteBatch(String dbName, Class<T> clazz, List<Long> ids) throws DaoException {
        getTemplateDao(dbName).deleteBatch(clazz, ids);
    }

    public <T extends IEntity> Long save(String dbName, T entity) throws DaoException {
        if (entity.pkValue() == null) {
            return insert(dbName, entity);
        } else {
            if (update(dbName, entity) > 0) {
                return entity.pkValue();
            }
            return 0L;
        }
    }

    public <T extends IEntity> boolean createTableIfAbsent(String dbName, Class<T> clazz, String createSql)
            throws DaoException {
        return getTemplateDao(dbName).createTableIfAbsent(clazz, createSql);
    }

}
