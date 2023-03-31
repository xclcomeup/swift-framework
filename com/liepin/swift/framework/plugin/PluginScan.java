package com.liepin.swift.framework.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import com.liepin.swift.core.log.MonitorLogger;

public class PluginScan<O> {

    private static final Logger logger = Logger.getLogger(PluginScan.class);

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();

    private static final String JAR_FILE_CLASS = "!/BOOT-INF/classes!/";
    private static final String WAR_FILE_CLASS = "!/WEB-INF/classes!/";
    // private static final String JAR_FILE_LIB = "!/BOOT-INF/lib/";
    // private static final String FILE_CLASS = ".class";

    private ApplicationContext applicationContext;

    public PluginScan(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public PluginScan() {

    }

    protected boolean isClassURL(URL url) {
        return url.getPath().contains(JAR_FILE_CLASS) || url.getPath().contains(WAR_FILE_CLASS);
    }

    private Object getBean(Class<?> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 扫描插件实现对象
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<O> scanObjects(IObjectFilter objectFilter) {
        List<O> list = new ArrayList<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object object = null;
            try {
                object = applicationContext.getBean(beanName);
            } catch (Throwable e) {
                MonitorLogger.getInstance().log(getClass().getSimpleName() + " scan applicationContext beanName="
                        + beanName + " fail, caused by:\"" + e.getMessage() + "\", ignore", e);
            }
            // 暂不需要考虑获取cglib的真实class
            if (Objects.nonNull(object) && objectFilter.test(object) && objectFilter.isOnlyComPinPackage(object)
                    && objectFilter.isContainJar(object)) {
                list.add((O) object);
            }
        }
        return list;
    }

    /**
     * 扫描插件实现的切入点方法
     * 
     * @return
     */
    public List<PluginCutPoing> scanMethods(IMethodFilter methodFilter) {
        List<PluginCutPoing> list = new ArrayList<PluginCutPoing>();
        String[] basePackages = StringUtils.tokenizeToStringArray(methodFilter.path(),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Set<String> excludeResourceURLs = getExcludeResourceURLs(methodFilter);
        for (String basePackage : basePackages) {
            String resourcePath = ClassUtils
                    .convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage)) + ".class";
            try {
                Resource[] resources = resolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        if (!methodFilter.isContainJar() && ResourceUtils.isJarURL(resource.getURL())
                                && !isClassURL(resource.getURL())) {
                            continue;
                        }
                        if (excludeResourceURLs.contains(resource.getURL().toString())) {
                            continue;
                        }
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        try {
                            Class<?> clazz = Class.forName(className);
                            List<PluginCutPoing> describes = conventional(clazz, methodFilter);
                            if (describes != null && describes.size() > 0) {
                                list.addAll(describes);
                            }
                        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                            logger.error(
                                    getClass() + " 扫描插件加载类=" + className + "失败：" + e.getCause() + ", 请及时处理，避免运行时异常!",
                                    e);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("PluginScan scan fail", e);
            }
        }
        return list;
    }

    private List<PluginCutPoing> conventional(Class<?> clazz, IMethodFilter methodFilter) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        Object instance = null;
        List<PluginCutPoing> list = null;
        for (Method method : declaredMethods) {
            if (!methodFilter.test(method)) {
                continue;
            }

            if (instance == null) {
                instance = getInstance(clazz);
                list = new ArrayList<>();
            }

            method.setAccessible(true);

            PluginCutPoing describe = new PluginCutPoing();
            describe.setClazz(clazz);
            describe.setInstance(instance);
            describe.setMethod(method);

            list.add(describe);
        }
        return list;
    }

    private Object getInstance(Class<?> clazz) {
        Object instance = null;
        try {
            instance = getBean(clazz);
        } catch (Exception e) {
            logger.warn("插件自动扫描：没有spring管理，降级自动创建对象，class=" + clazz + ", " + e.getMessage());
            // 没有@Service管理的，尝试自动创建
            try {
                instance = clazz.newInstance();
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    int mod = field.getModifiers();
                    if (Modifier.isFinal(mod) || Modifier.isNative(mod) || Modifier.isStatic(mod)
                            || Modifier.isStrict(mod) || Modifier.isTransient(mod)) {
                        continue;
                    }
                    if (!field.getType().isInterface()) {
                        continue;
                    }
                    if (Modifier.isPrivate(mod)) {
                        field.setAccessible(true);
                    }
                    Object fieldObj = field.get(instance);
                    if (fieldObj == null) {
                        fieldObj = getBean(field.getType());
                        field.set(instance, fieldObj);
                    }
                }
            } catch (Throwable e1) {
                throw new RuntimeException("插件自动扫描：class=" + clazz + ", 没有spring管理，降级自动创建对象失败", e1);
            }
            logger.warn("插件自动扫描：没有spring管理，降级自动创建对象，class=" + clazz + ", 成功并加载");
        }
        return instance;
    }

    /**
     * 扫描插件实现对象class
     * 
     * @return
     */
    public List<Class<?>> scanClazzes(IClassFilter classFilter) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        String[] basePackages = StringUtils.tokenizeToStringArray(classFilter.path(),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Set<String> excludeResourceURLs = getExcludeResourceURLs(classFilter);
        for (String basePackage : basePackages) {
            String resourcePath = ClassUtils
                    .convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage)) + ".class";
            try {
                Resource[] resources = resolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        if (!classFilter.isContainJar() && ResourceUtils.isJarURL(resource.getURL())
                                && !isClassURL(resource.getURL())) {
                            continue;
                        }
                        if (excludeResourceURLs.contains(resource.getURL().toString())) {
                            continue;
                        }
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (classFilter.test(clazz)) {
                                list.add(clazz);
                            }
                        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                            logger.error(
                                    getClass() + " 扫描插件加载类=" + className + "失败：" + e.getCause() + ", 请及时处理，避免运行时异常!",
                                    e);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("PluginScan scan fail", e);
            }
        }
        return list;
    }

    private Set<String> getExcludeResourceURLs(IScanFilter scanFilter) {
        Set<String> set = new HashSet<>();
        String path = scanFilter.excludePath();
        if (path == null || path.length() == 0) {
            return set;
        }
        String[] basePackages = StringUtils.tokenizeToStringArray(path,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        for (String basePackage : basePackages) {
            String resourcePath = ClassUtils
                    .convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage)) + ".class";
            try {
                Resource[] resources = resolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        if (!scanFilter.isContainJar() && ResourceUtils.isJarURL(resource.getURL())
                                && !isClassURL(resource.getURL())) {
                            continue;
                        }
                        set.add(resource.getURL().toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("PluginScan scan fail", e);
            }
        }
        return set;
    }

    /**
     * 扫描war包里的文件
     * 
     * @return
     */
    public List<URL> scanUrls(IURLFilter urlFilter) {
        List<URL> list = new ArrayList<>();
        String[] basePackages = StringUtils.tokenizeToStringArray(urlFilter.path(),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        for (String basePackage : basePackages) {
            String resourcePath = ClassUtils.convertClassNameToResourcePath(
                    SystemPropertyUtils.resolvePlaceholders(basePackage)) + urlFilter.suffix();
            try {
                Resource[] resources = resolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath);
                for (Resource resource : resources) {
                    URL url = resource.getURL();
                    if (resource.isReadable()) {
                        if (!urlFilter.isContainJar() && isInsideNestedJar(url.getPath())) {
                            continue;
                        }
                        if (urlFilter.test(url)) {
                            list.add(url);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("PluginScan scan fail", e);
            }
        }
        return list;
    }

    private boolean isInsideNestedJar(String dir) {
        return dir.indexOf(".war!") < dir.indexOf(".jar!");
    }

}
