package com.liepin.swift.framework.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.liepin.common.datastructure.Pair;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.entity.metadata.TableMove;
import com.liepin.dao.entity.metadata.TableMove.INotify;
import com.liepin.dao.entity.metadata.TableMoveLocal;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sharding.IShardValue;
import com.liepin.dao.sharding.IShardingTemplateDao;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;
import com.liepin.swift.framework.dao.query.QueryConditionBuilder;

public abstract class AbstractShardingEntityDao<T extends IEntity> implements INotify {

    protected static final Logger logger = Logger.getLogger(AbstractShardingEntityDao.class);

    protected Class<T> clazz;

    private volatile IShardingTemplateDao shardingTemplateDao;
    // 表迁移使用
    private volatile boolean readyMove = false;
    private volatile AtomicReference<Pair<IShardingTemplateDao, String>> daoReference = null;

    protected AbstractShardingEntityDao() {
        this.clazz = getClazz(getClass().getGenericSuperclass());
        if (this.clazz == null) {
            // 原因: AOP生成代理子类没有泛型这时需要cglib类再一次获取原始类的泛型
            this.clazz = getClazz(getClass().getSuperclass().getGenericSuperclass());
        }
        if (this.clazz == null) {
            throw new RuntimeException(getClass() + " 获取继承AbstractShardingEntityDao类的entity类失败!");
        }
        this.shardingTemplateDao = DaoTool.getShardingTemplateDao(clazz);

        readyMove = TableMove.listen(clazz, this);
    }

    protected AbstractShardingEntityDao(String dbName) {
        this.clazz = getClazz(getClass().getGenericSuperclass());
        if (this.clazz == null) {
            // 原因: AOP生成代理子类没有泛型这时需要cglib类再一次获取原始类的泛型
            this.clazz = getClazz(getClass().getSuperclass().getGenericSuperclass());
        }
        if (this.clazz == null) {
            throw new RuntimeException(getClass() + " 获取继承AbstractShardingEntityDao类的entity类失败!");
        }
        this.shardingTemplateDao = DaoTool.getShardingTemplateDao(dbName);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getClazz(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] array = ((ParameterizedType) type).getActualTypeArguments();
            return (Class<T>) array[0];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<T> mapping(List<IEntity> list) {
        List<T> ret = new ArrayList<T>();
        for (IEntity iEntity : list) {
            ret.add((T) iEntity);
        }
        return ret;
    }

    @Override
    public void handle(String dbName) {
        IShardingTemplateDao newShardingTemplateDao = DaoTool.getShardingTemplateDao(dbName);
        Pair<IShardingTemplateDao, String> pair = new Pair<IShardingTemplateDao, String>(newShardingTemplateDao,
                dbName);
        daoReference = new AtomicReference<>(pair);
    }

    public IShardingTemplateDao getTemplateDao() {
        // return shardingTemplateDao;
        if (readyMove) {
            if (daoReference != null) {
                Pair<IShardingTemplateDao, String> pair = daoReference.get();
                TableMoveLocal.getInstance().setJndiName(pair.getSecond());
                return pair.getFirst();
            } else {
                return shardingTemplateDao;
            }
        } else {
            return shardingTemplateDao;
        }
    }

    public Object selectOne(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue) throws DaoException {
        return getTemplateDao().selectOne(ibatisID, whereMap, shardValue);
    }

    public Object selectOneFromMaster(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectOneFromMaster(ibatisID, whereMap, shardValue);
    }

    public Object selectOneFromSlave(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue, int index)
            throws DaoException {
        return getTemplateDao().selectOneFromSlave(ibatisID, whereMap, shardValue, index);
    }

    public List<?> selectList(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectList(ibatisID, whereMap, shardValue);
    }

    public List<?> selectListFromMaster(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectListFromMaster(ibatisID, whereMap, shardValue);
    }

    public List<?> selectListFromSlave(String ibatisID, Map<String, Object> whereMap, IShardValue shardValue, int index)
            throws DaoException {
        return getTemplateDao().selectListFromSlave(ibatisID, whereMap, shardValue, index);
    }

    public int update(String ibatisID, Map<String, Object> parameters, IShardValue shardValue) throws DaoException {
        return getTemplateDao().update(ibatisID, parameters, shardValue);
    }

    public int delete(String ibatisID, Map<String, Object> parameters, IShardValue shardValue) throws DaoException {
        return getTemplateDao().delete(ibatisID, parameters, shardValue);
    }

    public List<T> selectEntities(final ISelectSql builder, IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntities(clazz, builder, shardValue);
        return mapping(list);
    }

    public List<T> selectEntitiesFromMaster(final ISelectSql builder, IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesFromMaster(clazz, builder, shardValue);
        return mapping(list);
    }

    public List<T> selectEntitiesFromSlave(final ISelectSql builder, IShardValue shardValue, int index)
            throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesFromSlave(clazz, builder, shardValue, index);
        return mapping(list);
    }

    public List<Long> selectPKs(final ISelectSql builder, IShardValue shardValue) throws DaoException {
        return getTemplateDao().selectPKs(clazz, builder, shardValue);
    }

    public List<Long> selectPKsFromMaster(final ISelectSql builder, IShardValue shardValue) throws DaoException {
        return getTemplateDao().selectPKsFromMaster(clazz, builder, shardValue);
    }

    public List<Long> selectPKsFromSlave(final ISelectSql builder, IShardValue shardValue, int index)
            throws DaoException {
        return getTemplateDao().selectPKsFromSlave(clazz, builder, shardValue, index);
    }

    public List<?> selectDistinctFromMaster(String field, ISelectSql selectSql, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectDistinctFromMaster(clazz, field, selectSql, shardValue);
    }

    public List<?> selectDistinctFromSlave(String field, ISelectSql selectSql, IShardValue shardValue, int index)
            throws DaoException {
        return getTemplateDao().selectDistinctFromSlave(clazz, field, selectSql, shardValue, index);
    }

    public List<?> selectDistinct(String field, ISelectSql selectSql, IShardValue shardValue) throws DaoException {
        return getTemplateDao().selectDistinct(clazz, field, selectSql, shardValue);
    }

    public List<T> selectEntitiesByQueryCondition(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntities(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(shardValue, builder));
        }
        return mapping(list);
    }

    public List<T> selectEntitiesByQueryConditionFromMaster(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntitiesFromMaster(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(shardValue, builder));
        }
        return mapping(list);
    }

    public List<T> selectEntitiesByQueryConditionFromSlave(final QueryCondition queryCondition, IShardValue shardValue,
            int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntitiesFromSlave(clazz, builder, shardValue, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(shardValue, builder, index));
        }
        return mapping(list);
    }

    public List<Long> selectPKsByQueryCondition(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKs(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(shardValue, builder));
        }
        return ids;
    }

    public List<Long> selectPKsByQueryConditionFromMaster(final QueryCondition queryCondition, IShardValue shardValue)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKsFromMaster(clazz, builder, shardValue);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(shardValue, builder));
        }
        return ids;
    }

    public List<Long> selectPKsByQueryConditionFromSlave(final QueryCondition queryCondition, IShardValue shardValue,
            int index) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKsFromSlave(clazz, builder, shardValue, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(shardValue, builder, index));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public T selectByPK(long id, IShardValue shardValue) throws DaoException {
        return (T) getTemplateDao().selectByPK(clazz, id, shardValue);
    }

    @SuppressWarnings("unchecked")
    public T selectByPKFromMaster(long id, IShardValue shardValue) throws DaoException {
        return (T) getTemplateDao().selectByPKFromMaster(clazz, id, shardValue);
    }

    @SuppressWarnings("unchecked")
    public T selectByPKFromSlave(long id, IShardValue shardValue, int index) throws DaoException {
        return (T) getTemplateDao().selectByPKFromSlave(clazz, id, shardValue, index);
    }

    public List<T> selectEntitiesByPKs(List<Long> ids, List<IShardValue> shardValues) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKs(clazz, ids, shardValues);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsFromMaster(List<Long> ids, List<IShardValue> shardValues) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsFromMaster(clazz, ids, shardValues);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsFromSlave(List<Long> ids, List<IShardValue> shardValues, int index)
            throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsFromSlave(clazz, ids, shardValues, index);
        return mapping(list);
    }

    public List<T> selectByIndex(Object indexValue, IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndex(clazz, indexValue, shardValue);
        return mapping(list);
    }

    public List<T> selectByIndexFromMaster(Object indexValue, IShardValue shardValue) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndexFromMaster(clazz, indexValue, shardValue);
        return mapping(list);
    }

    public List<T> selectByIndexFromSlave(Object indexValue, IShardValue shardValue, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndexFromSlave(clazz, indexValue, shardValue, index);
        return mapping(list);
    }

    public IEntity[] selectDiffEntityByPK(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectDiffEntityByPK(clazzs, id, shardValue);
    }

    public IEntity[] selectDiffEntityByPKFromMaster(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectDiffEntityByPKFromMaster(clazzs, id, shardValue);
    }

    public IEntity[] selectDiffEntityByPKFromSlave(Class<? extends IEntity>[] clazzs, long id, IShardValue shardValue,
            int index) throws DaoException {
        return getTemplateDao().selectDiffEntityByPKFromSlave(clazzs, id, shardValue, index);
    }

    public List<IEntity[]> selectDiffEntityByPKs(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        return getTemplateDao().selectDiffEntityByPKs(clazzs, ids, shardValues);
    }

    public List<IEntity[]> selectDiffEntityByPKsFromMaster(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues) throws DaoException {
        return getTemplateDao().selectDiffEntityByPKsFromMaster(clazzs, ids, shardValues);
    }

    public List<IEntity[]> selectDiffEntityByPKsFromSlave(Class<? extends IEntity>[] clazzs, List<Long> ids,
            List<IShardValue> shardValues, int index) throws DaoException {
        return getTemplateDao().selectDiffEntityByPKsFromSlave(clazzs, ids, shardValues, index);
    }

    public List<IEntity>[] selectDiffEntityByIndex(Class<? extends IEntity>[] clazzs, Object id, IShardValue shardValue)
            throws DaoException {
        return getTemplateDao().selectDiffEntityByIndex(clazzs, id, shardValue);
    }

    public List<IEntity>[] selectDiffEntityByIndexFromMaster(Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue) throws DaoException {
        return getTemplateDao().selectDiffEntityByIndexFromMaster(clazzs, id, shardValue);
    }

    public List<IEntity>[] selectDiffEntityByIndexFromSlave(Class<? extends IEntity>[] clazzs, Object id,
            IShardValue shardValue, int index) throws DaoException {
        return getTemplateDao().selectDiffEntityByIndexFromSlave(clazzs, id, shardValue, index);
    }

    public long insert(T entity, IShardValue shardValue) throws DaoException {
        return getTemplateDao().insert(entity, shardValue);
    }

    @SuppressWarnings("unchecked")
    public void insertBatch(List<T> entities, List<IShardValue> shardValues) throws DaoException {
        getTemplateDao().insertBatch((List<IEntity>) entities, shardValues);
    }

    @SuppressWarnings("unchecked")
    public void insertBatch2(List<T> entities, IShardValue shardValue) throws DaoException {
        getTemplateDao().insertBatch2((List<IEntity>) entities, shardValue);
    }

    public int insertOrUpdate(T entity, IShardValue shardValue) throws DaoException {
        return getTemplateDao().insertOrUpdate(entity, shardValue);
    }

    /**
     * 返回的是entity的主键，而不是影响行数
     * 
     * @param entity
     * @param shardValue
     * @return
     * @throws DaoException
     */
    public long update(T entity, IShardValue shardValue) throws DaoException {
        getTemplateDao().update(entity, shardValue);
        return entity.pkValue();
    }

    /**
     * 返回的是影响行数
     * 
     * @param entity
     * @param shardValue
     * @return
     * @throws DaoException
     */
    public int updateAndReturnRows(T entity, IShardValue shardValue) throws DaoException {
        return getTemplateDao().update(entity, shardValue);
    }

    public int update(T entity, ISelectSql selectSql, IShardValue shardValue) throws DaoException {
        return getTemplateDao().update(entity, selectSql, shardValue);
    }

    @SuppressWarnings("unchecked")
    public void updateBatch(List<T> entities, List<IShardValue> shardValues) throws DaoException {
        getTemplateDao().updateBatch((List<IEntity>) entities, shardValues);
    }

    @SuppressWarnings("unchecked")
    public void updateBatch2(List<T> entities, IShardValue shardValue) throws DaoException {
        getTemplateDao().updateBatch2((List<IEntity>) entities, shardValue);
    }

    public void delete(long id, IShardValue shardValue) throws DaoException {
        getTemplateDao().delete(clazz, id, shardValue);
    }

    public void deleteBatch(List<Long> ids, List<IShardValue> shardValues) throws DaoException {
        getTemplateDao().deleteBatch(clazz, ids, shardValues);
    }

    public void deleteBatch2(List<Long> ids, IShardValue shardValue) throws DaoException {
        getTemplateDao().deleteBatch2(clazz, ids, shardValue);
    }

    public int incr(long id, String fieldName, IShardValue shardValue) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName, shardValue);
    }

    public int incr(long id, String fieldName, int delta, IShardValue shardValue) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName, delta, shardValue);
    }

    public int incr(long id, String fieldName, float delta, IShardValue shardValue) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName, delta, shardValue);
    }

    public int decr(long id, String fieldName, IShardValue shardValue) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName, shardValue);
    }

    public int decr(long id, String fieldName, int delta, IShardValue shardValue) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName, delta, shardValue);
    }

    public int decr(long id, String fieldName, float delta, IShardValue shardValue) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName, delta, shardValue);
    }

    public long selectRowCount(IShardValue shardValue, final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCount(builder, shardValue);
    }

    public long selectRowCountFromMaster(IShardValue shardValue, final OptimizeSelectSqlImpl builder)
            throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCountFromMaster(builder, shardValue);
    }

    public long selectRowCountFromSlave(IShardValue shardValue, final OptimizeSelectSqlImpl builder, int index)
            throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCountFromSlave(builder, shardValue, index);
    }

    public boolean createTableIfAbsent(IShardValue shardValue, String createSql) throws DaoException {
        return getTemplateDao().createTableIfAbsent(clazz, shardValue, createSql);
    }

    public boolean dropTable(IShardValue shardValue) throws DaoException {
        return getTemplateDao().dropTable(clazz, shardValue);
    }

    public Long save(T entity, IShardValue shardValue) throws DaoException {
        if (entity.pkValue() == null) {
            return insert(entity, shardValue);
        } else {
            if (update(entity, shardValue) > 0) {
                return entity.pkValue();
            }
            return 0L;
        }
    }

}
