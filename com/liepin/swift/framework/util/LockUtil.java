package com.liepin.swift.framework.util;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.liepin.zookeeper.client.lock.DistributedReentrantLock;
import com.liepin.zookeeper.client.unusual.IZookeeperClient;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public class LockUtil {

    private static final Logger logger = Logger.getLogger(LockUtil.class);

    private static final IZookeeperClient ZKCLIENT = ZookeeperFactory.useDefaultZookeeperWithoutException();

    public static void doAndRetry(String projectName, Function function) {
        int num = 3;
        boolean done = false;
        DistributedReentrantLock lock = ZKCLIENT.createDistributedReentrantLock("swiftIsInterface", projectName);
        try {
            while (num-- > 0) {
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    function.run();
                    done = true;
                    break;
                }
            }
        } catch (Throwable e) {
            logger.warn("[LockUtil] swiftIsInterface/" + projectName + " try lock fail", e);
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
            }
        }
        if (!done) {
            logger.warn("[LockUtil] /config/public/rpc/server/isInterface/" + projectName
                    + " update failed because of concurrency!");
        }
    }

    public static interface Function {
        public void run();
    }

}
