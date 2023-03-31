package com.liepin.swift.framework.dao;

import java.util.List;
import java.util.Map;

import com.liepin.dao.ITemplateDao;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;

public interface IEntityDao<T extends IEntity> {

    public static final String INS_TEMPLETEDAO_PREFIX = "ins.templatedao.";

    public int update(String statement, Object parameter) throws DaoException;

    public int delete(String statement, Object parameter) throws DaoException;

    public int insert(String statement, Object parameter) throws DaoException;

    public Object selectOne(String ibatisID, Map<String, Object> whereMap) throws DaoException;

    public Object selectOneFromMaster(String ibatisID, Map<String, Object> whereMap) throws DaoException;

    public Object selectOneFromSlave(String ibatisID, Map<String, Object> whereMap, int index) throws DaoException;

    public List<?> selectList(String ibatisID, Map<String, Object> whereMap) throws DaoException;

    public List<?> selectListFromMaster(String ibatisID, Map<String, Object> whereMap) throws DaoException;

    public List<?> selectListFromSlave(String ibatisID, Map<String, Object> whereMap, int index) throws DaoException;

    public List<T> selectEntities(final ISelectSql builder) throws DaoException;

    public List<T> selectEntitiesFromMaster(final ISelectSql builder) throws DaoException;

    public List<T> selectEntitiesFromSlave(final ISelectSql builder, int index) throws DaoException;

    public List<T> selectEntitiesInDbFromMaster(final ISelectSql selectSql) throws DaoException;

    public List<T> selectEntitiesInDbFromSlave(final ISelectSql selectSql, int index) throws DaoException;

    public List<T> selectEntitiesInDb(final ISelectSql selectSql) throws DaoException;

    public List<Long> selectPKs(final ISelectSql builder) throws DaoException;

    public List<Long> selectPKsFromMaster(final ISelectSql builder) throws DaoException;

    public List<Long> selectPKsFromSlave(final ISelectSql builder, int index) throws DaoException;

    public List<?> selectDistinctFromMaster(String field, ISelectSql selectSql) throws DaoException;

    public List<?> selectDistinctFromSlave(String field, ISelectSql selectSql, int index) throws DaoException;

    public List<?> selectDistinct(String field, ISelectSql selectSql) throws DaoException;

    public List<T> selectEntitiesByQueryCondition(final QueryCondition queryCondition) throws DaoException;

    public List<T> selectEntitiesByQueryConditionFromMaster(final QueryCondition queryCondition) throws DaoException;

    public List<T> selectEntitiesByQueryConditionFromSlave(final QueryCondition queryCondition, int index)
            throws DaoException;

    public List<Long> selectPKsByQueryCondition(final QueryCondition queryCondition) throws DaoException;

    public List<Long> selectPKsByQueryConditionFromMaster(final QueryCondition queryCondition) throws DaoException;

    public List<Long> selectPKsByQueryConditionFromSlave(final QueryCondition queryCondition, int index)
            throws DaoException;

    public T selectByPK(long id) throws DaoException;

    public T selectByPKFromMaster(long id) throws DaoException;

    public T selectByPKFromSlave(long id, int index) throws DaoException;

    public List<T> selectEntitiesByPKs(List<Long> ids) throws DaoException;

    public List<T> selectEntitiesByPKsFromMaster(List<Long> ids) throws DaoException;

    public List<T> selectEntitiesByPKsFromSlave(List<Long> ids, int index) throws DaoException;

    public List<T> selectEntitiesByPKsWithSortFromMaster(List<Long> ids) throws DaoException;

    public List<T> selectEntitiesByPKsWithSortFromSlave(List<Long> ids, int index) throws DaoException;

    public List<T> selectEntitiesByPKsWithSort(List<Long> ids) throws DaoException;

    @Deprecated
    public List<T> selectByIndex(Object id) throws DaoException;

    @Deprecated
    public List<T> selectByIndexFromMaster(Object id) throws DaoException;

    @Deprecated
    public List<T> selectByIndexFromSlave(Object id, int index) throws DaoException;

    public long selectRowCount(OptimizeSelectSqlImpl builder) throws DaoException;

    public long selectRowCountFromMaster(OptimizeSelectSqlImpl builder) throws DaoException;

    public long selectRowCountFromSlave(OptimizeSelectSqlImpl builder, int index) throws DaoException;

    public long insert(T entity) throws DaoException;

    public List<Long> insertBatch(List<T> entities) throws DaoException;

    public int insertOrUpdate(T entity) throws DaoException;

    public long generatedId(String tableName) throws DaoException;

    public int update(T entity) throws DaoException;

    public int update(T entity, ISelectSql selectSql) throws DaoException;

    public List<Integer> updateBatch(List<T> entities) throws DaoException;

    public int incr(long id, String fieldName) throws DaoException;

    public int incr(long id, String fieldName, int delta) throws DaoException;

    public int incr(long id, String fieldName, float delta) throws DaoException;

    public int decr(long id, String fieldName) throws DaoException;

    public int decr(long id, String fieldName, int delta) throws DaoException;

    public int decr(long id, String fieldName, float delta) throws DaoException;

    public void delete(long id) throws DaoException;

    public void deleteBatch(List<Long> ids) throws DaoException;

    public Long save(T entity) throws DaoException;

    public boolean createTableIfAbsent(String createSql) throws DaoException;

    public ITemplateDao getTemplateDao();

}
