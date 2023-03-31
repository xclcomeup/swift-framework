package com.liepin.swift.framework.limit;

import java.util.concurrent.atomic.AtomicBoolean;

import com.liepin.swift.framework.limit.config.LimitControllConfiger;
import com.liepin.swift.framework.limit.rules.processor.fallback2s.FallbackProvider;

/**
 * 限流初始化
 * 
 * @author yuanxl
 *
 */
public final class SwiftLimitInitializer {

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    public static void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        LimitControllConfiger.get();
        FallbackProvider.get();
    }

}
