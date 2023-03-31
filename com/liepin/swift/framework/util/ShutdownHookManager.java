package com.liepin.swift.framework.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ShutdownHookManager {

    /** Reference to the JVM shutdown hook, if registered */
    private static final ConcurrentMap<Class<?>, Thread> hooks = new ConcurrentHashMap<>();

    /**
     * 
     * @param point
     */
    public static synchronized void register(ShutdownHookPoint point) {
        Thread absentHook = hooks.get(point.binding());
        if (absentHook == null) {
            // No shutdown hook registered yet.
            absentHook = new Thread() {

                @Override
                public void run() {
                    point.submit();
                }

            };
            Runtime.getRuntime().addShutdownHook(absentHook);
            hooks.put(point.binding(), absentHook);
        }
    }

    /**
     * removes a JVM shutdown hook, if registered, as it's not needed anymore.
     * 
     * @param point
     */
    public static void remove(ShutdownHookPoint point, boolean confirm) {
        // If we registered a JVM shutdown hook, we don't need it anymore
        // now:
        // We've already explicitly closed the context.
        Thread current = hooks.remove(point.binding());
        if (current != null) {
            Runtime.getRuntime().removeShutdownHook(current);
        }
        if (confirm) {
            point.submit();
        }
    }

    public static interface ShutdownHookPoint {

        public Class<?> binding();

        public void submit();

    }

}
