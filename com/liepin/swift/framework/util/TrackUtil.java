package com.liepin.swift.framework.util;

import com.liepin.common.datastructure.ThreadLocalRandom;
import com.liepin.swift.core.util.ThreadLocalUtil;

public class TrackUtil {

    private static final String FIRST = ".1";

    /**
     * 创建跟踪id并设置threadlocal
     * 
     * @return
     */
    public static String createTrackId() {
        String trackId = ThreadLocalRandom.current().nextInt() + FIRST;
        ThreadLocalUtil.getInstance().setTraceId(trackId);
        return trackId;
    }

    /**
     * 远程调用跟踪id并设置threadlocal
     * 
     * @return
     */
    public static String nextTrackId() {
        String trackId = ThreadLocalUtil.getInstance().getTraceId();
        String nextId = "";
        if (trackId == null || "".equals(trackId)) {
            nextId = createTrackId();
        } else {
            int pos = trackId.lastIndexOf(".");
            nextId = trackId + FIRST;
            if (pos != -1) {
                trackId = trackId.substring(0, pos) + "." + (Integer.parseInt(trackId.substring(pos + 1)) + 1);
            }
            ThreadLocalUtil.getInstance().setTraceId(trackId);
        }
        return nextId;
    }

    /**
     * 设置跟踪id并设置threadlocal
     * 
     * @param trackId
     * @return
     */
    public static String track(String trackId) {
        if (trackId == null) {
            return createTrackId();
        } else {
            ThreadLocalUtil.getInstance().setTraceId(trackId);
            return trackId;
        }
    }

}
