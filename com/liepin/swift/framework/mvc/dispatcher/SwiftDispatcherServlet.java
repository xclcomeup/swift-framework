package com.liepin.swift.framework.mvc.dispatcher;

import org.apache.log4j.Logger;

/**
 * 
 * router监听类 继承
 * {@link com.liepin.swift.framework.mvc.dispatcher.ServiceDispatcher}
 * <p>
 * 配置方法:在web.xml里
 * <p>
 * &lt;servlet-class&gt;com.liepin.swift.framework.mvc.dispatcher.
 * SwiftDispatcherServlet&lt;/servlet-class&gt;<br>
 * <p>
 * tomcat启动顺序：Listener->Filter->Servlet<br>
 * tomcat停止熟悉：Servlet->Filter->Listener<br>
 * 
 * @author yuanxl
 * 
 */
@SuppressWarnings("serial")
public class SwiftDispatcherServlet extends ServiceDispatcher {

    private static final Logger logger = Logger.getLogger(SwiftDispatcherServlet.class);

    public SwiftDispatcherServlet() {
        logger.info("SwiftDispatcherServlet create");
    }

    @Override
    protected void initDispatchBean() {
        super.initDispatchBean();
    }

}
