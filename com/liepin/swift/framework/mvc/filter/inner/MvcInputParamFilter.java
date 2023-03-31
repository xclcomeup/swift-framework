package com.liepin.swift.framework.mvc.filter.inner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.common.conf.SystemUtil;
import com.liepin.swift.core.bean.FscpHeader;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.mvc.contentType.ContentType;
import com.liepin.swift.framework.mvc.filter.GenericFilter;
import com.liepin.swift.framework.mvc.http.StreamReplicationHttpServletRequestWrapper;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.plugin.IPlugin;
import com.liepin.swift.framework.plugin.IPluginListener;
import com.liepin.swift.framework.plugin.controller.ControllerPlugin;
import com.liepin.swift.framework.util.RootDomainUtil;

public class MvcInputParamFilter extends GenericFilter implements IPluginListener {

    private static final Logger logger = Logger.getLogger(MvcInputParamFilter.class);

    private ControllerPlugin controllerPlugin;

    public MvcInputParamFilter() {
        ControllerPlugin.listen(this);
    }

    @Override
    public void handle(IPlugin<?> plugin) {
        this.controllerPlugin = (ControllerPlugin) plugin;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // 获取代理request
        HttpServletRequest requestWrapper = getRequestWrapper(request);
        // 获取请求参数
        Map<String, Object> input = Optional.ofNullable(getInput(requestWrapper)).orElseGet(HashMap::new);

        // 初始化追踪id
        doTraceId(input);

        // 初始化clientId
        doClientId(input);

        // 初始化deviceUuid
        doDeviceUuid(input);

        // 初始化请求发起url
        ThreadLocalUtil.getInstance().setInitiateUrl(request.getServletPath());

        // 初始化请求逻辑区标识
        doArea(input, request);

        // 初始化请求的根域名
        doRootDomain(input, request);

        // 初始化流量灰度标记
        doGrayId(input);

        RequestUtil.setInput(request, input);
        filterChain.doFilter(requestWrapper, response);
    }

    @Override
    protected String urlPattern() {
        return "/*";
    }

    /**
     * 判断请求流是否需要代理
     * 
     * @param request
     * @return
     */
    protected HttpServletRequest getRequestWrapper(HttpServletRequest request) {
        // 排除GET请求
        if (RequestUtil.isGetRequest(request)) {
            return request;
        }
        // json格式
        ContentType contentType = ContentType.support(request.getContentType());
        return Optional.ofNullable(contentType).filter(t -> ContentType.JSON == t)
                .map(k -> (HttpServletRequest) new StreamReplicationHttpServletRequestWrapper(request,
                        controllerPlugin.isNoPack4Input(request.getServletPath())))
                .orElse(request);
    }

    protected Map<String, Object> getInput(HttpServletRequest request) {
        Map<String, Object> input = null;
        // 排除
        // if (RequestBodyPathFinder.match(request.getServletPath())) {
        // return input;
        // }

        // contentType
        ContentType contentType = ContentType.support(request.getContentType());
        if (contentType == null) {
            MonitorLogger.getInstance().log("不支持的Http Content-Type请求: Content-Type:" + request.getContentType()
                    + ", ServletPath=" + request.getServletPath() + ", Referer=" + request.getHeader("referer"));
            return input;
        }
        // 向下兼容目前还有使用xml并且从input获取数据的场景
        if (ContentType.XMLTEXT == contentType && controllerPlugin.isNoPack4Input(request.getServletPath())) {
            return input;
        }

        // 解析
        // GET请求，按FORM解析
        if (RequestUtil.isGetRequest(request) && ContentType.FORM != contentType) {
            contentType = ContentType.FORM;
        }
        try {
            input = contentType.transform(request);
        } catch (Exception e) {
            logger.error(
                    "读取" + request.getServletPath() + "的请求参数失败: " + e.getMessage() + ". 打印: " + printHeader(request),
                    e);
            throw new SysException(SystemEnum.INVALID, e);
        }
        if (Objects.isNull(input)) {
            logger.warn("读取" + request.getServletPath() + "的请求参数为空. 打印: " + printHeader(request));
        }
        return input;
    }

    @SuppressWarnings("unchecked")
    private String getTraceId(Object object) {
        if (object instanceof List) {
            List<String> traceIds = (List<String>) object;
            if (traceIds.size() > 0) {
                return (String) traceIds.get(0);
            }
        } else if (object instanceof String) {
            return (String) object;
        }
        return null;
    }

    private void doTraceId(final Map<String, Object> input) {
        // 兼容场景：一种是接网关的、一种是不接网关的
        // 先判断网关请求
        FscpHeader fscpHeader = ThreadLocalUtil.getInstance().getFscpHeader();
        if (Objects.nonNull(fscpHeader) && Objects.nonNull(fscpHeader.getxFscpTraceId())) {
            if (!input.containsKey(Const.TRACEID)) {
                input.put(Const.TRACEID, fscpHeader.getxFscpTraceId());// event日志显示用
            }
            return;
        }
        // 再判断非网关请求
        if (!input.containsKey(Const.TRACEID)) {
            input.put(Const.TRACEID, TraceIdUtil.createTraceId().toString());
        } else {
            TraceIdUtil.trace(getTraceId(input.get(Const.TRACEID)));
        }
    }

    private void doClientId(final Map<String, Object> input) {
        // 兼容场景：一种是接网关的、一种是不接网关的
        // 先判断网关请求
        FscpHeader fscpHeader = ThreadLocalUtil.getInstance().getFscpHeader();
        if (Objects.nonNull(fscpHeader) && Objects.nonNull(fscpHeader.getxFscpStdInfoBeanBean())
                && Objects.nonNull(fscpHeader.getxFscpStdInfoBeanBean().getClientId())) {
            if (!input.containsKey(Const.CLIENT_IDS)) {
                input.put(Const.CLIENT_IDS, fscpHeader.getxFscpStdInfoBeanBean().getClientId());// event日志显示用
            }
            return;
        }
        // 再判断非网关请求
        Optional.ofNullable(input.get("client_id")).ifPresent(clientId -> {
            if (clientId instanceof String) {
                ThreadLocalUtil.getInstance().setClientId(new String[] { (String) clientId });
            } else if (clientId instanceof Integer) {
                ThreadLocalUtil.getInstance().setClientId(new String[] { ((Integer) clientId).toString() });
            }
        });
    }

    private void doDeviceUuid(final Map<String, Object> input) {
        // 兼容网关模式
        FscpHeader fscpHeader = ThreadLocalUtil.getInstance().getFscpHeader();
        if (Objects.nonNull(fscpHeader) && Objects.nonNull(fscpHeader.getxFscpStdInfoBeanBean())
                && Objects.nonNull(fscpHeader.getxFscpStdInfoBeanBean().getDeviceUuid())) {
            if (!input.containsKey("device_uuid")) {
                input.put("device_uuid", fscpHeader.getxFscpStdInfoBeanBean().getDeviceUuid());// event日志显示用
            }
            return;
        }
        // 再判断非网关请求
        Optional.ofNullable(
                Optional.ofNullable((String) input.get("device_uuid")).orElse((String) input.get(Const.DEVICE_UUID)))
                .ifPresent(deviceUuid -> {
                    ThreadLocalUtil.getInstance().setDeviceUuid(deviceUuid);
                });
    }

    private void doGrayId(final Map<String, Object> input) {
        // 用户流量灰度标记
        Optional.ofNullable(ThreadLocalUtil.getInstance().getFlowGrayId()).ifPresent(grayId -> {
            input.put(Const.FLOW_GRAY_ID, grayId);// event日志显示用
        });
    }

    private void doArea(final Map<String, Object> input, final HttpServletRequest request) {
        // 动态环境从header获取qrea
        String area = Optional.ofNullable(HeadReader.getArea(request)).orElseGet(() -> {
            return Optional.ofNullable((String) input.get(Const.AREA)).orElseGet(() -> {
                return SystemUtil.getLogicAreaStr();
            });
        }).trim().toLowerCase();

        // String area = HeadReader.getArea(request);
        // if (area == null || area.trim().length() == 0) {
        // area = (String) input.get(Const.AREA);
        // if (area == null || area.trim().length() == 0) {
        // area = SystemUtil.getLogicAreaStr();
        // }
        // }
        // area = area.trim().toLowerCase();
        input.put(Const.AREA, area);
        ThreadLocalUtil.getInstance().setArea(area);
    }

    private void doRootDomain(final Map<String, Object> input, final HttpServletRequest request) {
        String rootDomain = RootDomainUtil.getCrrentRootDomain(request);
        input.put(Const.ROOT_DOMAIN, rootDomain);
        ThreadLocalUtil.getInstance().setRootDomain(rootDomain);
    }

}
