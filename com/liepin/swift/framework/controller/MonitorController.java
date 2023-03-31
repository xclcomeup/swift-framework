//package com.liepin.swift.framework.controller;
//
//import java.io.PrintWriter;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.log4j.Logger;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import com.liepin.router.discovery.ServiceDiscovery;
//import com.liepin.swift.core.exception.BizException;
//import com.liepin.swift.framework.monitor.HttpMonitorService;
//import com.liepin.swift.framework.monitor.IHttpMonitor;
//import com.liepin.swift.framework.mvc.filter.handler.InnerFilterHandler;
//import com.liepin.swift.framework.util.IPUtil;
//
///**
// * 已迁移到 {@link InnerFilterHandler}
// * 
// * @author yuanxl
// *
// */
//@Deprecated
////@Controller
////@RequestMapping("/monitor")
//public class MonitorController {
//
//    private static final Logger logger = Logger.getLogger(MonitorController.class);
//
//    private static final String FLAG = "OK";
//
//    private IHttpMonitor httpMonitor = new HttpMonitorService();
//
//    @RequestMapping(value = "/jvm.do")
//    public void handlerJvm(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        write(response, httpMonitor.jvm());
//    }
//
//    @RequestMapping(value = "/http.do")
//    public void handlerHttp(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        boolean flag = httpMonitor.http(request.getParameter("switch"));
//        String ret = "OK";// 显示作用，判断靠http code
//        if (!flag) {
//            ret = null;
//            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
//        }
//        write(response, ret);
//    }
//
//    @RequestMapping(value = "/http")
//    public void handlerHttp2(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        try {
//            String needRecoveryStr = request.getParameter("needRecovery");
//            boolean needRecovery = false;
//            if (needRecoveryStr != null && needRecoveryStr.length() > 0) {
//                needRecovery = Boolean.parseBoolean(needRecoveryStr);
//            }
//            String action = request.getParameter("action");
//            if (action == null || action.trim().length() == 0) {
//                action = "all";
//            }
//            boolean status = httpMonitor.http2(request.getParameter("switch"), needRecovery, action);
//            if (status) {
//                write(response, FLAG);// 显示作用，判断靠http code
//            } else {
//                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
//            }
//        } catch (BizException e) {
//            logger.error(e.getMessage() + ", sign=" + request.getParameter("switch"));
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        }
//    }
//
//    @RequestMapping(value = "/routingTableRefresh.do")
//    public void handlerRefresh(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        ServiceDiscovery.getInstance().maybeRefreshApplication();
//    }
//
//    @RequestMapping(value = "/em.do")
//    public void handlerEventinfoManage(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        httpMonitor.eventinfoManage(request.getParameter("actionPath"), request.getParameter("fullPrint"));
//    }
//
//    @RequestMapping(value = "/isFlow.do")
//    public void handlerFlux(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        write(response, httpMonitor.isFlow());
//    }
//
//    @RequestMapping(value = "/topThread.do")
//    public void handlerTopThread(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        String thresholdStr = request.getParameter("threshold");
//        String countStr = request.getParameter("count");
//        float threshold = 99.0f;
//        if (thresholdStr != null) {
//            try {
//                threshold = Float.parseFloat(thresholdStr);
//            } catch (Exception e) {
//            }
//        }
//        int count = 10;
//        if (countStr != null) {
//            try {
//                count = Integer.parseInt(countStr);
//            } catch (Exception e) {
//            }
//        }
//        write(response, httpMonitor.topThread(threshold, count));
//    }
//
//    @RequestMapping(value = "/thread.do")
//    public void handlerThread(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        String lineBreak = request.getParameter("lineBreak");
//        if (lineBreak == null) {
//            lineBreak = "\n";
//        }
//        write(response, httpMonitor.thread(lineBreak));
//    }
//
//    @RequestMapping(value = "/env.do")
//    public void handlerEnv(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        write(response, httpMonitor.env());
//    }
//
//    @RequestMapping(value = "/dependency.do")
//    public void handlerDependency(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        write(response, httpMonitor.dependency());
//    }
//
//    @RequestMapping(value = "/search.do")
//    public void handlerSearch(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (!check(request)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            return;
//        }
//        String containJarStr = request.getParameter("containJar");
//        boolean containJar = false;
//        if (containJarStr != null) {
//            try {
//                containJar = Boolean.parseBoolean(containJarStr);
//            } catch (Exception e) {
//            }
//        }
//        String jarName = request.getParameter("jarName");
//        String texts = request.getParameter("texts");
//        if (texts == null || texts.trim().length() == 0) {
//            return;
//        }
//        String[] textArray = texts.trim().split(",");
//        write(response, httpMonitor.search(jarName, containJar, textArray));
//    }
//
//    private void write(final HttpServletResponse response, Object context) throws Exception {
//        if (context != null) {
//            response.setContentType("UTF-8");
//            PrintWriter writer = response.getWriter();
//            writer.print(context);
//            writer.flush();
//        }
//    }
//
//    private boolean check(HttpServletRequest request) {
//        String ip = IPUtil.getIpAddr(request);
//        if ("127.0.0.1".equals(ip)) {
//            return true;
//        }
//        if (IPUtil.isPrivateIp(ip)) {
//            return true;
//        }
//        return false;
//    }
//
//}
