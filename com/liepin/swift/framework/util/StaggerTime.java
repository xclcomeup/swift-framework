package com.liepin.swift.framework.util;

import java.util.concurrent.TimeUnit;

import com.liepin.common.datastructure.ThreadLocalRandom;

/**
 * 随机排队错开时间
 * <p>
 * 起因：框架的配置要提供给所有系统都会使用，一旦这样的配置发生变更，Zookeeper会广播给所有的watcher，然后所有Client都来拉取，
 * 瞬间造成非常大的网络流量，引起所谓的『惊群』。
 * 
 * @author yuanxl
 * @date 2016-11-17 下午04:04:56
 */
public class StaggerTime {

    private static final int RANGE = 10;

    /**
     * 随机等待<br>
     * 时间范围:0-10秒
     */
    public static final void waited() {
        int time = ThreadLocalRandom.current().nextInt(RANGE);
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
