package com.liepin.swift.framework.plugin;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;
import com.liepin.swift.framework.plugin.dao.DaoEntityPlugin;
import com.liepin.swift.framework.plugin.dao.jpa.JpaRepositoryPlugin;
import com.liepin.swift.framework.plugin.fallback.FallbackPlugin;
import com.liepin.swift.framework.plugin.filter.ExternalFilterPlugin;
import com.liepin.swift.framework.plugin.gateway.GatewayPlugin;
import com.liepin.swift.framework.plugin.gateway.interceptor.GwApiInterceptorPlugin;
import com.liepin.swift.framework.plugin.idp.IdpPlugin;
import com.liepin.swift.framework.plugin.jsp.JspPlugin;
import com.liepin.swift.framework.plugin.listener.AfterListenerPlugin;
import com.liepin.swift.framework.plugin.listener.ServiceListenerPlugin;
import com.liepin.swift.framework.plugin.listener.ShutdownListenerPlugin;
import com.liepin.swift.framework.plugin.resolver.ExceptionResolverPlugin;
import com.liepin.swift.framework.plugin.resolver.ajax.ExceptionInterceptorPlugin;
import com.liepin.swift.framework.plugin.resource.ResourcePlugin;
import com.liepin.swift.framework.plugin.schedule.SchedulePlugin;
import com.liepin.swift.framework.plugin.service.ServicePlugin;
import com.liepin.swift.framework.util.AsynProcess;
import com.liepin.swift.framework.util.AsynProcess.AsynHandle;

public class PluginContext {

    private static final Logger logger = Logger.getLogger(PluginContext.class);

    private final Map<Class<? extends IPlugin<?>>, IPlugin<?>> plugins = new LinkedHashMap<Class<? extends IPlugin<?>>, IPlugin<?>>();
    /**
     * 注意顺序
     */
    private final Class<?>[] pluginClassRegisters = {
            /** JSP层 */
            JspPlugin.class,
            /** 静态资源层 */
            ResourcePlugin.class,
            /** Dao层 */
            DaoEntityPlugin.class,
            /** JPA */
            JpaRepositoryPlugin.class,
            /** 过滤器 */
            ExternalFilterPlugin.class,
            /** mvc异常处理器 */
            ExceptionResolverPlugin.class,
            /** ajax异常处理器 */
            ExceptionInterceptorPlugin.class,
            /** Controller层 */
            ControllerPlugin.class,
            /** 服务层 */
            ServicePlugin.class,
            /** Gateway层 */
            GatewayPlugin.class,
            /** Gateway api interceptor层 */
            GwApiInterceptorPlugin.class,
            /** 降级层 */
            FallbackPlugin.class,
            /** 调度系统 */
            SchedulePlugin.class,
            /** IDP系统 */
            IdpPlugin.class,
            /** 启动完成监听 */
            AfterListenerPlugin.class,
            /** 停止前监听 */
            ShutdownListenerPlugin.class,
            /** 服务状态监听 */
            ServiceListenerPlugin.class };

    private ApplicationContext applicationContext;

    private static PluginContext instance = new PluginContext();

    private PluginContext() {
    }

    public static PluginContext get() {
        return instance;
    }

    /**
     * 组件初始化
     * 
     */
    public synchronized void initialize() {
        Arrays.asList(pluginClassRegisters).forEach(clazz -> loadPlugin(clazz));
    }

    /**
     * 组件销毁
     */
    public synchronized void cancel() {
        AsynProcess asynProcess = new AsynProcess(plugins.size());
        for (Map.Entry<Class<? extends IPlugin<?>>, IPlugin<?>> entry : plugins.entrySet()) {
            asynProcess.execute("释放" + entry.getKey().getSimpleName() + "资源", new AsynHandle() {

                @Override
                public void process() {
                    entry.getValue().destroy();
                }

            });
        }
        asynProcess.awaitAndfinish(30);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPlugin(Class<T> clazz) {
        return (T) plugins.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadPlugin(Class<T> clazz) {
        return (T) initPlugin(clazz);
    }

    @SuppressWarnings("unchecked")
    private IPlugin<?> initPlugin(Class<?> clazz) {
        applicationContext = Optional.ofNullable(applicationContext)
                .orElseGet(SpringContextUtil::getApplicationContext);
        IPlugin<?> plugin = plugins.get(clazz);
        if (Objects.isNull(plugin)) {
            try {
                long s = System.currentTimeMillis();
                plugin = (IPlugin<?>) clazz.newInstance();
                // 启动组件
                plugin.init(applicationContext);
                plugins.put((Class<? extends IPlugin<?>>) clazz, plugin);
                logger.info("加载插件<" + plugin.name() + "|" + clazz + ">启动完成, 耗时: " + (System.currentTimeMillis() - s)
                        + " ms");
            } catch (Exception e) {
                throw new RuntimeException("加载插件<" + clazz + ">启动失败!", e);
            }
        }
        return plugin;
    }

}
