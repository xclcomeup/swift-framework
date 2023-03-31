package com.liepin.swift.framework.util;

import javax.servlet.http.HttpServletRequest;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.liepin.swift.core.enums.SystemEnum;
import com.liepin.swift.core.exception.BizException;
import com.liepin.swift.core.exception.SysException;
import com.liepin.swift.framework.bundle.choice.EasySwitcher;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.monitor.cat.CatConsumer;
import com.liepin.swift.framework.mvc.util.AttributeUtil;

public class CatHelper {

    // 安全扫描产生的异常CAT不抛错
    private static EasySwitcher easySwitcher = new EasySwitcher("/common/protect/scanIps", "enable", "ips");

    private static boolean noStandardProtocolError = SwiftConfig.noStandardProtocolError();

    public static void logError(HttpServletRequest request, Transaction t, Throwable throwable) {
        if (t == null) {
            Cat.logError(throwable);
            return;
        }

        if (throwable instanceof BizException) {
            t.setStatus(Message.SUCCESS);
        } else {
            // 针对某些ip不让cat显示错误
            if (easySwitcher.getEnable(IPUtil.getIpAddr(request))) {
                // 埋点
                AttributeUtil.setErrorIgnore(request);
                t.setStatus(Message.SUCCESS);
            } else {
                if (ignoreCatError(throwable)) {
                    t.setStatus(Message.SUCCESS);
                } else {
                    t.setStatus(throwable);
                    Cat.logError(throwable);
                }
            }
        }
    }

    /**
     * 通用事件cat埋点
     * 
     * @param type
     * @param name
     * @param catConsumer
     * @throws Exception
     */
    public static void point(String type, String name, CatConsumer catConsumer) throws Exception {
        Transaction t = Cat.newTransaction(type, name);
        try {
            catConsumer.accept();
            t.setStatus(Message.SUCCESS);
        } catch (Exception e) {
            if (ignoreCatError(e)) {
                t.setStatus(Message.SUCCESS);
            } else {
                t.setStatus(e);
                Cat.logError(e);
            }
            throw e;
        } finally {
            t.complete();
        }
    }

    private static boolean ignoreCatError(Throwable throwable) {
        if (throwable instanceof SysException) {
            SysException sysException = (SysException) throwable;
            return !noStandardProtocolError && SystemEnum.INVALID.code().equals(sysException.code());
        }
        return false;
    }

}
