package com.liepin.swift.framework.monitor;

import com.liepin.common.des.DesPlus;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.framework.conf.SwiftConfig;

public class SignUtil {

    private static final long PERIOD_VALIDITY = 60 * 60 * 1000;// 60分钟有效期

    /**
     * 生成服务上线签名
     * 
     * @return
     */
    public static String onlineSign() {
        return DesPlus.getInstance().encrypt("true|" + System.currentTimeMillis(), SwiftConfig.HEARTBEAT_SECRET_KEY);
    }

    /**
     * 生成服务下线签名
     * 
     * @return
     */
    public static String offlineSign() {
        return DesPlus.getInstance().encrypt("false|" + System.currentTimeMillis(), SwiftConfig.HEARTBEAT_SECRET_KEY);
    }

    /**
     * 验证签名，并返回控制启动状态，鉴权失败抛出异常
     * 
     * 服务心跳接口
     * <p>
     * 还可以通过加密参数控制服务上线下线<br>
     * 签名算法：boolean|时间戳，根据密钥进行DES加密，生成value值<br>
     * 作用时间：本次请求签名的有效时间为该时间戳+5分钟，用于防止 replay 型攻击<br>
     * <p>
     * 上线返回：true<br>
     * 下线返回：false<br>
     * 
     * @param value
     * @return
     * @throws BizException
     */
    public static boolean verify(String value) throws BizException {
        // 控制逻辑校验
        String decrypt = null;
        try {
            decrypt = DesPlus.getInstance().decrypt(value, SwiftConfig.HEARTBEAT_SECRET_KEY);
        } catch (Exception e) {
        }
        if (null == decrypt || value == decrypt) {
            // 鉴权失败，返回当前状态
            alert();
        }
        String[] array = decrypt.split("\\|");
        if (array.length != 2) {
            alert();
        }
        String flag = array[0];
        if (!"true".equals(flag) && !"false".equals(flag)) {
            alert();
        }
        long requestTime = -1l;
        try {
            requestTime = Long.parseLong(array[1]);
        } catch (NumberFormatException e) {
            alert();
        }
        // 验证请求实效性
        if ((System.currentTimeMillis() - requestTime) > PERIOD_VALIDITY) {
            alert();
        }
        return Boolean.parseBoolean(flag);
    }

    private static void alert() throws BizException {
        throw new BizException("service flow illegal control by /monitor/http");
    }

}
