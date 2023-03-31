package com.liepin.swift.framework.rpc.router;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.liepin.common.conf.ProjectId;
import com.liepin.common.json.JsonUtil;
import com.liepin.router.discovery.ServicePortUtil;
import com.liepin.router.handler.IPreprocessor;
import com.liepin.swift.core.bean.RpcContext;
import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceIdUtil;
import com.liepin.swift.framework.conf.SwiftConfig;

public class CommonPreprocessor implements IPreprocessor {

    private static final Logger logger = Logger.getLogger(CommonPreprocessor.class);

    private String projectName;
    private String version;
    private String myselfPort;

    public CommonPreprocessor(String projectName, String version) {
        this.projectName = projectName;
        this.version = version;
        try {
            this.myselfPort = ServicePortUtil.getServerPort() + "";
        } catch (Exception e) {
            this.myselfPort = "unknow";
        }
    }

    @Override
    public Map<String, Object> preprocess(Object data) {
        lostRecord();
        Map<String, Object> params = new HashMap<String, Object>();
        putIfExist(params, Const.CURRENT_USER_ID, ThreadLocalUtil.getInstance().getCurrentUserId());
        putIfExist(params, Const.ORIGINAL_IP, ThreadLocalUtil.getInstance().getOriginalIP());
        putIfExist(params, Const.BI_INFO, ThreadLocalUtil.getInstance().getBIInfo());
        putIfExist(params, Const.DEVICE_UUID, ThreadLocalUtil.getInstance().getDeviceUuid());
        putIfExist(params, Const.VERSION, version);
        // 跟踪请求链路随机唯一标示
        putIfExist(params, Const.TRACEID, TraceIdUtil.getTraceId());
        // 跟踪请求逻辑区标示
        String area = ("".equals(ThreadLocalUtil.getInstance().getArea()))
                ? com.liepin.common.conf.SystemUtil.getLogicAreaStr() : ThreadLocalUtil.getInstance().getArea();
        putIfExist(params, Const.AREA, area);
        // 请求发起时间
        putIfExist(params, Const.TIME_RIVER, RpcContext.current().getTimePlan());
        // 初始请求url
        putIfExist(params, Const.INITIATE_URL, ThreadLocalUtil.getInstance().getInitiateUrl());
        // 初始化请求根域名
        putIfExist(params, Const.ROOT_DOMAIN, ThreadLocalUtil.getInstance().getRootDomain());
        // 流量灰度id
        putIfExist(params, Const.FLOW_GRAY_ID, ThreadLocalUtil.getInstance().getFlowGrayId());
        // 自定义扩展透传字段
        putIfExist(params, Const.TRANSMIT_EXTEND, JsonUtil.toJson(ThreadLocalUtil.getInstance().getExtend()));
        putIfExist(params, Const.DATA, JsonUtil.toJson(data));

        // 追加访问链
        String[] clientIds = ThreadLocalUtil.getInstance().getClientId();
        String[] clientIdArray = new String[clientIds.length + 1];
        System.arraycopy(clientIds, 0, clientIdArray, 0, clientIds.length);
        clientIdArray[clientIdArray.length - 1] = ProjectId.getClientId();
        putIfExist(params, Const.CLIENT_IDS, toString(clientIdArray));

        // 切换了线程 忽略cat统计
        if (Cat.getManager().getThreadLocalMessageTree() != null) {
            String rootId = Cat.getManager().getThreadLocalMessageTree().getRootMessageId();
            String currentId = Cat.getManager().getThreadLocalMessageTree().getMessageId();
            String childId = Cat.getProducer().createMessageId();
            Cat.logEvent("Call.port", myselfPort);// 本服务tomcat启动端口
            Cat.logEvent("Call.app", projectName);// 接口服务方项目名
            Cat.logEvent("RemoteCall", getClass().getSimpleName(), Message.SUCCESS, childId);
            Map<String, String> catMap = new HashMap<String, String>();
            catMap.put("CAT_ROOT_ID", rootId == null ? currentId : rootId);
            catMap.put("CAT_PARENT_ID", currentId);
            catMap.put("CAT_CHILD_ID", childId);
            catMap.put("SERVER_PORT", myselfPort);// 告诉服务方请求方的启动端口
            putIfExist(params, "cat", JsonUtil.toJson(catMap));
        }

        return params;
    }

    private String toString(String[] array) {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                log.append(",");
            }
            log.append(array[i]);
        }
        return log.toString();
    }

    private void putIfExist(final Map<String, Object> params, String key, String value) {
        if (value != null && value.length() != 0) {
            params.put(key, value);
        }
    }

    private void lostRecord() {
        if (!SwiftConfig.enableRpcParamsCheck()) {
            return;
        }
        // 暂时只监控非tomcat线程，也就是业务自定义创建的线程
        if (!Thread.currentThread().getName().startsWith("catalina-exec-")) {
            if (Objects.isNull(ThreadLocalUtil.getInstance().getFlowGrayId())) {
                logger.error("#业务创建线程发起rpc请求丢失流量灰度id# 线程名=" + Thread.currentThread().getName() + ", 代码链="
                        + printStackTraceMgr(5));
            }
        }
    }

    private String printStackTraceMgr(int lineNum) {
        try {
            StackTraceElement[] stes = Thread.currentThread().getStackTrace();
            StackTraceElement ste;
            StringBuilder sb = new StringBuilder();
            for (int i = 0, j = 0; i < stes.length && j < lineNum; i++) {
                ste = stes[i];
                if (ste.getLineNumber() <= 0) {
                    continue;
                }
                if (!ste.getClassName().startsWith("com.liepin")) {
                    continue;
                }
                if (ste.getClassName().startsWith("com.liepin.swift.")
                        || ste.getClassName().startsWith("com.liepin.cache.")
                        || ste.getClassName().startsWith("com.liepin.dao.")
                        || ste.getClassName().startsWith("com.liepin.router.")) {
                    continue;
                }
                sb.append("at ");
                sb.append(ste.getClassName());
                sb.append(".");
                sb.append(ste.getMethodName());
                sb.append("(");
                sb.append(ste.getFileName());
                sb.append(":");
                sb.append(ste.getLineNumber());
                sb.append(")");
                sb.append("\r\n");
                j++;
            }
            return sb.toString();
        } catch (Exception e) {
            return "定位异常";
        }
    }

}
