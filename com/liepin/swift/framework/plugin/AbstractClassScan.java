package com.liepin.swift.framework.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
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
import com.liepin.swift.core.util.SpringContextUtil;

@Deprecated
public abstract class AbstractClassScan {

    private static final Logger logger = Logger.getLogger(AbstractClassScan.class);

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();

    private static final String JAR_FILE_CLASS = "!/BOOT-INF/classes!/";
    // private static final String JAR_FILE_LIB = "!/BOOT-INF/lib/";
    // private static final String FILE_CLASS = ".class";


    /**
     * 扫描路径
     * 
     * @return
     */
    public abstract String path();

    /**
     * 排除扫描路径
     * 
     * @return
     */
    public String excludePath() {
        return null;
    }

    /**
     * 实现约束接口
     * 
     * @param clazz
     * @return
     */
    public abstract boolean conventional(Class<?> clazz);

    /**
     * 扫描包是否包含jar包扩展的
     * <p>
     * 默认包括jar包<br>
     * 
     * @return
     */
    protected boolean isContainJar() {
        return true;
    }

    /**
     * 扫描满足条件的class
     * 
     * @return
     */
    public List<Class<?>> scan() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        String[] basePackages = StringUtils.tokenizeToStringArray(path(),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Set<String> excludeResourceURLs = getExcludeResourceURLs(excludePath());
        for (String basePackage : basePackages) {
            String resourcePath = ClassUtils
                    .convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage)) + ".class";
            try {
                Resource[] resources = resolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        if (!isContainJar() && ResourceUtils.isJarURL(resource.getURL())
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
                            if (conventional(clazz)) {
                                list.add(clazz);
                            }
                        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                            MonitorLogger.getInstance().log(getClass() + " plguin load class=" + className + " fail=" + e.getCause()
                                    + ", please be careful，if not important, please ignore!");
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("AbstractClassScan scan fail", e);
            }
        }
        return list;
    }

    protected boolean isClassURL(URL url) {
        return url.getPath().contains(JAR_FILE_CLASS);
        // || (!url.getPath().contains(JAR_FILE_LIB) &&
        // url.getPath().endsWith(FILE_CLASS));
    }

    protected void checkBean(Class<?> clazz) {
        // 校验clazz是否使用注解创建单例
        SpringContextUtil.getBean(clazz);
    }

    protected void checkBean(String beanName) {
        SpringContextUtil.getBean(beanName);
    }

    private Set<String> getExcludeResourceURLs(String path) {
        Set<String> set = new HashSet<>();
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
                        if (!isContainJar() && ResourceUtils.isJarURL(resource.getURL())
                                && !isClassURL(resource.getURL())) {
                            continue;
                        }
                        set.add(resource.getURL().toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("AbstractClassScan scan fail", e);
            }
        }
        return set;
    }

}
