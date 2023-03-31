//package com.liepin.swift.framework.plugin.listener;
//
//import org.springframework.stereotype.Service;
//
//import com.liepin.common.conf.ProjectId;
//import com.liepin.common.conf.PropUtil;
//import com.liepin.common.net.IPUtil;
//
//@Service
//public class IPPreloadListener implements IAfterListener {
//
//    @Override
//    public void onApplicationEvent() {
//        String projectName = ProjectId.getProjectName();
//        if (projectName.endsWith("-proxy") || projectName.endsWith("-web")) {
//            if (PropUtil.getInstance().getBoolean("ip.preload", true)) {
//                loadIp();
//            }
//        } else {
//            if (PropUtil.getInstance().getBoolean("ip.preload", false)) {
//                loadIp();
//            }
//        }
//    }
//
//    private void loadIp() {
//        // no meaning
//        IPUtil.getAllLocalIPs();
//    }
//
//}
