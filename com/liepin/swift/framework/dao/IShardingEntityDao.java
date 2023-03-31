package com.liepin.swift.framework.dao;

import java.util.List;
import java.util.Map;

import com.liepin.dao.entity.IEntity;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sharding.IShardValue;
import com.liepin.dao.sharding.IShardingTemplateDao;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;

public interface IShardingEntityDao<T extends IEntity> {

    public static final String INS_SHARDING_TEMPLETEDAO_PREFIX = "ins.templatedao.sharding.";

    public Object selectOne(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue) throws DaoException;

    public Object selectOneFromMaster(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException;

    public Object selectOneFromSlave(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue, int index)
            throws DaoException;

    public List<?> selectList(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException;

    public List<?> selectListFromMaster(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException;

    public List<?> selectListFromSlave(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue, int index)
            throws DaoException;

    public int update(String ibatisID, Map<String, Object> parameters, IShardValue shardValue) throws DaoException;

    public int delete(String ibatisID, Map<String, Object> parameters, IShardValue shardValue) throws DaoException;

    public List<T> selectEntities(final ISelectSql builder, IShardValue shardValue) throws DaoException;

    public List<T> selectEntitiesFromMaster(final ISelectSql builder, IShardValue shardValue) throws DaoException;

    public List<T> selectEntitiesFromSlave(final ISelectSql builder, IShardValue shardValue, int index)
            throws DaoException;

    public List<Long> selectPKs(final ISelectSql builder, IShardValue shardValue) throws DaoException;

    public List<Long> selectPKsFromMaster(final ISelectSql builder, IShardValue shardValue) throws DaoException;

    public List<Long> selectPKsFromSlave(final ISelectSql builder, IShardValue shardValue, int index)
            throws DaoException;

    public List<?> selectDistinctFromMaster(String field, ISelectSql selectSql, IShardValue shardValue)
            throws DaoException;

    public List<?> selectDistinctFromSlave(String field, ISelectSql selectSql, IShardValue shardValue, int index)
            throws DaoException;

    public List<?> selectDistinct(String field, ISelectSql selectSql, IShardValue shardValue) throws DaoException;

    public List<T> selectEntitiesByQueryCondition(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException;

    public List<T> selectEntitiesByQueryConditionFromMaster(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException;

    public List<T> selectEntitiesByQueryConditionFromSlave(final QueryCondition queryCondition, IShardValue shardValue,
            int index) throws DaoException;

    public List<Long> selectPKsByQueryCondition(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException;

    public List<Long> selectPKsByQueryConditionFromMaster(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException;

    public List<Long> selectPKsByQueryConditionFromSlave(final QueryCondition queryCondition, IShardValue shardValue,
            int index) throws DaoException;

    public T selectByPK(long id, IShardValue shardValue) throws DaoException;

    public T selectByPKFromMaster(long id, IShardValue shardValue) throws DaoException;

    public T selectByPKFromSlave(long id, IShardValue shardValue, int index) throws DaoException;

    public List<T> selectEntitiesByPKs(List<Long> ids, List<IShardValue> shardValue) throws DaoException;

    public List<T> selectEntitiesByPKsFromMaster(List<Long> ids, List<IShardValue> shardValue) throws DaoException;

    public List<T> selectEntitiesByPKsFromSlave(List<Long> ids, List<IShardValue> shardValue, int index)
            throws DaoException;

    public List<T> selectByIndex(Object indexValue, IShardValue shardValue) throws DaoException;

    public List<T> selectByIndexFromMaster(Object indexValue, IShardValue shardValue) throws DaoException;

    public List<T> selectByIndexFromSlave(Object indexValue, IShardValue shardValue, int index) throws DaoException;

    public IEntity[] selectDiffEntityByPK(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue)
            throws DaoException;

    public IEntity[] selectDiffEntityByPKFromMaster(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue)
            throws DaoException;

    public IEntity[] selectDiffEntityByPKFromSlave(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue,
            int index) throws DaoException;

    public List<IEntity[]> selectDiffEntityByPKs(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException;

    public List<IEntity[]> selectDiffEntityByPKsFromMaster(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException;

    public List<IEntity[]> selectDiffEntityByPKsFromSlave(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues, int index) throws DaoException;

    public List<IEntity>[] selectDiffEntityByIndex(Class<? extends IEntity>[] clazzs, Object id, IShardValue shardValue)
            throws DaoException;

    public List<IEntity>[] selectDiffEntityByIndexFromMaster(Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue) throws DaoException;

    public List<IEntity>[] selectDiffEntityByIndexFromSlave(Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue, int index) throws DaoException;

    public long insert(T entity, IShardValue shardValue) throws DaoException;

    public void insertBatch(List<T> entities, List<IShardValue> shardValues) throws DaoException;

    public void insertBatch2(List<T> entities, IShardValue shardValue) throws DaoException;

    public int insertOrUpdate(T entity, IShardValue shardValue) throws DaoException;

    public long update(T entity, IShardValue shardValue) throws DaoException;

    public int update(T entity, ISelectSql selectSql, IShardValue shardValue) throws DaoException;

    public void updateBatch(List<T> entities, List<IShardValue> shardValues) throws DaoException;

    public void updateBatch2(List<T> entities, IShardValue shardValue) throws DaoException;

    public void delete(long id, IShardValue shardValue) throws DaoException;

    public void deleteBatch(List<Long> entities, List<IShardValue> shardValues) throws DaoException;

    public void deleteBatch2(List<Long> entities, IShardValue shardValues) throws DaoException;

    public int incr(long id, String fieldName, IShardValue shardValue) throws DaoException;

    public int incr(long id, String fieldName, int delta, IShardValue shardValue) throws DaoException;

    public int incr(long id, String fieldName, float delta, IShardValue shardValue) throws DaoException;

    public int decr(long id, String fieldName, IShardValue shardValue) throws DaoException;

    public int decr(long id, String fieldName, int delta, IShardValue shardValue) throws DaoException;

    public int decr(long id, String fieldName, float delta, IShardValue shardValue) throws DaoException;

    public long selectRowCount(IShardValue shardValue, OptimizeSelectSqlImpl builder) throws DaoException;

    public long selectRowCountFromMaster(IShardValue shardValue, OptimizeSelectSqlImpl builder) throws DaoException;

    public long selectRowCountFromSlave(IShardValue shardValue, OptimizeSelectSqlImpl builder, int index)
            throws DaoException;

    public boolean createTableIfAbsent(IShardValue shardValue, String createSql) throws DaoException;

    public boolean dropTable(IShardValue shardValue) throws DaoException;

    public Long save(T entity, IShardValue shardValue) throws DaoException;

    public IShardingTemplateDao getTemplateDao();

}
