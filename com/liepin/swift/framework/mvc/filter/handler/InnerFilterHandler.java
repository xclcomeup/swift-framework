package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.common.json.JsonUtil;
import com.liepin.router.discovery.ServiceDiscovery;
import com.liepin.router.discovery.ServiceRecorder;
import com.liepin.router.util.RouterConfigCurator;
import com.liepin.swift.framework.monitor.HttpMonitorService;
import com.liepin.swift.framework.monitor.IHttpMonitor;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.eventInfo.NullEvent;
import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.impl.app.AppSafeHelper;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.Poller;

/**
 * 内部接口处理器，承载/monitor请求
 * 
 * @author yuanxl
 *
 */
public class InnerFilterHandler extends AbstractFilterHandler implements FilterHandler {

    private static final Logger logger = Logger.getLogger(InnerFilterHandler.class);

    private static final String FLAG = "OK";

    private static IHttpMonitor httpMonitor = new HttpMonitorService();

    private final Map<String, InnerHandler> innerHandlers = new HashMap<>();

    public InnerFilterHandler(List<GenericFilter> externalFilterChains) {
        setExternalFilterChains(externalFilterChains);
        init();
    }

    private void init() {
        for (InnerHandler innerHandler : InnerHandler.values()) {
            innerHandlers.put(innerHandler.path(), innerHandler);
        }
    }

    public static enum InnerHandler implements Handler {

        JVM_DO() {

            @Override
            public String path() {
                return "/monitor/jvm.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                write(response, httpMonitor.jvm());
            }

        },
        HTTP() {

            @Override
            public String path() {
                return "/monitor/http";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if (httpMonitor.http()) {
                    write(response, FLAG);// 显示作用，判断靠http code
                } else {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            }

        },
        ROUTING_TABLE_REFRESH_DO() {

            @Override
            public String path() {
                return "/monitor/routingTableRefresh.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                ServiceDiscovery.getInstance().maybeRefreshApplication();
            }

        },
        EVENTINFO_MANAGE_DO() {

            @Override
            public String path() {
                return "/monitor/em.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                httpMonitor.eventinfoManage(request.getParameter("actionPath"), request.getParameter("fullPrint"));
            }

        },
        CHECK_FLOW_DO() {

            @Override
            public String path() {
                return "/monitor/isFlow.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                write(response, httpMonitor.isFlow());
            }

        },
        TOP_THREAD_DO() {

            @Override
            public String path() {
                return "/monitor/topThread.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                String thresholdStr = request.getParameter("threshold");
                String countStr = request.getParameter("count");
                float threshold = 99.0f;
                if (thresholdStr != null) {
                    try {
                        threshold = Float.parseFloat(thresholdStr);
                    } catch (Exception e) {
                    }
                }
                int count = 10;
                if (countStr != null) {
                    try {
                        count = Integer.parseInt(countStr);
                    } catch (Exception e) {
                    }
                }
                write(response, httpMonitor.topThread(threshold, count));
            }

        },
        THREAD_DO() {

            @Override
            public String path() {
                return "/monitor/thread.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                String lineBreak = request.getParameter("lineBreak");
                if (lineBreak == null) {
                    lineBreak = "\n";
                }
                write(response, httpMonitor.thread(lineBreak));
            }

        },
        ENV_DO() {

            @Override
            public String path() {
                return "/monitor/env.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                write(response, httpMonitor.env());
            }

        },
        DEPENDENCY_DO() {

            @Override
            public String path() {
                return "/monitor/dependency.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                write(response, httpMonitor.dependency());
            }

        },
        SEARCH_DO() {

            @Override
            public String path() {
                return "/monitor/search.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                String containJarStr = request.getParameter("containJar");
                boolean containJar = false;
                if (containJarStr != null) {
                    try {
                        containJar = Boolean.parseBoolean(containJarStr);
                    } catch (Exception e) {
                    }
                }
                String jarName = request.getParameter("jarName");
                String texts = request.getParameter("texts");
                if (texts == null || texts.trim().length() == 0) {
                    return;
                }
                String[] textArray = texts.trim().split(",");
                write(response, httpMonitor.search(jarName, containJar, textArray));
            }

        },
        @Deprecated
        APP_DO() {

            @Override
            public String path() {
                return "/monitor/app.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                String schema = request.getParameter("schema");
                if (schema == null || "".equals(schema.trim())) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                try {
                    Map<String, Object> export = AppSafeHelper.getInstance().export(schema);
                    String json = JsonUtil.toJson(export);
                    write(response, json);
                } catch (Exception e) {
                    logger.error("导出app私钥失败", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        },
        @Deprecated
        APP__DO() {

            @Override
            public String path() {
                return "/monitor/app_.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                String schema = request.getParameter("schema");
                String token = request.getParameter("_time_");
                if (schema == null || "".equals(schema.trim()) || token == null || "".equals(token.trim())) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                try {
                    Map<String, Object> export = AppSafeHelper.getInstance().export(schema, token);
                    String json = JsonUtil.toJson(export);
                    write(response, json);
                } catch (Exception e) {
                    logger.error("导出app私钥失败", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        },
        HEALTHSTATUS_DDO() {

            private final AtomicInteger concurrentConnections = new AtomicInteger(0);
            private final int maxConcurrentConnections = 5;

            @Override
            public String path() {
                return "/service/healthstatus.do";
            }

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                if (!RequestUtil.checkInnerRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                int numberConnections = concurrentConnections.incrementAndGet();
                Poller poller = null;
                try {
                    if (numberConnections > maxConcurrentConnections) {
                        response.sendError(503,
                                "health status MaxConcurrentConnections reached: " + maxConcurrentConnections);
                    } else {
                        response.setHeader("Content-Type", "text/event-stream;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                        response.setHeader("Pragma", "no-cache");

                        poller = new Poller();
                        poller.start();

                        // 检查临时节点，如果没有重新创建
                        ServiceDiscovery.getInstance().checkServiceRegistrationStatus(true);

                        // monitorLog.log("Starting one poller for health status
                        // to the servicecenter");
                        ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatRegister,
                                "Starting one poller for health status to the servicecenter");
                        PrintWriter writer = response.getWriter();
                        try {
                            while (poller.isRunning() && ServiceDiscovery.getInstance().inService()) {
                                writer.write(poller.message());
                                writer.flush();
                                if (!ServiceDiscovery.getInstance().inService()) {
                                    break;
                                }

                                if (response.getWriter().checkError()) {
                                    throw new IOException("io error");
                                }

                                TimeUnit.MILLISECONDS.sleep(RouterConfigCurator.get().getActiveHeartbeatDelay());
                            }
                        } catch (InterruptedException e) {
                            poller.shutdown();
                            // monitorLog.log("Happen InterruptedException, will
                            // stop polling health status.");
                            ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatBreak,
                                    "Happen InterruptedException, will stop polling health status.");
                            Thread.currentThread().interrupt();
                        } catch (IOException e) {
                            poller.shutdown();
                            // monitorLog.log(
                            // "Happen IOException while trying to write
                            // (generally caused by client disconnecting), will
                            // stop polling health status.");
                            ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatBreak,
                                    "Happen IOException while trying to write (generally caused by client disconnecting), will stop polling health status.");
                        } catch (Exception e) {
                            poller.shutdown();
                            // monitorLog
                            // .log("Happen Exception Failed to send heartbeat,
                            // will stop polling health status.");
                            ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatBreak,
                                    "Happen Exception Failed to send heartbeat, will stop polling health status.");
                        }
                        // monitorLog.log("Stopping one poller for health status
                        // to the service center");
                        ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatBreak,
                                "Stopping one poller for health status to the service center");
                    }
                } catch (Exception e) {
                    // monitorLog.log("Error initializing controller for health
                    // status event stream.");
                    ServiceRecorder.record(null, null, ServiceRecorder.EventType.ServiceHeartbeatBreak,
                            "Error initializing controller for health status event stream.");
                } finally {
                    concurrentConnections.decrementAndGet();
                    if (poller != null) {
                        poller.shutdown();
                    }
                }
            }

        };

        private static void write(final HttpServletResponse response, Object context)
                throws ServletException, IOException {
            if (context != null) {
                response.setContentType("UTF-8");
                PrintWriter writer = response.getWriter();
                writer.print(context);
                writer.flush();
            }
        }

    }

    protected static interface Handler {

        public String path();

        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException;

    }

    @Override
    public boolean supports(HttpServletRequest request) {
        return innerHandlers.containsKey(request.getServletPath());
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        // 不输出日志
        NullEvent event = new NullEvent();
        event.begin();
        event.setType("inner");
        event.setName(request.getServletPath());
        return event;
    }

    @Override
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        doFilterProxy(request, response, getFilters(servletPath), null);
        InnerHandler innerHandler = innerHandlers.get(servletPath);
        if (Objects.nonNull(innerHandler)) {
            innerHandler.handle(request, response);
        }
        return ResultStatus.ok();
    }

    @Override
    public boolean context() {
        return false;
    }

}
