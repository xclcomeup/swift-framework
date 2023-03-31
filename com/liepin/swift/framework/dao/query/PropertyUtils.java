package com.liepin.swift.framework.dao.query;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class PropertyUtils {

    public static class PropertyBean {
        Field field;
        Class<?> clazz;

        public PropertyBean() {
        }

        public PropertyBean(Field field, Class<?> clazz) {
            this.field = field;
            this.clazz = clazz;
        }

        public Field getField() {
            return field;
        }

        public void setField(Field field) {
            this.field = field;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

    }

    public static PropertyBean[] getUnConstDeclaredFields(Object o) {
        List<PropertyBean> list = new ArrayList<PropertyBean>();
        for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field[] fields = clazz.getDeclaredFields();
                if (fields != null) {
                    for (Field field : fields) {

                        int modifiers = field.getModifiers();
                        if ((modifiers & Modifier.STATIC) != 0 || (modifiers & Modifier.FINAL) != 0) // 排除用
                                                                                                     // static
                                                                                                     // 或
                                                                                                     // final
                                                                                                     // 修饰的属性
                        {
                            continue;
                        }
                        // else if(((modifiers & Modifier.STATIC) != 0 &&
                        // (modifiers & Modifier.FINAL) != 0)) // 常量
                        // {
                        // continue;
                        // }
                        else {

                            list.add(new PropertyBean(field, clazz));
                        }
                    }
                }
            } catch (Exception e) {
                clazz = clazz.getSuperclass();
            }
        }
        return (PropertyBean[]) list.toArray(new PropertyBean[list.size()]);
    }

    public static Field getField(Object obj, String name) throws SecurityException, NoSuchFieldException {
        return obj.getClass().getField(name);
    }

    public static Object getProperty(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException {

        f.setAccessible(true);
        return f.get(obj);
    }

    public static void setFieldValue(Object obj, Field f, Object value) throws IllegalArgumentException,
            IllegalAccessException {
        f.setAccessible(true);
        f.set(obj, value);
    }

    public static Object getPropertyByMethod(Object obj, Field field, Class<?> clazz) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, IntrospectionException {
        PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz);
        Method getMethod = pd.getReadMethod();
        getMethod.setAccessible(true);
        return getMethod.invoke(obj);
    }

    public static Object getPropertyByMethod(Object obj, String fieldName, Class<?> clazz)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IntrospectionException {
        PropertyDescriptor pd = new PropertyDescriptor(fieldName, clazz);
        Method getMethod = pd.getReadMethod();
        getMethod.setAccessible(true);
        return getMethod.invoke(obj);
    }
}
