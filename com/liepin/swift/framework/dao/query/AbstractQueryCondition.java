package com.liepin.swift.framework.dao.query;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import com.liepin.swift.framework.dao.query.AbstractQueryCondition.InnerCondition.LogicCondition;

@SuppressWarnings("serial")
public abstract class AbstractQueryCondition extends AbstractPageCondition {

    private QueryBean queryBean;

    private QuerySort querySort;

    public QueryBean getQueryBean() {
        return queryBean;
    }

    public void setQueryBean(QueryBean queryBean) {
        this.queryBean = queryBean;
    }

    public QuerySort getQuerySort() {
        return querySort;
    }

    public QuerySort catchQuerySort() {
        return querySort;
    }

    public void setQueryCondition(InnerCondition queryCondition) {
        if (queryCondition == null) {
            this.queryBean = null;
        } else {
            this.queryBean = QueryConditionBuilder.toQueryBean(queryCondition);
        }
    }

    public InnerCondition catchQueryCondition() {
        return QueryConditionBuilder.fromQueryBean(queryBean);
    }

    public Class<?> genericsEntityClass() {
        int classIndex = 0;
        Type genType = getClass().getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (classIndex >= params.length || classIndex < 0) {
            throw new RuntimeException("Index outof bounds");
        }
        if (!(params[classIndex] instanceof Class)) {
            return Object.class;
        }
        return (Class<?>) params[classIndex];
    }

    public int getStartPosition() {
        calculate();
        return super.getStartPosition();
    }

    public int getMaxRows() {
        return super.getMaxRows();
    }

    public void setTotalRows(long selectRowCount) {
        super.setTotalRows(selectRowCount);
    }

    /**
     * 添加查询条件
     * 
     * @param searchCondition
     */
    public void addQueryCondition(InnerCondition searchCondition) {
        if (queryBean != null) {
            InnerCondition q = QueryConditionBuilder.fromQueryBean(queryBean);
            q.appendAnd(searchCondition);
            this.queryBean = QueryConditionBuilder.toQueryBean(q);
        } else {
            this.queryBean = QueryConditionBuilder.toQueryBean(searchCondition);
        }
    }


    public Map<String, Object> createConditionMap() {
        Map<String, Object> condition = new HashMap<String, Object>();
        PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(this);

        for (PropertyDescriptor descriptor : properties) {
            String propertyName = descriptor.getName();
            if (propertyName.equals("class") || propertyName.endsWith("_op") || propertyName.endsWith("curPage")
                    || propertyName.endsWith("pageSize") || propertyName.endsWith("startPosition")
                    || propertyName.endsWith("maxRows") || propertyName.endsWith("totalRows")
                    || propertyName.endsWith("paged"))
                continue;
            if (descriptor.getReadMethod() == null)
                continue;
            try {
                Object filterValue = PropertyUtils.getProperty(this, propertyName);
                if (filterValue != null && !filterValue.equals("") && !filterValue.equals("()"))
                    condition.put(propertyName, filterValue);
            } catch (Exception e) {
                throw new RuntimeException("AbstractQueryForm createConditionMap fail", e);
            }

        }

        return condition;
    }


    /**
     * 放入排序
     * 
     * @param querySort
     */
    public void setQuerySort(QuerySort querySort) {
        this.querySort = querySort;
    }

    /**
     * 添加排序,要先setQuerySort,才能调用这个方法
     * 
     * @param column
     */
    public void addQuerySort(String column) {
        if (this.querySort == null) {
            this.querySort = new QuerySort(column, AbstractQueryCondition.Sort.ASC);
        } else {
            this.querySort.appendAnd(column, AbstractQueryCondition.Sort.ASC);
        }
    }

    /**
     * 添加排序,要先setQuerySort,才能调用这个方法
     * 
     * @param column
     * @param sort
     */
    public void addQuerySort(String column, Sort sort) {
        if (this.querySort == null) {
            this.querySort = new QuerySort(column, sort);
        } else {
            this.querySort.appendAnd(column, sort);
        }
    }

    /**
     * 查询条件内部类
     * 
     * @author Administrator
     * 
     */
    public static class InnerCondition implements Serializable {

        private static final long serialVersionUID = 1438371620057829149L;

        public static final String BRANCH = "BRANCH";
        public static final String LEAF = "LEAF";

        ArrayList<InnerCondition> queryCoditions;
        private LogicCondition logic = LogicCondition.AND;
        protected String clazz = BRANCH;


        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        private InnerCondition() {
        }

        public InnerCondition(String column, Object value) {
            this(column, value, Op.EQUAL);
        }

        public InnerCondition(String column, Object value, Op op) {
            queryCoditions = new ArrayList<InnerCondition>();
            queryCoditions.add(new SimpleInnerCondition(column, value, op, LogicCondition.DEFAULT));
        }

        /**
         * 预加载的查询条件 例如：'state=1,deleted=0' 目前只支持等于并且是数字
         * 
         * @param con
         * @param spl ','
         * @param eql '='
         * @param op
         */
        public InnerCondition(String con, String spl, String eql, Op op) {
            queryCoditions = new ArrayList<InnerCondition>();
            if (con.indexOf(spl) != -1) {
                String[] ss = con.split(spl);
                for (String s : ss) {
                    String[] cs = s.split(eql);
                    queryCoditions.add(new SimpleInnerCondition(cs[0], cs[1], op, LogicCondition.AND));
                }
            } else {
                String[] cs = con.split(eql);
                queryCoditions
                        .add(new SimpleInnerCondition(cs[0], Integer.parseInt(cs[1]), op, LogicCondition.DEFAULT));
            }
        }

        public void setLogic(LogicCondition logic) {
            this.logic = logic;
        }

        public InnerCondition appendAnd(String column, Object value) {
            return appendAnd(column, value, Op.EQUAL);
        }

        public InnerCondition appendAnd(String column, Object value, Op op) {
            queryCoditions.add(new SimpleInnerCondition(column, value, op, LogicCondition.AND));
            return this;
        }

        public InnerCondition appendAnd(String column, Object[] value) {
            return appendAnd(column, value, Op.EQUAL);
        }

        public InnerCondition appendAnd(String column, Object[] value, Op op) {
            queryCoditions.add(new SimpleInnerCondition(column, value, op, LogicCondition.AND));
            return this;
        }

        public InnerCondition appendOr(String column, Object value) {
            return appendOr(column, value, Op.EQUAL);
        }

        public InnerCondition appendOr(String column, Object value, Op op) {
            queryCoditions.add(new SimpleInnerCondition(column, value, op, LogicCondition.OR));
            return this;
        }

        public InnerCondition appendOr(String column, Object[] value) {
            return appendOr(column, value, Op.EQUAL);
        }

        public InnerCondition appendOr(String column, Object[] value, Op op) {
            queryCoditions.add(new SimpleInnerCondition(column, value, op, LogicCondition.OR));
            return this;
        }

        public InnerCondition appendAnd(InnerCondition searchCondition) {
            searchCondition.logic = LogicCondition.AND;
            queryCoditions.add(searchCondition);
            return this;
        }

        public InnerCondition appendOr(InnerCondition searchCondition) {
            searchCondition.logic = LogicCondition.OR;
            queryCoditions.add(searchCondition);
            return this;
        }


        public ArrayList<InnerCondition> getQueryCoditions() {
            return queryCoditions;
        }

        public void setQueryCoditions(ArrayList<InnerCondition> queryCoditions) {
            this.queryCoditions = queryCoditions;
        }

        public LogicCondition getLogic() {
            return logic;
        }

        public class SimpleInnerCondition extends InnerCondition {

            private static final long serialVersionUID = -2567125503671602270L;

            private String column;
            private Object value;
            private Op op;
            protected String clazz = LEAF;

            public SimpleInnerCondition(String column, Object value, Op op, LogicCondition logic) {
                this.column = column;
                this.value = value;
                this.op = op;
                super.logic = logic;
            }

            public String getColumn() {
                return column;
            }

            public Object getValue() {
                return value;
            }

            public Op getOp() {
                return op;
            }

            public void setColumn(String column) {
                this.column = column;
            }

            public void setValue(Object value) {
                this.value = value;
            }

            public void setOp(Op op) {
                this.op = op;
            }

            public String getClazz() {
                return clazz;
            }

            public void setClazz(String clazz) {
                this.clazz = clazz;
            }

        }

        static enum LogicCondition {
            AND, OR, DEFAULT;
        }
    }

    public static enum Op {
        EQUAL("="), UNEQUAL("!="), BIGGER(">"), SMALLER("<"), BIGGER_EQUAL(">="), SMALLER_EQUAL("<="), LIKE("LIKE"),
        LEFT_LIKE("LIKE"), RIGHT_LIKE("LIKE"), NOT_LIKE("NOT LIKE"), NOT_LEFT_LIKE("NOT LIKE"), NOT_RIGHT_LIKE(
                "NOT LIKE"), IN("IN"), NOT_IN("NOT IN"), NULL("IS NULL"), NOT_NULL("IS NOT NULL");

        private final String value;

        private Op(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 排序内部类
     * 
     * @author kingmxj 2012-03-13
     * 
     */
    public static class QuerySort implements Serializable {

        private static final long serialVersionUID = -6366994330140374393L;
        LinkedList<Map<String, Object>> querySorts;
        private String column;
        private Sort logic = Sort.ASC;

        public QuerySort(String column) {
            this(column, Sort.ASC);
        }

        public QuerySort(String column, Sort logicSort) {
            querySorts = new LinkedList<Map<String, Object>>();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("column", column);
            map.put("sort", logicSort);
            querySorts.add(map);
        }

        public QuerySort appendAnd(String column, Sort logicSort) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("column", column);
            map.put("sort", logicSort);
            querySorts.add(map);
            return this;
        }

        public QuerySort appendAnd(QuerySort querySort) {
            List<Map<String, Object>> list = querySort.getQuerySorts();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = (Map<String, Object>) list.get(i);
                    querySorts.add(map);
                }
            }
            return this;
        }


        public LinkedList<Map<String, Object>> getQuerySorts() {
            return querySorts;
        }

        public Sort getLogic() {
            return logic;
        }

        public void setLogic(Sort logic) {
            this.logic = logic;
        }

        public String getColumn() {
            return column;
        }

        public void setQuerySorts(LinkedList<Map<String, Object>> querySorts) {
            this.querySorts = querySorts;
        }

        public void setColumn(String column) {
            this.column = column;
        }
    }

    public static enum Sort {
        ASC, DESC;
    }

    public static class QueryBean {
        List<QueryBean> list;
        private LogicCondition logic;
        protected String clazz;
        private String column;
        private Object value;
        private Op op;

        public List<QueryBean> getList() {
            return list;
        }

        public void setList(List<QueryBean> list) {
            this.list = list;
        }

        public LogicCondition getLogic() {
            return logic;
        }

        public void setLogic(LogicCondition logic) {
            this.logic = logic;
        }

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Op getOp() {
            return op;
        }

        public void setOp(Op op) {
            this.op = op;
        }

    }

}
