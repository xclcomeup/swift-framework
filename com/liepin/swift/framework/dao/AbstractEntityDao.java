package com.liepin.swift.framework.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.liepin.common.datastructure.Pair;
import com.liepin.dao.ITemplateDao;
import com.liepin.dao.entity.IEntity;
import com.liepin.dao.entity.metadata.TableMove;
import com.liepin.dao.entity.metadata.TableMove.INotify;
import com.liepin.dao.entity.metadata.TableMoveLocal;
import com.liepin.dao.exception.DaoException;
import com.liepin.dao.sql.ISelectSql;
import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.swift.framework.dao.query.QueryCondition;
import com.liepin.swift.framework.dao.query.QueryConditionBuilder;

public abstract class AbstractEntityDao<T extends IEntity> implements INotify {

    protected static final Logger logger = Logger.getLogger(AbstractEntityDao.class);

    protected Class<T> clazz;

    private ITemplateDao templateDao;
    // 表迁移使用
    private volatile boolean readyMove = false;
    private volatile AtomicReference<Pair<ITemplateDao, String>> daoReference = null;

    protected AbstractEntityDao() {
        this.clazz = getClazz(getClass().getGenericSuperclass());
        if (this.clazz == null) {
            // 原因: AOP生成代理子类没有泛型这时需要cglib类再一次获取原始类的泛型
            this.clazz = getClazz(getClass().getSuperclass().getGenericSuperclass());
        }
        if (this.clazz == null) {
            throw new RuntimeException(getClass() + " 获取继承AbstractEntityDao类的entity类失败!");
        }
        this.templateDao = DaoTool.getTemplateDao(clazz);

        readyMove = TableMove.listen(clazz, this);
    }

    protected AbstractEntityDao(String dbName) {
        this.clazz = getClazz(getClass().getGenericSuperclass());
        if (this.clazz == null) {
            // 原因: AOP生成代理子类没有泛型这时需要cglib类再一次获取原始类的泛型
            this.clazz = getClazz(getClass().getSuperclass().getGenericSuperclass());
        }
        if (this.clazz == null) {
            throw new RuntimeException(getClass() + " 获取继承EntityDao类的entity类失败!");
        }

        this.templateDao = DaoTool.getTemplateDao(dbName);
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
        ITemplateDao newTemplateDao = DaoTool.getTemplateDao(dbName);
        Pair<ITemplateDao, String> pair = new Pair<ITemplateDao, String>(newTemplateDao, dbName);
        daoReference = new AtomicReference<>(pair);
    }

    public ITemplateDao getTemplateDao() {
        // return templateDao;
        if (readyMove) {
            if (daoReference != null) {
                Pair<ITemplateDao, String> pair = daoReference.get();
                TableMoveLocal.getInstance().setJndiName(pair.getSecond());
                return pair.getFirst();
            } else {
                return templateDao;
            }
        } else {
            return templateDao;
        }
    }

    public int update(String statement, Object parameter) throws DaoException {
        return getTemplateDao().update(statement, parameter);
    }

    public int delete(String statement, Object parameter) throws DaoException {
        return getTemplateDao().delete(statement, parameter);
    }

    public int insert(String statement, Object parameter) throws DaoException {
        return getTemplateDao().insert(statement, parameter);
    }

    public Object selectOne(String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao().selectOne(ibatisID, whereMap);
    }

    public Object selectOneFromMaster(String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao().selectOneFromMaster(ibatisID, whereMap);
    }

    public Object selectOneFromSlave(String ibatisID, Map<String, Object> whereMap, int index) throws DaoException {
        return getTemplateDao().selectOneFromSlave(ibatisID, whereMap, index);
    }

    public List<?> selectList(String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao().selectList(ibatisID, whereMap);
    }

    public List<?> selectListFromMaster(String ibatisID, Map<String, Object> whereMap) throws DaoException {
        return getTemplateDao().selectListFromMaster(ibatisID, whereMap);
    }

    public List<?> selectListFromSlave(String ibatisID, Map<String, Object> whereMap, int index) throws DaoException {
        return getTemplateDao().selectListFromSlave(ibatisID, whereMap, index);
    }

    public List<T> selectEntities(final ISelectSql builder) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntities(clazz, builder);
        return mapping(list);
    }

    public List<T> selectEntitiesFromMaster(final ISelectSql builder) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesFromMaster(clazz, builder);
        return mapping(list);
    }

    public List<T> selectEntitiesFromSlave(final ISelectSql builder, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesFromSlave(clazz, builder, index);
        return mapping(list);
    }

    public List<T> selectEntitiesInDbFromMaster(final ISelectSql selectSql) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesInDbFromMaster(clazz, selectSql);
        return mapping(list);
    }

    public List<T> selectEntitiesInDbFromSlave(final ISelectSql selectSql, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesInDbFromSlave(clazz, selectSql, index);
        return mapping(list);
    }

    public List<T> selectEntitiesInDb(final ISelectSql selectSql) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesInDb(clazz, selectSql);
        return mapping(list);
    }

    public List<Long> selectPKs(final ISelectSql builder) throws DaoException {
        return getTemplateDao().selectPKs(clazz, builder);
    }

    public List<Long> selectPKsFromMaster(final ISelectSql builder) throws DaoException {
        return getTemplateDao().selectPKsFromMaster(clazz, builder);
    }

    public List<Long> selectPKsFromSlave(final ISelectSql builder, int index) throws DaoException {
        return getTemplateDao().selectPKsFromSlave(clazz, builder, index);
    }

    public List<?> selectDistinctFromMaster(String field, ISelectSql selectSql) throws DaoException {
        return getTemplateDao().selectDistinctFromMaster(clazz, field, selectSql);
    }

    public List<?> selectDistinctFromSlave(String field, ISelectSql selectSql, int index) throws DaoException {
        return getTemplateDao().selectDistinctFromSlave(clazz, field, selectSql, index);
    }

    public List<?> selectDistinct(String field, ISelectSql selectSql) throws DaoException {
        return getTemplateDao().selectDistinct(clazz, field, selectSql);
    }

    public List<T> selectEntitiesByQueryCondition(final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntities(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(builder));
        }
        return mapping(list);
    }

    public List<T> selectEntitiesByQueryConditionFromMaster(final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntitiesFromMaster(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(builder));
        }
        return mapping(list);
    }

    public List<T> selectEntitiesByQueryConditionFromSlave(final QueryCondition queryCondition, int index)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<IEntity> list = getTemplateDao().selectEntitiesFromSlave(clazz, builder, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(builder, index));
        }
        return mapping(list);
    }

    public List<Long> selectPKsByQueryCondition(final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKs(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCount(builder));
        }
        return ids;
    }

    public List<Long> selectPKsByQueryConditionFromMaster(final QueryCondition queryCondition) throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKsFromMaster(clazz, builder);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromMaster(builder));
        }
        return ids;
    }

    public List<Long> selectPKsByQueryConditionFromSlave(final QueryCondition queryCondition, int index)
            throws DaoException {
        QueryConditionBuilder qfb = new QueryConditionBuilder(queryCondition);
        OptimizeSelectSqlImpl builder = qfb.builder();
        List<Long> ids = getTemplateDao().selectPKsFromSlave(clazz, builder, index);
        if (queryCondition.isHasCount()) {
            queryCondition.setTotalRows(selectRowCountFromSlave(builder, index));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public T selectByPK(long id) throws DaoException {
        return (T) getTemplateDao().selectByPK(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public T selectByPKFromMaster(long id) throws DaoException {
        return (T) getTemplateDao().selectByPKFromMaster(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public T selectByPKFromSlave(long id, int index) throws DaoException {
        return (T) getTemplateDao().selectByPKFromSlave(clazz, id, index);
    }

    public List<T> selectEntitiesByPKs(List<Long> ids) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKs(clazz, ids);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsFromMaster(List<Long> ids) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsFromMaster(clazz, ids);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsFromSlave(List<Long> ids, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsFromSlave(clazz, ids, index);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsWithSortFromMaster(List<Long> ids) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsWithSortFromMaster(clazz, ids);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsWithSortFromSlave(List<Long> ids, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsWithSortFromSlave(clazz, ids, index);
        return mapping(list);
    }

    public List<T> selectEntitiesByPKsWithSort(List<Long> ids) throws DaoException {
        List<IEntity> list = getTemplateDao().selectEntitiesByPKsWithSort(clazz, ids);
        return mapping(list);
    }

    public List<T> selectByIndex(Object id) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndex(clazz, id);
        return mapping(list);
    }

    public List<T> selectByIndexFromMaster(Object id) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndexFromMaster(clazz, id);
        return mapping(list);
    }

    public List<T> selectByIndexFromSlave(Object id, int index) throws DaoException {
        List<IEntity> list = getTemplateDao().selectByIndexFromSlave(clazz, id, index);
        return mapping(list);
    }

    public long selectRowCount(final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCount(builder);
    }

    public long selectRowCountFromMaster(final OptimizeSelectSqlImpl builder) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCountFromMaster(builder);
    }

    public long selectRowCountFromSlave(final OptimizeSelectSqlImpl builder, int index) throws DaoException {
        builder.setClass(clazz);
        return getTemplateDao().selectRowCountFromSlave(builder, index);
    }

    public long insert(T entity) throws DaoException {
        return getTemplateDao().insert(entity);
    }

    @SuppressWarnings("unchecked")
    public List<Long> insertBatch(List<T> entities) throws DaoException {
        return getTemplateDao().insertBatch((List<IEntity>) entities);
    }

    public int insertOrUpdate(T entity) throws DaoException {
        return getTemplateDao().insertOrUpdate(entity);
    }

    public long generatedId(String tableName) throws DaoException {
        return getTemplateDao().generatedId(tableName);
    }

    public int update(T entity) throws DaoException {
        return getTemplateDao().update(entity);
    }

    public int update(T entity, ISelectSql selectSql) throws DaoException {
        return getTemplateDao().update(entity, selectSql);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> updateBatch(List<T> entities) throws DaoException {
        int[] ids = getTemplateDao().updateBatch((List<IEntity>) entities);
        List<Integer> _ids = new ArrayList<Integer>();
        for (int i : ids) {
            _ids.add(i);
        }
        return _ids;
    }

    public int incr(long id, String fieldName) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName);
    }

    public int incr(long id, String fieldName, int delta) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName, delta);
    }

    public int incr(long id, String fieldName, float delta) throws DaoException {
        return getTemplateDao().incr(clazz, id, fieldName, delta);
    }

    public int decr(long id, String fieldName) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName);
    }

    public int decr(long id, String fieldName, int delta) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName, delta);
    }

    public int decr(long id, String fieldName, float delta) throws DaoException {
        return getTemplateDao().decr(clazz, id, fieldName, delta);
    }

    public void delete(long id) throws DaoException {
        getTemplateDao().delete(clazz, id);
    }

    public void deleteBatch(List<Long> ids) throws DaoException {
        getTemplateDao().deleteBatch(clazz, ids);
    }

    public Long save(T entity) throws DaoException {
        if (entity.pkValue() == null) {
            return insert(entity);
        } else {
            if (update(entity) > 0) {
                return entity.pkValue();
            }
            return 0L;
        }
    }

    public boolean createTableIfAbsent(String createSql) throws DaoException {
        return getTemplateDao().createTableIfAbsent(clazz, createSql);
    }

}
