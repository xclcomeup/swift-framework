package com.liepin.swift.framework.mvc.dispatcher;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.liepin.swift.core.util.SpringContextUtil;

/**
 * 简单、快速的 MVC
 * <p>
 * 为Controller层提供RPC服务
 * 
 * @author yuanxl
 * 
 */
@Deprecated
@SuppressWarnings("serial")
public class ControllerDispatcher extends AbstractAdaptorDispatcherServlet {

    private static final Logger logger = Logger.getLogger(ControllerDispatcher.class);

    private final Map<String, DispatcherBean> urlPathMap = new HashMap<String, DispatcherBean>();

    private String contextConfigLocation;

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
            true);

    @Override
    protected void initDispatchBean() {
        super.initDispatchBean();
        try {
            this.contextConfigLocation = getInitParameter("contextConfigLocation");
            Set<String> packages = packages(contextConfigLocation);
            for (String _package : packages) {
                Set<BeanDefinition> beanDefinitions = provider.findCandidateComponents(convert(_package));
                for (BeanDefinition beanDefinition : beanDefinitions) {
                    Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                    annotationMapping(clazz);
                    logger.info("Dispatch Mapping class: " + beanDefinition.getBeanClassName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("ControllerFastDispatcher build controller fail", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DispatcherBean mappingRPC(String servletPath) {
        return urlPathMap.get(servletPath);
    }

    @Override
    protected void serviceApi(DispatcherBean bean, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        bean.method.invoke(bean.target, req, resp);
    }

    private Set<String> packages(String contextConfigLocation) throws Exception {
        Set<String> packages = new HashSet<String>();
        Resource[] resources = resolver.getResources(contextConfigLocation);
        for (Resource resource : resources) {
            parse(resource.getInputStream(), packages);
        }
        return packages;
    }

    private void parse(InputStream is, Set<String> packages) throws Exception {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(is);
        Element rootElement = doc.getRootElement();
        List<?> elements = rootElement.elements("component-scan");
        for (Iterator<?> it = elements.iterator(); it.hasNext();) {
            Element element = (Element) it.next();
            String packageStr = element.attributeValue("base-package");
            String[] basePackages = StringUtils.tokenizeToStringArray(packageStr,
                    ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
            for (String str : basePackages) {
                packages.add(str);
            }
        }
    }

    private String convert(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    private String firstLowercase(String name) {
        StringBuilder builder = new StringBuilder();
        char[] array = name.toCharArray();
        array[0] = Character.toLowerCase(array[0]);
        builder.append(array);
        return builder.toString();
    }

    private void annotationMapping(Class<?> clazz) {
        // 读取class注解
        RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            checkSupport(requestMapping);
        }
        String[] classMappingNames = (requestMapping != null) ? requestMapping.value() : new String[] { "" };

        Object targetObject = SpringContextUtil.getBean(firstLowercase(clazz.getSimpleName()), clazz);
        if (targetObject == null) {
            throw new RuntimeException(clazz.getName() + " no found instance in spring context");
        }

        // 读取method注解
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            RequestMapping annotation = method.getAnnotation(RequestMapping.class);
            if (annotation == null) {
                continue;
            }

            checkSupport(annotation);

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 2) {
                throw new RuntimeException(clazz.getName() + "@" + method.getName() + " params args is not "
                        + "(HttpServletRequest request,HttpServletResponse response)");
            }

            String[] methodMappingNames = annotation.value();
            if (methodMappingNames.length == 0) {
                throw new RuntimeException(clazz.getName() + "@" + method.getName() + " value uri is null");
            }

            for (String methodPath : methodMappingNames) {
                for (String clazzPath : classMappingNames) {
                    DispatcherBean bean = new DispatcherBean();
                    bean.target = targetObject;
                    bean.method = method;

                    urlPathMap.put(clazzPath + methodPath, bean);
                    logger.info("Mapped URL path [" + clazzPath + methodPath + "] to " + targetObject + "@"
                            + method.getName());
                }
            }
        }
    }

    private void checkSupport(final RequestMapping rm) {
        RequestMethod[] method = rm.method();
        if (method.length > 0) {
            throw new UnsupportedOperationException("Does not support @RequestMapping 'method' value temporarily");
        }
        String[] params = rm.params();
        if (params.length > 0) {
            throw new UnsupportedOperationException("Does not support @RequestMapping 'params' value forever");
        }
        String[] headers = rm.headers();
        if (headers.length > 0) {
            throw new UnsupportedOperationException("Does not support @RequestMapping 'headers' value forever");
        }
    }

    @Override
    protected void gwApi(DispatcherBean bean, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    protected <T extends DispatcherBean> T mappingGW(String servletPath) {
        // TODO Auto-generated method stub
        return null;
    }

}
