package com.liepin.swift.framework.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean.ParamBean;

public class TypeUtil {

    /**
     * 递归获取Type类型的范型信息
     * <p>
     * 注意array、Map、Collection类型不会递归下去，只取一层
     * 
     * @param root
     * @param list
     */
    public static void recursiveParamClasses(Type root, final List<Class<?>> list) {
        if (root instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) root;
            Class<?> outClass = (Class<?>) parameterizedType.getRawType();
            if (list.size() > 0) {
                if (Map.class.isAssignableFrom(list.get(0)) || Collection.class.isAssignableFrom(list.get(0))) {
                    list.add(outClass);
                    return;
                }
            }
            list.add(outClass);
            Type[] array = parameterizedType.getActualTypeArguments();
            for (Type type : array) {
                recursiveParamClasses(type, list);
            }
        } else if (root instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) root;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            Object newInstance = Array.newInstance((Class<?>) genericComponentType, 0);
            list.add(newInstance.getClass());
        } else if (root instanceof WildcardType) {
            // 不处理
            // WildcardType wildcardType = (WildcardType) root;
            // Type[] lowerBounds = wildcardType.getLowerBounds();
            // Type[] upperBounds = wildcardType.getUpperBounds();
        } else {
            list.add((Class<?>) root);
        }
    }

    private static final Map<Class<?>, List<Field>> fieldCache = new HashMap<Class<?>, List<Field>>();
    private static final Map<Type, ParamBean> cache = new HashMap<Type, ParamBean>();
    private static final Map<Class<?>, ParamBean> cache1 = new HashMap<Class<?>, ParamBean>();
    private static final Map<Class<?>, Class<?>> primitiveCache = new HashMap<Class<?>, Class<?>>();

    static {
        primitiveCache.put(int.class, Integer.class);
        primitiveCache.put(boolean.class, Boolean.class);
        primitiveCache.put(byte.class, Byte.class);
        primitiveCache.put(short.class, Short.class);
        primitiveCache.put(long.class, Long.class);
        primitiveCache.put(float.class, Float.class);
        primitiveCache.put(double.class, Double.class);
    }

    private static ParamBean fromType(Type type) {
        ParamBean paramBean = cache.get(type);
        if (paramBean == null) {
            synchronized (cache) {
                paramBean = cache.get(type);
                if (paramBean == null) {
                    paramBean = new ParamBean();
                    List<Class<?>> list = new ArrayList<Class<?>>();
                    TypeUtil.recursiveParamClasses(type, list);
                    paramBean.parametrized = list.get(0);
                    if (list.size() > 1) {
                        List<Class<?>> subList = list.subList(1, list.size());
                        paramBean.parameterClasses = subList.toArray(new Class<?>[] {});
                    }
                    cache.put(type, paramBean);
                }
            }
        }
        return paramBean;
    }

    private static ParamBean fromClass(Class<?> clazz) {
        ParamBean paramBean = cache1.get(clazz);
        if (paramBean == null) {
            synchronized (cache1) {
                paramBean = cache1.get(clazz);
                if (paramBean == null) {
                    paramBean = new ParamBean();
                    paramBean.parametrized = clazz;
                }
            }
        }
        return paramBean;
    }

    private static List<Field> getFields(Class<?> clazz) {
        List<Field> fields = fieldCache.get(clazz);
        if (fields == null) {
            synchronized (fieldCache) {
                fields = fieldCache.get(clazz);
                if (fields == null) {
                    fieldCache.put(clazz, fields = collectFieldInfo(clazz));
                }
            }
        }
        return fields;
    }

    private static List<Field> collectFieldInfo(Class<?> clazz) {
        List<Field> list = new ArrayList<Field>();
        AccessibleObject[] fields = clazz.getDeclaredFields();
        for (AccessibleObject ao : fields) {
            ao.setAccessible(true);
            Field field = (Field) ao;
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            list.add(field);
        }
        return list;
    }

    /**
     * 订制由json格式装换成的map对象转换成指定的class
     * <p>
     * 1. 支持基本类型（包括对象形式）、枚举类型<br>
     * 2. 支持数组类型（组成：基本类型、枚举、对象）<br>
     * 3. 支持对象类型（form）<br>
     * 4. 支持集合List、Set（范型是基本类型、枚举、form对象）<br>
     * 5. 不支持map集合<br>
     * 
     * @param jsonObject
     * @param paramBean
     * @return Object
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getObject(Object jsonObject, ParamBean paramBean) throws Exception {
        Class<?> parametrized = paramBean.parametrized;
        Class<?>[] parameterClasses = paramBean.parameterClasses;
        if (jsonObject == null) {
            if (parametrized.isPrimitive() && parameterClasses.length == 0) {
                throw new RuntimeException(parametrized.getName() + "是基本类型，不允许赋值null");
            } else {
                return null;
            }
        }

        Object clazzObj = null;
        if (parameterClasses.length == 0) {// 非参数化类型
            if (parametrized.isPrimitive() || Boolean.class == parametrized || Byte.class == parametrized
                    || Character.class == parametrized || Short.class == parametrized || Integer.class == parametrized
                    || Long.class == parametrized || Float.class == parametrized || Double.class == parametrized
                    || String.class == parametrized) {// 基本类型
                Class<?> clazz = parametrized.isPrimitive() ? primitiveCache.get(parametrized) : parametrized;
                if (clazz != jsonObject.getClass()) {
                    if (Long.class == clazz && Integer.class == jsonObject.getClass()) {
                        clazzObj = new Long(jsonObject.toString());
                    } else {
                        throw new BizException(SystemEnum.VALIDATE);
                    }
                } else {
                    clazzObj = jsonObject;
                }
            } else {// 对象类型
                if (parametrized.isArray()) {// 数组
                    if (!(jsonObject instanceof ArrayList<?>)) {
                        throw new RuntimeException(parametrized.getName() + "是数组类型，而请求参数非数组类型");
                    }
                    Class<?> paramClazz = (Class<?>) parametrized.getComponentType();
                    ArrayList<?> tmpList = (ArrayList<?>) jsonObject;
                    Object objArr = Array.newInstance(paramClazz, tmpList.size());
                    ParamBean pb = fromClass(paramClazz);

                    for (int i = 0; i < tmpList.size(); i++) {
                        Array.set(objArr, i, getObject(tmpList.get(i), pb));
                    }
                    clazzObj = objArr;
                } else if (parametrized.isEnum()) {// 枚举
                    clazzObj = Enum.valueOf((Class<? extends Enum>) parametrized, (String) jsonObject);
                } else {// 普通对象
                    LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) jsonObject;
                    clazzObj = parametrized.newInstance();
                    List<Field> fields = getFields(parametrized);
                    for (Field field : fields) {
                        Object filedValue = map.get(field.getName());
                        ParamBean fieldParamBean = fromType(field.getGenericType());
                        Object fieldVlaue = getObject(filedValue, fieldParamBean);
                        field.set(clazzObj, fieldVlaue);
                    }
                }
            }
        } else {
            ArrayList<?> tmpList = (ArrayList<?>) jsonObject;
            Collection collection = null;
            if (parametrized == List.class || parametrized == Collection.class) {
                collection = new ArrayList(tmpList.size());
            } else if (parametrized == Set.class) {
                collection = new HashSet(tmpList.size());
            } else {
                collection = (Collection) parametrized.newInstance();
            }
            ParamBean pb = fromClass(parameterClasses[0]);
            for (Object tmp : tmpList) {
                collection.add(getObject(tmp, pb));
            }
            clazzObj = collection;
        }
        return clazzObj;
    }

}
