package com.liepin.swift.framework.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.liepin.common.conf.SystemUtil;
import com.liepin.swift.framework.monitor.call.RepeatCallCollecter;
import com.liepin.swift.framework.monitor.jvm.JvmCollecter;
import com.liepin.swift.framework.monitor.proc.ProcCollecter;
import com.liepin.swift.framework.monitor.tomcat.SocketCollecter;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public final class MonitorRegister {

    private static final List<ILogPlugin> REGISTERS = new ArrayList<ILogPlugin>();

    static {
        REGISTERS.add(JvmCollecter.getInstance());
        REGISTERS.add(RepeatCallCollecter.getInstance());
        REGISTERS.add(SocketCollecter.getInstance());
        //REGISTERS.add(CrossCollecter.getInstance());
        REGISTERS.add(ProcCollecter.getInstance());
    }

    public static void initialize() {
        if (enable()) {
            for (ILogPlugin plugin : REGISTERS) {
                plugin.install();
            }
        }
    }

    public static void destroy() {
        if (enable()) {
            for (ILogPlugin plugin : REGISTERS) {
                plugin.uninstall();
            }
            REGISTERS.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean enable() {
        String value = System.getProperty("state");
        if ("alone".equals(value)) {
            return false;
        }
        try {
            Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException()
                    .getMap(EnumNamespace.PUBLIC, "/common/monitor");
            if (data != null) {
                List<String> areas = (List<String>) data.get("monitorClosedAreas");
                if (areas != null && areas.size() > 0) {
                    if (areas.contains(SystemUtil.getLogicAreaStr())) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

}
