package com.liepin.swift.framework.mvc.filter.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.monitor.cat.CatConsumer;
import com.liepin.swift.framework.mvc.ResultStatus;
import com.liepin.swift.framework.mvc.eventInfo.Event;
import com.liepin.swift.framework.mvc.filter.AbstractFilterHandler;
import com.liepin.swift.framework.mvc.rest.json.RpcObjectBuilder;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.CatHelper;
import com.liepin.swift.framework.util.RpcProtocol;
import com.liepin.swift.framework.util.RpcProtocol.ErrorType;
import com.liepin.swift.framework.util.UrlUtil;

public class RpcFilterHandler extends AbstractFilterHandler implements FilterHandler {

    /**
     * RPC拦截链
     */
    private List<Filter> rpcFilterChains;

    public RpcFilterHandler(List<Filter> rpcFilterChains) {
        this.rpcFilterChains = rpcFilterChains;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        return UrlUtil.isRPC(request.getServletPath());
    }

    @Override
    public Event newEvent(HttpServletRequest request) {
        Event event = super.newEvent(request);
        event.setType("Service");
        event.setName(logRequestServiceName(request));
        return event;
    }

    @Override
    public ResultStatus handle(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 执行拦截器
        doFilterProxy(request, response, rpcFilterChains, null);

        // 参数map
        Map<String, Object> inputMap = RequestUtil.getInput(request);

        // 预处理
        preprocessor.preprocess(inputMap, request);

        // cat初始化
        initCat(inputMap, request);

        filterChain.doFilter(request, response);

        return ResultStatus.ok().setData(RequestUtil.getOutput(request));
    }

    @Override
    public ResultStatus resolveException(HttpServletRequest request, HttpServletResponse response, Throwable e) {
        Throwable temp = e;
        if (exceptionInterceptor != null) {
            // 降级处理
            try {
                Object fallbackObj = exceptionInterceptor.intercept(request.getServletPath(), temp);
                return ResultStatus.ok().setData(fallbackObj);
            } catch (Throwable throwable) {
                temp = throwable;
            }
        }

        ResultStatus rs;
        try {
            rs = exceptionHandler.handle(request, temp);
        } catch (Throwable t) {
            catalinaLog.error("traceId=" + TraceIdUtil.getTraceId() + " " + t.getMessage(), t);
            rs = ResultStatus.unknown(t.getMessage());
        }
        // 如果biz异常header返回标示
        if (Objects.nonNull(rs.getBizCode())) {
            RpcProtocol.addBizCodeHeader(response, rs.getBizCode());
        } else {
            RpcProtocol.addErrorTypeHeader(response, ErrorType.sys);
        }
        return rs;
    }

    @Override
    public String output(HttpServletRequest request, HttpServletResponse response, ResultStatus rs) throws Exception {
        // 包装返回数据结构
        String outputStr = RpcObjectBuilder.response(rs);

        // 后置处理
        postprocessor.postprocess(request);

        // 输出响应CAT埋点
        CatHelper.point("Response", "common", new CatConsumer() {

            @Override
            public void accept() throws Exception {
                export(request, response, outputStr);
            }

        });

        return outputStr;
    }

    /**
     * cat记录 Type=Service 名字
     * 
     * @param req
     * @return
     */
    private String logRequestServiceName(HttpServletRequest req) {
        String path = UrlUtil.compile(req.getServletPath(), UrlUtil.getNamespace4API());
        String[] array = UrlUtil.uncompile(path);
        return (array.length == 2) ? array[0] + ":" + array[1] : "unknow";
    }

}
