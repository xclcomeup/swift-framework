package com.liepin.swift.framework.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class Poller {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Start polling.
     */
    public synchronized void start() {
        running.compareAndSet(false, true);
    }

    /**
     * Stops polling and shuts down the ExecutorService.
     * <p>
     * This instance can no longer be used after calling shutdown.
     */
    public synchronized void shutdown() {
        pause();
    }

    /**
     * Pause (stop) polling. Polling can be started again with
     * <code>start</code> as long as <code>shutdown</code> is not called.
     */
    public synchronized void pause() {
        // use compareAndSet to make sure it stops only once and when
        // running
        running.compareAndSet(true, false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public String message() {
        return "ping";
    }

}
