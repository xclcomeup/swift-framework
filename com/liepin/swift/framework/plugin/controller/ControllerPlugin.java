package com.liepin.swift.framework.plugin.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.core.annotation.SwiftNoPack;
import com.liepin.swift.core.annotation.SwiftNoPack4Input;
import com.liepin.swift.core.annotation.UnEscapeHtml;
import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean;
import com.liepin.swift.framework.mvc.dispatcher.DispatcherMethodBean.ParamBean;
import com.liepin.swift.framework.mvc.util.JsonBodyPathFinder;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.PluginScan;
import com.liepin.swift.framework.security.csrf.CsrfCover;
import com.liepin.swift.framework.util.ObjectUtil;
import com.liepin.swift.framework.util.TypeUtil;

public class ControllerPlugin implements IPlugin<Map<String, DispatcherMethodBean>> {

    private static final Logger logger = Logger.getLogger(ControllerPlugin.class);

    @Deprecated
    private final Map<String, DispatcherMethodBean> controllerMethodBeanMap = new HashMap<String, DispatcherMethodBean>();

    private final LocalVariableTableParameterNameDiscoverer classPathDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    // 加在param上的@UnEscapeHtml
    private final Map<String, Set<String>> unEscapeHtmlControllerParamMap = new HashMap<String, Set<String>>();
    // first: 完整匹配 | second:前缀匹配 FIXME：删除
    private final Pair<Set<String>, Set<String>> unEscapeHtmlControllerPair = new Pair<Set<String>, Set<String>>(
            new HashSet<>(), new HashSet<>());
    // 加在method上的@UnEscapeHtml
    private final Set<String> unEscapeHtmlSet = new HashSet<>();

    // JSON请求响应体不使用雨燕协议包装的接口列表
    private final Set<String> noPackControllerSet = new HashSet<>();
    // JSON请求请求体不使用雨燕协议包装的接口列表
    private final Set<String> noPackControllerSet4Input = new HashSet<>();
    // 请求体是json的请求
    private final List<String> jsonReqeustUrls = new ArrayList<>();
    // 返回体是json的请求
    private final List<String> jsonResponseUrls = new ArrayList<>();

    // 需要csrf拦截的请求
    private final Set<String> needCsrfUrls = new HashSet<>();

    public static List<IPluginListener> listeners = new ArrayList<>();

    public static void listen(IPluginListener listener) {
        listeners.add(listener);
    }

    @Deprecated
    public DispatcherMethodBean getControllerMethod(String methodUri) {
        DispatcherMethodBean bean = controllerMethodBeanMap.get(methodUri);
        if (bean == null) {
            logger.warn("App请求接口uri=" + methodUri + "不存在!");
        }
        return bean;
    }

    public Set<String> getUnEscapeHtmlControllerParam(String methodUri) {
        return unEscapeHtmlControllerParamMap.get(methodUri);
    }

    public Pair<Set<String>, Set<String>> getUnEscapeHtmlControllerPair() {
        return unEscapeHtmlControllerPair;
    }

    /**
     * 判断请求的响应体是否忽略猎聘协议
     * 
     * @param servletPath
     * @return
     */
    public boolean isNoPack(String servletPath) {
        // return noPackControllerSet.contains(servletPath);
        return JsonBodyPathFinder.getNopack().match(servletPath);
    }

    /**
     * 判断请求的输入体是否忽略猎聘协议
     * 
     * @param servletPath
     * @return
     */
    public boolean isNoPack4Input(String servletPath) {
        // return noPackControllerSet4Input.contains(servletPath);
        return JsonBodyPathFinder.getNopack4input().match(servletPath);
    }

    public Set<String> getNeedCsrfUrls() {
        return needCsrfUrls;
    }

    @Override
    public void init(ApplicationContext applicationContext) {
        logger.info("ControllerPlugin init.");
        StringBuilder log = new StringBuilder();
        new PluginScan<Object>(applicationContext).scanObjects(new ControllerObjectFilter()).forEach(c -> {
            Object actual = ObjectUtil.getActual(c);
            Class<? extends Object> clazz = actual.getClass();
            Map<String, DispatcherMethodBean> methodMap = annotationMapping(clazz);
            for (Map.Entry<String, DispatcherMethodBean> entry : methodMap.entrySet()) {
                String uri = entry.getKey();
                DispatcherMethodBean bean = entry.getValue();
                if (controllerMethodBeanMap.containsKey(uri)) {
                    // 验证重复的controller requestMapping定义
                    throw new RuntimeException("Controller RequestMapping URI重复定义: " + clazz.getName() + "@" + uri);
                }
                bean.target = actual;
                controllerMethodBeanMap.put(uri, bean);
                logger.info("Mapped URL path [" + uri + "] to " + clazz.getName() + "@" + bean.method.getName());
            }
            log.append("Added {" + clazz.getName()).append("} to Controller\n");
        });
        logger.info("@UnEscapeHtml Param Find: " + unEscapeHtmlControllerParamMap);
        logger.info("@UnEscapeHtml Find: " + unEscapeHtmlControllerPair);
        logger.info("@UnEscapeHtml Method Find: " + unEscapeHtmlSet);
        logger.info("@RequestBody Find: " + jsonReqeustUrls);
        logger.info("@ResponseBody Find: " + jsonResponseUrls);
        logger.info("@SwiftNoPack Find: " + noPackControllerSet);
        logger.info("@SwiftNoPack4Input Find: " + noPackControllerSet4Input);
        logger.info("@CsrfCover Find: " + needCsrfUrls);
        logger.info(log.toString());

        listeners.forEach(t -> {
            t.handle(this);
        });
    }

    private Map<String, DispatcherMethodBean> annotationMapping(Class<?> controllerClass) {
        Map<String, DispatcherMethodBean> methodMap = new HashMap<String, DispatcherMethodBean>();
        String[] classMappings = AnnotationUtil.getRequestMapping(controllerClass);

        // 读取@RestController
        RestController restController = controllerClass.getAnnotation(RestController.class);

        // 不封装协议的接口
        SwiftNoPack noPack2Class = controllerClass.getAnnotation(SwiftNoPack.class);
        SwiftNoPack4Input noPack4Input2Class = controllerClass.getAnnotation(SwiftNoPack4Input.class);

        // 读取method注解
        Method[] methods = controllerClass.getMethods();
        for (Method method : methods) {
            // 检查
            if (Objects.isNull(method.getAnnotation(RequestMapping.class))
                    && Objects.isNull(method.getAnnotation(GetMapping.class))
                    && Objects.isNull(method.getAnnotation(PostMapping.class))) {
                continue;
            }
            String[] methodMappings = AnnotationUtil.getRequestMapping(method);
            if (methodMappings.length == 0) {
                // 验证是否指定了controller的requestMapping
                throw new RuntimeException(
                        "Controller的RequestMapping没有定义: " + controllerClass.getName() + "@" + method.getName());
            }

            // 校验
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> ptClass = parameterTypes[i];
                if (ptClass == HttpServletRequest.class) {
                    // 验证方法参数里有没有HttpServletRequest
                    throw new RuntimeException("Controller的方法参数不允许出现HttpServletRequest: " + controllerClass.getName()
                            + "@" + method.getName());
                }
            }

            // 判断是否有body请求和Xss白名单参数
            boolean bodyReqeust = false;
            Set<String> unEscapeHtmlParamNames = new LinkedHashSet<String>();
            String[] parameterNames = classPathDiscoverer.getParameterNames(method);
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof RequestBody) {
                        bodyReqeust = true;
                    }
                    if (annotation instanceof UnEscapeHtml) {
                        unEscapeHtmlParamNames.add(parameterNames[i]);
                        // FIXME 后期支持对象里面的字段
                    }
                }
            }
            // 方法是否json体响应
            ResponseBody responseBody = method.getAnnotation(ResponseBody.class);

            DispatcherMethodBean methodBean = getMethodBean(method);

            // 方法级别Xss白名单
            UnEscapeHtml method2UnEscapeHtml = method.getAnnotation(UnEscapeHtml.class);

            // 不封装协议的接口
            SwiftNoPack noPack = method.getAnnotation(SwiftNoPack.class);
            if (Objects.nonNull(noPack2Class)) {
                noPack = noPack2Class;
            }
            SwiftNoPack4Input noPack4Input = method.getAnnotation(SwiftNoPack4Input.class);
            if (Objects.nonNull(noPack4Input2Class)) {
                noPack4Input = noPack4Input2Class;
            }

            CsrfCover csrfCover = method.getAnnotation(CsrfCover.class);

            for (String classMapping : classMappings) {
                for (String methodMapping : methodMappings) {
                    String servletPath = join(classMapping, methodMapping);
                    methodMap.put(servletPath, methodBean);
                    // json体请求
                    if (bodyReqeust) {
                        JsonBodyPathFinder.getRequest().append(servletPath);
                        jsonReqeustUrls.add(servletPath);
                    }
                    // json体响应
                    if (Objects.nonNull(restController) || Objects.nonNull(responseBody)) {
                        JsonBodyPathFinder.getResponse().append(servletPath);
                        jsonResponseUrls.add(servletPath);
                    }
                    // param UnEscapeHtml
                    if (unEscapeHtmlParamNames.size() > 0) {
                        unEscapeHtmlControllerParamMap.put(servletPath, unEscapeHtmlParamNames);
                    }
                    if (Objects.nonNull(method2UnEscapeHtml)) {
                        if ("/".equals(method2UnEscapeHtml.prefixServletPath())) {
                            unEscapeHtmlControllerPair.getFirst().add(servletPath);
                        } else {
                            unEscapeHtmlControllerPair.getSecond().add(method2UnEscapeHtml.prefixServletPath());
                        }
                    }
                    // method UnEscapeHtml
                    if (Objects.nonNull(method2UnEscapeHtml)) {
                        JsonBodyPathFinder.getUnEscapeHtml().append(servletPath);
                        unEscapeHtmlSet.add(servletPath);
                    }
                    if (Objects.nonNull(noPack)) {
                        noPackControllerSet.add(servletPath);
                        JsonBodyPathFinder.getNopack().append(servletPath);
                    }
                    if (Objects.nonNull(noPack4Input)) {
                        noPackControllerSet4Input.add(servletPath);
                        JsonBodyPathFinder.getNopack4input().append(servletPath);
                    }
                    if (Objects.nonNull(csrfCover)) {
                        // FIXME 暂只支持精确路径匹配
                        needCsrfUrls.add(servletPath);
                    }
                }
            }
        }
        return methodMap;
    }

    private String join(String classMapping, String methodMapping) {
        return (!classMapping.endsWith("/") && !methodMapping.startsWith("/")) ? classMapping + "/" + methodMapping
                : classMapping + methodMapping;
    }

    private DispatcherMethodBean getMethodBean(final Method method) {
        DispatcherMethodBean methodBean = new DispatcherMethodBean();
        methodBean.method = method;
        String[] parameterNames = classPathDiscoverer.getParameterNames(method);
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            ParamBean paramBean = new ParamBean();
            List<Class<?>> list = new ArrayList<Class<?>>();
            TypeUtil.recursiveParamClasses(type, list);
            paramBean.parametrized = list.get(0);
            if (list.size() > 1) {
                List<Class<?>> subList = list.subList(1, list.size());
                paramBean.parameterClasses = subList.toArray(new Class<?>[] {});
            }
            methodBean.setParamBean(parameterNames[i], paramBean);
        }
        return methodBean;
    }

    @Override
    public void destroy() {
        controllerMethodBeanMap.clear();
        logger.info("ControllerPlugin destroy.");
    }

    @Override
    public Map<String, DispatcherMethodBean> getObject() {
        return Collections.unmodifiableMap(controllerMethodBeanMap);
    }

    @Override
    public String name() {
        return "Controller类加载";
    }

}
