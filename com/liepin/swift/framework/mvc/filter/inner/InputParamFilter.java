//package com.liepin.swift.framework.mvc.filter.inner;
//
//import java.io.IOException;
//import java.net.URLDecoder;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Set;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.log4j.Logger;
//
//import com.liepin.common.conf.PropUtil;
//import com.liepin.common.conf.SystemUtil;
//import com.liepin.common.file.IoUtil;
//import com.liepin.common.json.JsonUtil;
//import com.liepin.common.other.GzipUtil;
//import com.liepin.swift.core.bean.FscpHeader;
//import com.liepin.swift.core.consts.Const;
//import com.liepin.swift.core.enums.SystemEnum;
//import com.liepin.swift.core.exception.BizException;
//import com.liepin.swift.core.log.MonitorLogger;
//import com.liepin.swift.core.util.ThreadLocalUtil;
//import com.liepin.swift.core.util.TraceIdUtil;
//import com.liepin.swift.framework.mvc.compress.ContentEncoding;
//import com.liepin.swift.framework.mvc.contentType.ContentType;
//import com.liepin.swift.framework.mvc.filter.GenericFilter;
//import com.liepin.swift.framework.mvc.util.GwHeadReader;
//import com.liepin.swift.framework.mvc.util.HeadReader;
//import com.liepin.swift.framework.mvc.util.JsonBodyPathFinder;
//import com.liepin.swift.framework.mvc.util.RequestUtil;
//import com.liepin.swift.framework.util.RootDomainUtil;
//
//@Deprecated
//public class InputParamFilter extends GenericFilter {
//
//    private static final Logger logger = Logger.getLogger(InputParamFilter.class);
//
//    private static final Set<String> PARAMKEY_ORDER = new LinkedHashSet<String>();
//
//    // 是否针对rpc处理
//    private boolean rpcFilter;
//
//    private static final Set<String> COOKIE_NAMES = new HashSet<>();
//
//    public InputParamFilter(boolean rpcFilter) {
//        this.rpcFilter = rpcFilter;
//        try {
//            initFilterBean();
//        } catch (ServletException e) {
//            throw new RuntimeException("InputParamFilter初始化Bean失败!", e);
//        }
//    }
//
//    static {
//        String value = PropUtil.getInstance().get("request.cookie.debug");
//        if (value != null) {
//            String[] array = value.split("\\,");
//            for (String path : array) {
//                if (path == null || path.trim().length() == 0) {
//                    continue;
//                }
//                COOKIE_NAMES.add(path);
//            }
//        }
//    }
//
//    @Override
//    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
//            FilterChain filterChain) throws ServletException, IOException {
//        Map<String, Object> input = getInput(request);
//        if (input == null) {
//            input = new HashMap<String, Object>();
//        }
//        // 针对非RPC请求埋点
//        if (!rpcFilter) {
//            // 初始化追踪id
//            doTraceId(input);
//
//            // 初始化clientId
//            doClientId(input);
//
//            // 请求发起url
//            ThreadLocalUtil.getInstance().setInitiateUrl(request.getServletPath());
//            // cookie debug
//            cookieDebug(request);
//
//            // 初始化请求逻辑区标识
//            // 动态环境从header获取qrea
//            String area = getArea(request);
//            if (area == null || area.trim().length() == 0) {
//                area = (String) input.get(Const.AREA);
//                if (area == null || area.trim().length() == 0) {
//                    area = SystemUtil.getLogicAreaStr();
//                }
//            }
//            area = area.trim().toLowerCase();
//            input.put(Const.AREA, area);
//            ThreadLocalUtil.getInstance().setArea(area);
//
//            // 初始化请求的根域名
//            String rootDomain = RootDomainUtil.getCrrentRootDomain(request);
//            input.put(Const.ROOT_DOMAIN, rootDomain);
//            ThreadLocalUtil.getInstance().setRootDomain(rootDomain);
//
//            // 初始化流量灰度标记
//            doGrayId(input);
//        } else {
//            // API网关
//            // 初始化网关客户端信息信息传递参数
//            String gwClientInfoStr = request.getHeader("X-Gw-Client-Info".toLowerCase());
//            gwClientInfoStr = Optional.ofNullable(gwClientInfoStr).map(t -> {
//                try {
//                    return URLDecoder.decode(t, "UTF-8");
//                } catch (Exception e) {
//                    return t;
//                }
//            }).orElse(null);
//            input.put(Const.GW_CLIENT_INFO, gwClientInfoStr);
//            // 请求是否来自网关
//            String xAltGw = request.getHeader("X-Alt-Gw");
//            Optional.ofNullable(RequestUtil.parseHeaderXAltGw4Type(xAltGw)).ifPresent(t -> {
//                ThreadLocalUtil.getInstance().setRequestFromGw(t);
//            });
//
//            // 初始化流量灰度标记
//            doGrayId(request, input);
//        }
//        // 参数排序
//        if (input.size() > 1) {
//            input = sortInputParams(input);
//        }
//        RequestUtil.setInput(request, input);
//        filterChain.doFilter(request, response);
//    }
//
//    @Override
//    protected String urlPattern() {
//        return "/*";
//    }
//
//    @Override
//    protected void initFilterBean() throws ServletException {
//        PARAMKEY_ORDER.add(Const.CLIENT_IDS);
//        PARAMKEY_ORDER.add(Const.CURRENT_USER_ID);
//        PARAMKEY_ORDER.add(Const.ORIGINAL_IP);
//        PARAMKEY_ORDER.add(Const.TRACEID);
//        PARAMKEY_ORDER.add(Const.FLOW_GRAY_ID);
//        PARAMKEY_ORDER.add(Const.INITIATE_URL);
//        PARAMKEY_ORDER.add(Const.AREA);
//        PARAMKEY_ORDER.add(Const.ROOT_DOMAIN);
//        PARAMKEY_ORDER.add(Const.TRANSMIT_EXTEND);
//        PARAMKEY_ORDER.add(Const.VERSION);
//        PARAMKEY_ORDER.add(Const.TIME_RIVER);
//        PARAMKEY_ORDER.add(Const.GW_CLIENT_INFO);
//    }
//
//    protected Map<String, Object> getInput(HttpServletRequest request) throws BizException {
//        Map<String, Object> input = null;
//        // 排除
//        if (!rpcFilter && JsonBodyPathFinder.match(request.getServletPath())) {
//            return input;
//        }
//        // contentType排除
//        ContentType contentType = ContentType.support(request.getContentType());
//        if (contentType == null) {
//            MonitorLogger.getInstance().log("不支持的Http Content-Type请求: Content-Type:" + request.getContentType()
//                    + ", ServletPath=" + request.getServletPath() + ", Referer=" + request.getHeader("referer"));
//            return input;
//        }
//        // 解析
//        try {
//            // 压缩请求处理
//            ContentEncoding contentEncoding = ContentEncoding.support(request);
//            if (Objects.nonNull(contentEncoding)) {
//                // 来自access网关、rpc的请求有压缩的场景
//                if (!isInvalid(request)) {
//                    String content = contentEncoding.decompress(request);
//                    input = Optional.ofNullable(content).map(t -> {
//                        try {
//                            return contentType.transform(t);
//                        } catch (Exception e) {
//                            logger.error("contentType=" + contentType.getContentType() + "的请求转换失败: " + e.getMessage(),
//                                    e);
//                            return null;
//                        }
//                    }).orElse(null);
//                }
//            } else {
//                // FIXME 兼容老的请求，待后续全部接入网关后、框架ins-swift-router版本>=2.0.17都升级完后去掉
//                if (RequestUtil.needGzipHandle(request, rpcFilter)) {
//                    // FIXME
//                    // 去掉app压缩请求，暂时使用X-Client-Type=app做为与老请求的区分，因为要兼容老得请求；并且有此header的请求都走网关解压缩了
//                    if (HeadReader.isAppRequest(request)) {
//                        // 理论：来自app请求场景
//                        byte[] inputStreamToByte = IoUtil.inputStreamToByte(request.getInputStream());
//                        input = JsonUtil.json2map(new String(inputStreamToByte, "UTF-8"));
//                    } else {
//                        if (!isInvalid(request)) {
//                            String content = GzipUtil.uncompress(request.getInputStream());
//                            if (content != null && content.trim().length() != 0) {
//                                if (ContentType.JSON == contentType) {
//                                    // 理论：来自app、rpc请求场景
//                                    input = ContentType.JSON.transform(content);
//                                } else if (ContentType.XMLTEXT == contentType) {
//                                    // 理论：无场景，待下线
//                                    input = ContentType.XMLTEXT.transform(content);
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    if (RequestUtil.isWxaRequest(request) && ContentType.MULTIPART_FORMDATA != contentType) {
//                        // 理论：来自wxa的请求场景
//                        // wxa的文件上传排除
//                        // TODO 临时方案
//                        byte[] inputStreamToByte = IoUtil.inputStreamToByte(request.getInputStream());
//                        String json = new String(inputStreamToByte, "UTF-8");
//                        input = JsonUtil.json2map(json);
//                    } else {
//                        if (ContentType.FROM == contentType) {
//                            // 理论：来自gw、rpc、ajax、page的请求场景
//                            input = ContentType.FROM.transform(request);
//                        } else if (ContentType.JSON == contentType) {
//                            // 理论：无场景，待下线
//                            input = ContentType.JSON.transform(request);
//                        } else if (ContentType.XMLTEXT == contentType) {
//                            // 理论：无场景，待下线
//                            input = ContentType.XMLTEXT.transform(request);
//                        } else if (ContentType.MULTIPART_FORMDATA == contentType && RequestUtil.isAppRequest(request)) {
//                            // 理论：来自app、wxa的请求场景
//                            // 页面请求交给Spring MVC 处理，只处理app、wxa的上传图片请求
//                            input = ContentType.MULTIPART_FORMDATA.transform(request);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            MonitorLogger.getInstance().log("读取请求数据失败: " + printHeader(request), e);
//            if (e instanceof BizException) {
//                throw (BizException) e;
//            }
//            throw new BizException(SystemEnum.INVALID, e);
//        }
//        if (Objects.isNull(input)) {
//            MonitorLogger.getInstance().log("读取请求数据失败或有没识别的Content-Type处理逻辑: " + printHeader(request));
//        }
//        return input;
//    }
//
//    protected Map<String, Object> sortInputParams(Map<String, Object> map) {
//        Map<String, Object> sortMap = new LinkedHashMap<String, Object>();
//        if (map != null && map.size() > 0) {
//            for (String key : PARAMKEY_ORDER) {
//                Object value = map.get(key);
//                if (value != null) {
//                    sortMap.put(key, value);
//                }
//            }
//            for (Map.Entry<String, Object> entry : map.entrySet()) {
//                if (PARAMKEY_ORDER.contains(entry.getKey())) {
//                    continue;
//                }
//                sortMap.put(entry.getKey(), entry.getValue());
//            }
//        }
//        return sortMap;
//    }
//
//    private void cookieDebug(final HttpServletRequest request) {
//        if (COOKIE_NAMES.size() > 0) {
//            Cookie[] cookies = request.getCookies();
//            if (cookies != null) {
//                StringBuilder cookieLog = new StringBuilder();
//                for (Cookie cookie : cookies) {
//                    if (COOKIE_NAMES.contains(cookie.getName())) {
//                        if (cookieLog.length() != 0) {
//                            cookieLog.append(",");
//                        }
//                        cookieLog.append(cookie.getName()).append("=").append(cookie.getValue());
//                    }
//                }
//                if (cookieLog.length() > 0) {
//                    MonitorLogger.getInstance().log("Request Cookie Debug: ServletPath=" + request.getServletPath()
//                            + ", cookies: " + cookieLog.toString());
//                }
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private String getTraceId(Object object) {
//        if (object instanceof List) {
//            List<String> traceIds = (List<String>) object;
//            if (traceIds.size() > 0) {
//                return (String) traceIds.get(0);
//            }
//        } else if (object instanceof String) {
//            return (String) object;
//        }
//        return null;
//    }
//
//    private void doTraceId(final Map<String, Object> input) {
//        // 兼容场景：一种是接网关的、一种是不接网关的
//        // 先判断网关请求
//        FscpHeader fscpHeader = ThreadLocalUtil.getInstance().getFscpHeader();
//        if (Objects.nonNull(fscpHeader) && Objects.nonNull(fscpHeader.getxFscpTraceId())) {
//            if (!input.containsKey(Const.TRACEID)) {
//                input.put(Const.TRACEID, fscpHeader.getxFscpTraceId());// event日志显示用
//            }
//            return;
//        }
//        // 再判断非网关请求
//        if (!input.containsKey(Const.TRACEID)) {
//            input.put(Const.TRACEID, TraceIdUtil.createTraceId().toString());
//        } else {
//            TraceIdUtil.trace(getTraceId(input.get(Const.TRACEID)));
//        }
//    }
//
//    private void doClientId(final Map<String, Object> input) {
//        FscpHeader fscpHeader = ThreadLocalUtil.getInstance().getFscpHeader();
//        Optional.ofNullable(fscpHeader).map(t -> t.getxFscpStdInfoBeanBean()).map(k -> k.getClientId())
//                .ifPresent(clientId -> {
//                    if (!input.containsKey(Const.CLIENT_IDS)) {
//                        input.put(Const.CLIENT_IDS, clientId);// event日志显示用
//                    }
//                });
//    }
//
//    private void doGrayId(final Map<String, Object> input) {
//        // 用户流量灰度标记
//        Optional.ofNullable(ThreadLocalUtil.getInstance().getFlowGrayId()).ifPresent(grayId -> {
//            input.put(Const.FLOW_GRAY_ID, grayId);// event日志显示用
//        });
//    }
//
//    private void doGrayId(final HttpServletRequest request, final Map<String, Object> input) {
//        // 用户流量灰度标记
//        Optional.ofNullable(GwHeadReader.getGcId(request)).ifPresent(gcId -> {
//            ThreadLocalUtil.getInstance().setFlowGrayId(gcId);
//            input.put(Const.FLOW_GRAY_ID, gcId);// event日志显示用
//        });
//    }
//
//}
