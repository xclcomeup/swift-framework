package com.liepin.swift.framework.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class AsynProcess {

    private static final Logger logger = Logger.getLogger(AsynProcess.class);

    public static interface AsynHandle {
        public void process();
    }

    private final CountDownLatch latch;
    private final Map<String, FutureTask<Void>> futures;

    public AsynProcess(int num) {
        this.latch = new CountDownLatch(num);
        this.futures = new HashMap<>(num);
    }

    public void execute(String taskName, final AsynHandle asynHandle) {
        FutureTask<Void> task = new FutureTask<>(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                try {
                    asynHandle.process();
                } finally {
                    latch.countDown();
                }
                return null;
            }

        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

        futures.put(taskName, task);
    }

    public void awaitAndfinish() {
        awaitAndfinish(5);
    }

    public void awaitAndfinish(int second) {
        try {
            latch.await(second, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        for (Map.Entry<String, FutureTask<Void>> entry : futures.entrySet()) {
            logger.info(entry.getKey() + ": " + print(entry.getValue()));
        }
    }

    private String print(FutureTask<Void> task) {
        return (task.isDone()) ? "在预期时间内释放完" : "预期时间内没释放完";
    }

}
