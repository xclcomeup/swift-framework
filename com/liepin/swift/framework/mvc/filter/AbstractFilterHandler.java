package com.liepin.swift.framework.mvc.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.MessageProducer;
import com.dianping.cat.message.spi.MessageTree;
import com.liepin.common.conf.ProjectIdMap;
import com.liepin.swift.core.exception.IMessageCode;
import com.liepin.swift.core.util.CatUtil;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.compress.AcceptEncoding;
import com.liepin.swift.framework.mvc.contentType.ContentType;
import com.liepin.swift.framework.mvc.eventInfo.DefaultEvent;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.filter.handler.FilterHandler;
import com.liepin.swift.framework.mvc.resolver.IExceptionInterceptor;
import com.liepin.swift.framework.mvc.resolver.IExceptionResolver;
import com.liepin.swift.framework.mvc.util.HeadReader;
import com.liepin.swift.framework.mvc.util.RequestUtil;

public abstract class AbstractFilterHandler implements FilterHandler {

    protected final Logger catalinaLog = Logger.getLogger(getClass());

    protected AbstractExceptionHandler exceptionHandler = new AbstractExceptionHandler() {};
    protected AbstractPreprocessor preprocessor = new AbstractPreprocessor() {};
    protected AbstractPostprocessor postprocessor = new AbstractPostprocessor() {};

    protected IExceptionResolver exceptionResolver;
    protected IExceptionInterceptor exceptionInterceptor;

    /**
     * 路径匹配
     */
    private final PathMatcher pathMatcher = new DefaultPathMatcher();

    /**
     * 非RPC拦截链
     */
    private List<GenericFilter> externalFilterChains;

    public AbstractFilterHandler() {
    }

    public AbstractFilterHandler setExceptionHandler(AbstractExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public AbstractFilterHandler setPreprocessor(AbstractPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
        return this;
    }

    public AbstractFilterHandler setPostprocessor(AbstractPostprocessor postprocessor) {
        this.postprocessor = postprocessor;
        return this;
    }

    public AbstractFilterHandler setExternalFilterChains(List<GenericFilter> externalFilterChains) {
        this.externalFilterChains = externalFilterChains;
        return this;
    }

    public AbstractFilterHandler setExceptionResolver(IExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
        return this;
    }

    public AbstractFilterHandler setExceptionInterceptor(IExceptionInterceptor exceptionInterceptor) {
        this.exceptionInterceptor = exceptionInterceptor;
        return this;
    }

    /**
     * 
     * @param request
     * @param response
     * @param filters
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    protected void doFilterProxy(HttpServletRequest request, HttpServletResponse response, final List<Filter> filters,
            FilterChain filterChain) throws ServletException, IOException {
        if (filters != null && filters.size() > 0) {
            ProxyFilterChain pfc = new ProxyFilterChain(filters, filterChain);
            pfc.doFilter(request, response);
        }
    }

    /**
     * 根据路径动态获取链式拦截器
     * <p>
     * 使用：request.getServletPath()<br>
     * 
     * @param servletPath
     * @return
     */
    protected List<Filter> getFilters(String servletPath) {
        List<Filter> list = new ArrayList<Filter>();
        for (GenericFilter filter : externalFilterChains) {
            String urlPattern = filter.urlPattern();
            if (pathMatcher.match(urlPattern, servletPath)) {
                list.add(filter);
            }
        }
        return list;
    }

    /**
     * 输出内容
     * 
     * @param request
     * @param response
     * @param output
     * @throws Exception
     */
    protected void export(HttpServletRequest request, HttpServletResponse response, String output) throws Exception {
        response.setContentType(ContentType.JSON_UTF8.getContentType());
        if (HeadReader.isAcceptEncodingRequest(request)) {
            AcceptEncoding.gzip.compress(response, output);
        } else {
            PrintWriter writer = response.getWriter();
            writer.print(output);
            writer.flush();
        }
    }

    protected ResultStatus toResultStatus(final Throwable throwable) {
        if (throwable instanceof IMessageCode) {
            IMessageCode messageCode = (IMessageCode) throwable;
            return new ResultStatus(messageCode);
        } else {
            return ResultStatus.unknown();
        }
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        DefaultEvent eventInfo = new DefaultEvent();
        eventInfo.begin();
        eventInfo.setClientIP(RequestUtil.getClientIp(request));// 默认上一跳ip
        eventInfo.setActionPath(request.getServletPath());
        return eventInfo;
    }

    /**
     * 初始化cat
     * 
     * @param inputMap
     * @param request
     */
    protected void initCat(final Map<String, Object> inputMap, final HttpServletRequest request) {
        String[] messageIds = CatUtil.catMessageIds(inputMap);

        MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
        tree.setMessageId(messageIds[2]);
        tree.setParentMessageId(messageIds[1]);
        tree.setRootMessageId(messageIds[0]);

        MessageProducer cat = Cat.getProducer();
        initRequestClientInfo(cat, request, messageIds[3]);
    }

    private void initRequestClientInfo(MessageProducer cat, HttpServletRequest req, String clientPort) {
        StringBuilder sb = new StringBuilder(1024);
        String ip = "";
        String ipForwarded = req.getHeader("x-forwarded-for");

        if (ipForwarded == null) {
            ip = RequestUtil.getClientIp(req);
        } else {
            String ips[] = ipForwarded.split(",");
            ip = ips[ips.length - 1].trim();
        }

        sb.append("ClientIP=").append(ip);
        sb.append("&InitialIP=").append(ThreadLocalUtil.getInstance().getOriginalIP());
        // sb.append("&VirtualIP=").append(req.getRemoteAddr());
        // sb.append("&Server=").append(req.getServerName());
        sb.append("&Referer=").append(req.getHeader("referer"));
        sb.append("&Agent=").append(req.getHeader("user-agent"));

        cat.logEvent("Service", "ClientInfo", Message.SUCCESS, sb.toString());
        String[] clientIds = ThreadLocalUtil.getInstance().getClientId();
        String clientId = (clientIds.length != 0) ? clientIds[clientIds.length - 1] : "unkown";
        Cat.logEvent("ClientId", clientId);
        Cat.logEvent("Service.app", ProjectIdMap.projectName(clientId));
        Cat.logEvent("Service.client", (clientPort != null) ? ip + ":" + clientPort : ip);
    }

}
