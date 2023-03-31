package com.liepin.swift.framework.mvc.dispatcher;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.common.datastructure.Pair;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.framework.limit.Hourglass;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.plugin.PluginContext;
import com.liepin.swift.framework.plugin.gateway.GatewayPlugin;
import com.liepin.swift.framework.plugin.gateway.interceptor.GwApiInterceptorPlugin;
import com.liepin.swift.framework.plugin.gateway.interceptor.chain.DefaultGwApiInterceptorChain;
import com.liepin.swift.framework.plugin.gateway.interceptor.chain.GwApiContext;
import com.liepin.swift.framework.plugin.gateway.interceptor.chain.GwApiInterceptorProxy;
import com.liepin.swift.framework.plugin.service.ServicePlugin;
import com.liepin.swift.framework.rpc.deserialize.MethodValueDeserialize;
import com.liepin.swift.framework.util.UrlUtil;

@SuppressWarnings("serial")
public class ServiceDispatcher extends AbstractAdaptorDispatcherServlet {

    private static final Logger logger = Logger.getLogger(ServiceDispatcher.class);

    private ServicePlugin servicePlugin;
    private GatewayPlugin gatewayPlugin;
    private GwApiInterceptorPlugin gwApiInterceptorPlugin;

    private MethodValueDeserialize methodValueDeserialize = new MethodValueDeserialize();

    @Override
    protected void initDispatchBean() {
        super.initDispatchBean();
        this.servicePlugin = PluginContext.get().getPlugin(ServicePlugin.class);
        this.gatewayPlugin = PluginContext.get().getPlugin(GatewayPlugin.class);
        this.gwApiInterceptorPlugin = PluginContext.get().getPlugin(GwApiInterceptorPlugin.class);
        logger.info("ServiceDispatcher injected ServicePlugin, GatewayPlugin, GwApiInterceptorPlugin.");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DispatcherMethodBean mappingRPC(String servletPath) {
        String[] array = UrlUtil.uncompile(servletPath);
        if (array.length != 2) {
            return null;
        }
        String serviceName = array[0];
        String methodName = array[1];
        return servicePlugin.getServiceMethod(serviceName, methodName);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DispatcherMethodBean mappingGW(String servletPath) {
        return gatewayPlugin.getGatewayMethod(servletPath);
    }

    @Override
    protected void serviceApi(DispatcherBean bean, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Map<String, Object> input = RequestUtil.getInput(request);
        if (Objects.isNull(input)) {
            throw new BizException(SystemEnum.PARAMETER_EXCEPTION);
        }
        String json = (String) input.get(Const.DATA);
        DispatcherMethodBean methodBean = (DispatcherMethodBean) bean;
        Object[] args = methodValueDeserialize.fromJson(json, methodBean.paramMap);
        Object output = null;
        // 限流插桩
        Pair<Boolean, Object> entry = Hourglass.entry(args);
        if (entry.getFirst().booleanValue()) {
            output = bean.invoke(args);
        } else {
            output = entry.getSecond();
        }
        RequestUtil.setOutput(request, output);
    }

    @Override
    protected void gwApi(DispatcherBean bean, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Map<String, Object> input = RequestUtil.getInput(request);
        if (Objects.isNull(input)) {
            throw new BizException(SystemEnum.PARAMETER_EXCEPTION);
        }
        String json = (String) input.get(Const.DATA);
        if (Objects.isNull(json)) {
            throw new BizException(SystemEnum.PARAMETER_EXCEPTION);
        }
        DispatcherMethodBean methodBean = (DispatcherMethodBean) bean;
        Object[] args = methodValueDeserialize.fromJson(json, methodBean.paramMap);
        Object output = null;
        // interceptor
        List<GwApiInterceptorProxy> interceptors = gwApiInterceptorPlugin.getObject();
        if (interceptors.size() > 0) {
            DefaultGwApiInterceptorChain processorChain = DefaultGwApiInterceptorChain.newProcessorChain(interceptors,
                    new GwApiContext(bean, args));
            output = processorChain.call();
        } else {
            output = bean.invoke(args);
        }
        RequestUtil.setOutput(request, output);
    }

}
