package com.liepin.swift.framework.boot.listener.initializer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import com.liepin.dao.mongodb.initializer.SwiftMongodbInitializer;
import com.liepin.router.discovery.ServiceDiscovery;
import com.liepin.swift.core.util.SpringContextUtil;
import com.liepin.swift.framework.boot.SwiftApplicationContext;
import com.liepin.swift.framework.dao.initializer.SwiftDaoInitializer;
import com.liepin.swift.framework.describe.DescribeRegisterContext;
import com.liepin.swift.framework.log.StartLog;
import com.liepin.swift.framework.log.initializer.SwiftLogInitializer;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.idp.IdpPlugin;
import com.liepin.swift.framework.plugin.listener.AfterListenerPlugin;
import com.liepin.swift.framework.plugin.listener.ShutdownListenerPlugin;
import com.liepin.swift.framework.plugin.schedule.SchedulePlugin;

/**
 * 事件启动初始化
 * 
 * @author yuanxl
 *
 */
public class ApplicationListenerInitializer {

    private static volatile boolean initialize = true;

    public static ApplicationListener<?>[] initialize() {

        // 注入的顺序，也就是springboot触发的顺序
        return new ApplicationListener<?>[] {
                // step1 是SpringBoot启动开始的时候执行的事件
                new ApplicationListener<ApplicationStartingEvent>() {

                    @Override
                    public void onApplicationEvent(ApplicationStartingEvent event) {
                        SwiftLogInitializer.initialize();
                    }

                },
                // step2 是SpringBoot对应的Enviroment已经准备完毕时执行的事件，此时上下文 context
                // 还没有创建。在该监听中获取到
                // ConfigurableEnvironment 后可以对配置信息做操作，例如：修改默认的配置信息，增加额外的配置信息等。
                new ApplicationListener<ApplicationEnvironmentPreparedEvent>() {

                    @Override
                    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
                        // 暂时没使用场景
                    }

                },
                // step3 是当SpringApplication启动并且准备好 ApplicationContext ，并且在加载任何
                // bean 定义之前调用了
                // ApplicationContextInitializers 时发布的事件
                new ApplicationListener<ApplicationContextInitializedEvent>() {

                    @Override
                    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
                        // 暂时没使用场景
                    }

                },
                // step4 是SpringBoot的上下文 context 创建完成是发布的事件；但此时 spring 中的 bean
                // 还没有完全加载完成。这里可以将上下文传递出去做一些额外的操作。但是在该监听器中是无法获取自定义 bean 并进行操作的。
                new ApplicationListener<ApplicationPreparedEvent>() {

                    @Override
                    public void onApplicationEvent(ApplicationPreparedEvent event) {
                        // 注入ApplicationContext
                        SpringContextUtil.setApplicationContext4Internal(event.getApplicationContext());

                        // 加载dao
                        SwiftDaoInitializer.initialize();
                        
                        // 加载mongodb
                        SwiftMongodbInitializer.initialize();
                    }

                },
                // step5
                // WebxmlListener
                // step6 初始化或刷新完成后触发的事件
                new ApplicationListener<ContextRefreshedEvent>() {

                    @Override
                    public void onApplicationEvent(ContextRefreshedEvent event) {
                        if (event.getApplicationContext().getParent() != null) {
                            // 防止重复执行
                            return;
                        }
                        // 雨燕框架上下文启动
                        SwiftApplicationContext.initialize();
                    }

                },
                // step7
                // ApplicationListener<WebServerInitializedEvent>
                // step8 具体发布是在应用程序上下文刷新之后，调用任何 ApplicationRunner 和
                // CommandLineRunner 运行程序之前。
                new ApplicationListener<ApplicationStartedEvent>() {

                    @Override
                    public void onApplicationEvent(ApplicationStartedEvent event) {
                        PluginContext pluginContext = PluginContext.get();

                        // 启动成功后业务注册事件回调
                        pluginContext.getPlugin(AfterListenerPlugin.class).start();

                        // 启动成功后加载IDP
                        pluginContext.getPlugin(IdpPlugin.class).start();

                        // 启动成功后启动调度
                        pluginContext.getPlugin(SchedulePlugin.class).start();
                    }

                },
                // step9 这个和ApplicationStartedEvent
                // 很类似，也是在应用程序上下文刷新之后之后调用，区别在于此时 ApplicationRunner 和
                // CommandLineRunner 已经完成调用了，也意味着 SpringBoot 加载已经完成。
                new ApplicationListener<ApplicationReadyEvent>() {

                    private final Logger logger = Logger.getLogger(getClass());

                    @Override
                    public void onApplicationEvent(ApplicationReadyEvent event) {
                        // 功能信息注册
                        DescribeRegisterContext.initialize();
                        // 注册服务
                        ServiceDiscovery.getInstance().register();
                        // 给青龙系统提示启动成功
                        StartLog.out(logger, SwiftLogInitializer.printSuccessMessage());
                    }

                },
                // step10 springboot启动失败时触发的事件
                new ApplicationListener<ApplicationFailedEvent>() {

                    private final Logger logger = Logger.getLogger(getClass());

                    @Override
                    public void onApplicationEvent(ApplicationFailedEvent event) {
                        initialize = false;
                        // 给青龙系统提示启动失败
                        StartLog.err(logger, SwiftLogInitializer.printFailMessage() + "\n", event.getException());
                    }

                },
                // step11 springboot关闭后触发的事件
                new ApplicationListener<ContextClosedEvent>() {

                    private final AtomicBoolean active = new AtomicBoolean(false);

                    @Override
                    public void onApplicationEvent(ContextClosedEvent event) {
                        if (active.compareAndSet(false, true)) {
                            // 停止第一阶段
                            step1();
                            if (!initialize) {
                                step2();
                            }
                        } else {
                            // 停止第二阶段
                            step2();
                        }
                    }

                    private void step1() {
                        // 注销服务
                        ServiceDiscovery.getInstance().unregister();

                        // 停止前业务注册事件回调
                        Optional.ofNullable(PluginContext.get().getPlugin(ShutdownListenerPlugin.class))
                                .ifPresent(ShutdownListenerPlugin::destroy);
                    }

                    private void step2() {
                        // 框架上下文停止
                        SwiftApplicationContext.stop();
                    }

                }
                // ApplicationContext启动后触发的事件
                // ,new ApplicationListener<ContextStartedEvent>() {
                //
                // @Override
                // public void onApplicationEvent(ContextStartedEvent event) {
                // ContextLog.log("ContextStartedEvent");
                // // 暂时没使用场景
                // }
                // },
                // ApplicationContext停止后触发的事件
                // new ApplicationListener<ContextStoppedEvent>() {
                //
                // @Override
                // public void onApplicationEvent(ContextStoppedEvent event) {
                // ContextLog.log("ContextStoppedEvent");
                // // 暂时没使用场景
                // }
                //
                // }
        };
    }

}
