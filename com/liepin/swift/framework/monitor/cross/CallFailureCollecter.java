package com.liepin.swift.framework.monitor.cross;

import org.apache.log4j.Logger;

import com.liepin.common.conf.ProjectId;
import com.liepin.swift.core.util.Log4jUtil;
import com.liepin.swift.core.util.ThreadLocalUtil;
import com.liepin.swift.core.util.TraceId;

/**
 * 调用失败日志
 * 
 * @author yuanxl
 *
 */
public class CallFailureCollecter {

    private static final String pattern = "请求链clientIds#自身clientId#被调用clientId#被调用服务ip#错误码status#请求时间河TimeRiver（发起请求^开始调用^接收请求）#追踪码traceId";

    private final Logger logger;

    private CallFailureCollecter() {
        this.logger = Log4jUtil.register2("failurecall", "failure");
    }

    private static CallFailureCollecter instance = new CallFailureCollecter();

    public static CallFailureCollecter getInstance() {
        return instance;
    }

    public void init() {
        logger.info("The default log template is " + pattern);
    }

    /**
     * 输出格式：请求链clientIds#自身clientId#被调用clientId#被调用服务ip#错误码status#请求时间河TimeRiver
     * （发起请求、开始调用、 接收请求）#追踪码traceId
     * 
     * @param projectName
     * @param time
     */
    public void record(String targetClientId, String ip, String status, String timeRiver) {
        try {
            StringBuilder log = new StringBuilder();
            log.append(toString(ThreadLocalUtil.getInstance().getClientId()));
            log.append("#");
            log.append(ProjectId.getClientId());
            log.append("#");
            log.append(targetClientId);
            log.append("#");
            log.append(ip);
            log.append("#");
            log.append(status);
            log.append("#");
            log.append(timeRiver);
            log.append("#");
            TraceId traceId = ThreadLocalUtil.getInstance().getTraceId();
            if (traceId != null) {
                log.append(traceId.toString());
            }
            logger.info(log.toString());
        } catch (Exception e) {
            // ignore
        }
    }

    private String toString(String[] array) {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            log.append(array[i]);
            if (i != array.length - 1) {
                log.append(",");
            }
        }
        return log.toString();
    }

}
