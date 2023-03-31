package com.liepin.swift.framework.dao.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.liepin.dao.sql.OptimizeSelectSqlImpl;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.Condition;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.ConditionAND;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.ConditionOR;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.DIRECT;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.IN;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.LIKE;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.NULL;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.ORDER;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.SIGN;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.WhereIn;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.WhereLike;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.WhereNull;
import com.liepin.dao.sql.OptimizeSelectSqlImpl.WhereSign;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.InnerCondition;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.InnerCondition.LogicCondition;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.InnerCondition.SimpleInnerCondition;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.Op;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.QueryBean;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.QuerySort;
import com.liepin.swift.framework.dao.query.AbstractQueryCondition.Sort;
import com.liepin.swift.framework.dao.query.PropertyUtils.PropertyBean;

public class QueryConditionBuilder {

    private OptimizeSelectSqlImpl builder;

    private AbstractQueryCondition qf;

    // public static final String INJ_STR =
    // "'|and|exec|insert|select|delete|update|count|*|%|chr|mid|master|truncate|char|declare|;|or|-|+|,|!";

    // public static String[] INJ_STRS;

    // static {
    // INJ_STRS = INJ_STR.split("\\|");
    // }

    public QueryConditionBuilder(AbstractQueryCondition qf) {
        builder = new OptimizeSelectSqlImpl();
        this.qf = qf;
    }

    public OptimizeSelectSqlImpl builder() {
        builder = new OptimizeSelectSqlImpl();
        buildCondition();
        buildOrderBy(qf, builder);
        // builder.setClass(qf.genericsEntityClass());
        builder.limit(qf.getStartPosition(), qf.getMaxRows());

        return builder;
    }

    // TODO:kingmxj
    public void buildCondition() {
        if (qf.getQueryBean() != null) {
            InnerCondition qc = fromQueryBean(qf.getQueryBean());
            buildCondition(qc, null, builder);
        }
        builder.where(createCondition());
    }

    public static void buildOrderBy(AbstractQueryCondition qf, OptimizeSelectSqlImpl builder) {
        if (qf.catchQuerySort() != null) {
            buildOrderBy(qf.catchQuerySort(), builder);
        }
    }

    public static class conditionFactory {

        @SuppressWarnings("unchecked")
        private static Condition appendUtil(Condition condition, SimpleInnerCondition obj) {
            //
            // //sql 注入保护
            // if(obj.getValue() != null && obj.getValue() instanceof String) {
            // if(QueryFormBuilder.hasSqlKeyword((String)obj.getValue())) {
            // obj.setValue(QueryFormBuilder.replaceKeyword((String)obj.getValue()));
            // }
            // }

            if (obj.getOp().equals(Op.EQUAL)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.EQUAL));
                }
            } else if (obj.getOp().equals(Op.UNEQUAL)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.UNEQUAL));
                }
            } else if (obj.getOp().equals(Op.BIGGER)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.BIGGER));
                }
            } else if (obj.getOp().equals(Op.SMALLER)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.SMALLER));
                }
            } else if (obj.getOp().equals(Op.BIGGER_EQUAL)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.BIGGER_EQUAL));
                }
            } else if (obj.getOp().equals(Op.SMALLER_EQUAL)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereSign(obj.getColumn(), obj.getValue(), SIGN.SMALLER_EQUAL));
                }
            } else if (obj.getOp().equals(Op.LIKE)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.MIDDLE, LIKE.LIKE));
                }
            } else if (obj.getOp().equals(Op.LEFT_LIKE)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.LEFT, LIKE.LIKE));
                }
            } else if (obj.getOp().equals(Op.RIGHT_LIKE)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.RIGHT, LIKE.LIKE));
                }
            } else if (obj.getOp().equals(Op.NOT_LIKE)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.MIDDLE,
                            LIKE.NOTLIKE));
                }
            } else if (obj.getOp().equals(Op.NOT_LEFT_LIKE)) {
                if (obj.getValue() != null) {
                    condition
                            .append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.LEFT, LIKE.NOTLIKE));
                }
            } else if (obj.getOp().equals(Op.NOT_RIGHT_LIKE)) {
                if (obj.getValue() != null) {
                    condition
                            .append(new WhereLike(obj.getColumn(), (String) obj.getValue(), DIRECT.RIGHT, LIKE.NOTLIKE));
                }
            } else if (obj.getOp().equals(Op.IN)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereIn(obj.getColumn(), (List<Integer>) obj.getValue(), IN.IN));
                }
            } else if (obj.getOp().equals(Op.NOT_IN)) {
                if (obj.getValue() != null) {
                    condition.append(new WhereIn(obj.getColumn(), (List<Integer>) obj.getValue(), IN.NOT_IN));
                }
            } else if (obj.getOp().equals(Op.NULL)) {
                if (obj.getColumn() != null && !"".equals(obj.getColumn())) {
                    condition.append(new WhereNull(obj.getColumn(), NULL.EQUAL));
                }
            } else if (obj.getOp().equals(Op.NOT_NULL)) {
                if (obj.getColumn() != null && !"".equals(obj.getColumn())) {
                    condition.append(new WhereNull(obj.getColumn(), NULL.UNEQUAL));
                }
            }
            return condition;
        }
    }

    private void buildCondition(InnerCondition queryCondition, Condition condition, OptimizeSelectSqlImpl builder) {
        List<InnerCondition> sclist = queryCondition.getQueryCoditions();
        Condition tempCodition = null;
        if (sclist != null && sclist.size() > 0) {
            for (int i = 0; i < sclist.size(); i++) {
                InnerCondition sc = sclist.get(i);
                if (sc instanceof SimpleInnerCondition) {
                    SimpleInnerCondition leaf = (SimpleInnerCondition) sc;

                    if (tempCodition == null) {
                        if (leaf.getLogic().equals(InnerCondition.LogicCondition.AND)) {
                            tempCodition = new ConditionAND();
                        } else if (leaf.getLogic().equals(InnerCondition.LogicCondition.OR)) {
                            tempCodition = new ConditionOR();
                        } else if (leaf.getLogic().equals(InnerCondition.LogicCondition.DEFAULT)) {
                            if ((i + 1) < sclist.size()) {
                                InnerCondition nextsc = sclist.get(i + 1);
                                if (nextsc.getLogic().equals(InnerCondition.LogicCondition.AND)) {
                                    tempCodition = new ConditionAND();
                                    leaf.setLogic(InnerCondition.LogicCondition.AND);
                                } else if (nextsc.getLogic().equals(InnerCondition.LogicCondition.OR)) {
                                    tempCodition = new ConditionOR();
                                    leaf.setLogic(InnerCondition.LogicCondition.OR);
                                }
                            } else {
                                tempCodition = new ConditionAND();
                                leaf.setLogic(InnerCondition.LogicCondition.AND);
                            }
                        }
                    } else {
                        if ((leaf.getLogic().equals(InnerCondition.LogicCondition.AND) && tempCodition instanceof ConditionAND)
                                || (leaf.getLogic().equals(InnerCondition.LogicCondition.OR) && tempCodition instanceof ConditionOR)) {
                        } else {
                            if (tempCodition instanceof ConditionAND) {
                                builder.and().where(tempCodition);
                            } else if (tempCodition instanceof ConditionOR) {
                                builder.or().where(tempCodition);
                            }
                            if (leaf.getLogic().equals(InnerCondition.LogicCondition.AND)) {
                                tempCodition = new ConditionAND();
                            } else if (leaf.getLogic().equals(InnerCondition.LogicCondition.OR)) {
                                tempCodition = new ConditionOR();
                            } else if (leaf.getLogic().equals(InnerCondition.LogicCondition.DEFAULT)) {
                                if ((i + 1) < sclist.size()) {
                                    InnerCondition nextsc = sclist.get(i + 1);
                                    if (nextsc.getLogic().equals(InnerCondition.LogicCondition.AND)) {
                                        tempCodition = new ConditionAND();
                                        leaf.setLogic(InnerCondition.LogicCondition.AND);
                                    } else if (nextsc.getLogic().equals(InnerCondition.LogicCondition.OR)) {
                                        tempCodition = new ConditionOR();
                                        leaf.setLogic(InnerCondition.LogicCondition.OR);
                                    }
                                } else {
                                    tempCodition = new ConditionAND();
                                    leaf.setLogic(InnerCondition.LogicCondition.AND);
                                }
                            }
                        }
                    }
                    conditionFactory.appendUtil(tempCodition, leaf);

                } else {
                    buildCondition(sc, tempCodition, builder);
                }
            }
        }

        if (condition != null && tempCodition != null) {
            if (condition instanceof ConditionAND) {
                condition.append(tempCodition);
            } else if (condition instanceof ConditionOR) {
                condition.append(tempCodition);
            }
        } else if (tempCodition != null) {
            if (tempCodition instanceof ConditionAND) {
                builder.and().where(tempCodition);
            } else if (tempCodition instanceof ConditionOR) {
                builder.or().where(tempCodition);
            }
        }
    }

    public ConditionAND createCondition() {
        ConditionAND conditionAnd = new ConditionAND();

        // PropertyDescriptor[] properties =
        // PropertyUtils.getPropertyDescriptors(qf);
        PropertyBean[] PropertyBeans = PropertyUtils.getUnConstDeclaredFields(qf);

        for (PropertyBean propertyBean : PropertyBeans) {
            Field field = propertyBean.getField();
            Class<?> clazz = propertyBean.getClazz();
            String propertyName = field.getName();
            if (propertyName.equals("class") || propertyName.endsWith("_op") || propertyName.endsWith("queryBean")
                    || propertyName.endsWith("querySort") || propertyName.endsWith("curPage")
                    || propertyName.endsWith("pageSize") || propertyName.endsWith("startPosition")
                    || propertyName.endsWith("maxRows") || propertyName.endsWith("totalRows")
                    || propertyName.endsWith("paged") || propertyName.endsWith("hasCount")
                    || propertyName.startsWith("otherInfo_"))
                continue;

            try {
                String operator = propertyName + "_op";
                Object filterValue = PropertyUtils.getPropertyByMethod(qf, field, clazz);
                String operatorValue = null;
                try {
                    Class<?> c = null;
                    for (PropertyBean p : PropertyBeans) {
                        if (p.getField().getName().equals(operator)) {
                            c = p.getClazz();
                        }
                    }
                    if (c != null) {
                        operatorValue = PropertyUtils.getPropertyByMethod(qf, operator, c).toString();
                    }
                } catch (Exception e) {
                }

                if (operatorValue == null || operatorValue.toString().equals(""))
                    operatorValue = "=";

                if (filterValue == null || filterValue.equals("")
                        || (filterValue instanceof List && ((List<?>) filterValue).size() == 0))
                    continue;

                // // sql 注入保护
                // if (filterValue != null && filterValue instanceof String) {
                // if (QueryFormBuilder.hasSqlKeyword((String) filterValue)) {
                // filterValue = QueryFormBuilder.replaceKeyword((String)
                // filterValue);
                // }
                // }

                conditionAnd.append(new WhereSign(propertyName, filterValue, SIGN.valueof(operatorValue)));
            } catch (Exception e) {
            }
        }
        return conditionAnd;
    }

    private static void buildOrderBy(QuerySort querySort, OptimizeSelectSqlImpl builder) {
        List<Map<String, Object>> sortList = querySort.getQuerySorts();
        if (sortList != null && sortList.size() > 0) {
            for (int i = 0; i < sortList.size(); i++) {
                Map<String, Object> map = sortList.get(i);
                Sort logic = null;
                if (map.get("sort") instanceof String) {
                    logic = Sort.valueOf((String) map.get("sort"));
                } else {
                    logic = (Sort) map.get("sort");
                }
                String column = (String) map.get("column");
                if (logic.equals(Sort.ASC)) {
                    builder.order_by(column, ORDER.ASC);
                } else {
                    builder.order_by(column, ORDER.DESC);
                }
            }
        }
    }

    public static QueryBean toQueryBean(InnerCondition queryCondition) {
        QueryBean bean = new QueryBean();
        bean.setClazz(queryCondition.getClazz());
        bean.setLogic(queryCondition.getLogic());
        List<QueryBean> list = new ArrayList<QueryBean>();
        toQueryBean(queryCondition, list);
        bean.setList(list);
        return bean;
    }

    private static void toQueryBean(InnerCondition queryCondition, List<QueryBean> list) {
        List<InnerCondition> sclist = queryCondition.getQueryCoditions();

        if (sclist != null && sclist.size() > 0) {
            for (int i = 0; i < sclist.size(); i++) {
                InnerCondition sc = sclist.get(i);
                QueryBean bean = new QueryBean();
                bean.setLogic(sc.getLogic());
                bean.setClazz(sc.getClazz());
                list.add(bean);
                if (sc instanceof SimpleInnerCondition) {
                    SimpleInnerCondition leaf = (SimpleInnerCondition) sc;
                    bean.setColumn(leaf.getColumn());
                    bean.setOp(leaf.getOp());
                    bean.setValue(leaf.getValue());
                } else {
                    ArrayList<QueryBean> sublist = new ArrayList<QueryBean>();
                    bean.setList(sublist);
                    toQueryBean(sc, sublist);
                }
            }
        }
    }

    public static InnerCondition fromQueryBean(QueryBean queryBean) {
        if (queryBean == null || queryBean.getList() == null || queryBean.getList().size() == 0)
            return null;
        QueryBean subQb = queryBean.getList().get(0);
        InnerCondition qc = new InnerCondition(subQb.getColumn(), subQb.getValue(), subQb.getOp());
        fromQueryBean(queryBean.getList(), qc, false);
        return qc;
    }

    private static void fromQueryBean(List<QueryBean> list, InnerCondition qc, boolean out) {
        if (list != null && list.size() > 0) {
            for (int i = 1; i < list.size(); i++) {
                QueryBean b = list.get(i);
                if (b.getClazz().equals(InnerCondition.LEAF)) {
                    if (b.getLogic().equals(LogicCondition.OR)) {
                        qc.appendOr(b.getColumn(), b.getValue(), b.getOp());
                    } else {
                        qc.appendAnd(b.getColumn(), b.getValue(), b.getOp());
                    }
                } else {
                    InnerCondition q = null;
                    if (b.getList() == null || b.getList().size() == 0) {
                        q = new InnerCondition(null, null);
                    } else {
                        QueryBean subQb = b.getList().get(0);
                        q = new InnerCondition(subQb.getColumn(), subQb.getValue(), subQb.getOp());
                    }
                    if (b.getLogic().equals(LogicCondition.OR)) {
                        qc.appendOr(q);
                    } else {
                        qc.appendAnd(q);
                    }
                    fromQueryBean(b.getList(), q, true);
                }
            }
        }
    }

    // public static boolean hasSqlKeyword(String str) {
    // for (int i = 0; i < INJ_STRS.length; i++) {
    // if (str.toLowerCase().indexOf(INJ_STRS[i]) >= 0) {
    // return true;
    // }
    // }
    //
    // return false;
    // }

    // public static String replaceKeyword(String str) {
    // String result = str;
    // for (int i = 0; i < INJ_STRS.length; i++) {
    // if (result.toLowerCase().indexOf(INJ_STRS[i]) >= 0) {
    // result = result.replaceAll(INJ_STRS[i], "");
    // }
    // }
    // return result;
    // }
}
