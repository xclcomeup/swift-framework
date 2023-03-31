package com.liepin.swift.framework.boot.tomcat.initializer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Optional;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.webresources.ExtractingRoot;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import com.liepin.common.conf.SystemUtil;
import com.liepin.common.conf.SystemUtil.DeployType;
import com.liepin.router.discovery.ServicePortUtil;
import com.liepin.swift.framework.plugin.jsp.JspPlugin;

@Component
public class SwiftTomcatWebServerInitializer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        // 注意此处必须设置，否则 System.getProperty(Globals.CATALINA_BASE_PROP)
        // 的值就被修改为临时目录、
        String basedir = System.getProperty(Globals.CATALINA_BASE_PROP);
        if (Objects.isNull(basedir)) {
            basedir = System.getProperty("user.dir");
            System.setProperty(Globals.CATALINA_BASE_PROP, basedir);
        }
        factory.setBaseDirectory(new File(basedir));

        factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

            @Override
            public void customize(Connector connector) {
                // init Executor
                ExecutorConfig executorConfig = new ExecutorConfig();
                StandardThreadExecutor executor = new StandardThreadExecutor();
                executor.setName(executorConfig.getName());
                executor.setNamePrefix(executorConfig.getNamePrefix());
                executor.setMaxThreads(executorConfig.getMaxThreads());
                executor.setMinSpareThreads(executorConfig.getMinSpareThreads());
                executor.setMaxIdleTime(executorConfig.getMaxIdleTime());
                connector.getService().addExecutor(executor);
                Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
                protocolHandler.setExecutor(executor);

                // init Connector
                ConnectorConfig connectorConfig = new ConnectorConfig();
                connector.setURIEncoding(connectorConfig.getURIEncoding());
                connector.setEnableLookups(connectorConfig.isEnableLookups());
                connector.setRedirectPort(connectorConfig.getRedirectPort());
                connector.setMaxPostSize(connectorConfig.getMaxPostSize());
                protocolHandler.setAcceptCount(connectorConfig.getAcceptCount());
                protocolHandler.setCompression(connectorConfig.getCompression());
                protocolHandler.setMaxHttpHeaderSize(connectorConfig.getMaxHttpHeaderSize());
                protocolHandler.setMaxHeaderCount(connectorConfig.getMaxHeaderCount());
                try {
                    protocolHandler.setAddress(InetAddress.getByName(connectorConfig.getAddress()));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(connectorConfig.addressErrorPrompt());
                }
                protocolHandler.setConnectionTimeout(connectorConfig.getConnectionTimeout());
                protocolHandler.setMaxKeepAliveRequests(connectorConfig.getMaxKeepAliveRequests());
                protocolHandler.setKeepAliveTimeout(connectorConfig.getKeepAliveTimeout());

                // init port
                ServicePortUtil.setServerPort(connector.getPort());
            }

        });

        factory.addContextCustomizers(new TomcatContextCustomizer() {

            @Override
            public void customize(Context context) {
                Container container = context.findChild("jsp");
                Optional.ofNullable(container).ifPresent(e -> {
                    JspPlugin.setStandardWrapper((StandardWrapper) e);
                });
            }

        }, new TomcatContextCustomizer() {

            @Override
            public void customize(Context context) {
                // 在启动类中配置不扫描manifest文件
                ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
            }

        }, new TomcatContextCustomizer() {

            @Override
            public void customize(Context context) {
                // 配置resource
                WebResourceRoot resources = context.getResources();
                if (Objects.nonNull(resources)) {// 兼容jar
                    ResourceConfig resourceConfig = new ResourceConfig();
                    resources.setCachingAllowed(resourceConfig.isCachingAllowed());
                    resources.setCacheMaxSize(resourceConfig.getCacheMaxSize());
                }
            }

        }, new TomcatContextCustomizer() {

            @Override
            public void customize(Context context) {
                if (DeployType.WAR == SystemUtil.getDeployType()) {
                    // war包里的jar提取出来，提升类加载速度
                    context.setResources(new ExtractingRoot());
                }
            }

        });

    }

}
