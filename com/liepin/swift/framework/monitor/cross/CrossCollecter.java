package com.liepin.swift.framework.monitor.cross;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.ProjectIdMap;
import com.liepin.common.datastructure.ThreadLocalDateFormat;
import com.liepin.swift.framework.monitor.AbstractLogPlugin;

/**
 * 调用日志
 * <p>
 * 暂时停用
 * 
 * @author yuanxl
 *
 */
@Deprecated
public class CrossCollecter extends AbstractLogPlugin {

    private static final int BUFFERED_SIZE = 10000;
    private volatile int bufferedSize = BUFFERED_SIZE;

    private ArrayBlockingQueue<String> buffered = new ArrayBlockingQueue<String>(bufferedSize);
    private final AtomicBoolean fuse = new AtomicBoolean(false);

    private static final ThreadLocalDateFormat DATAFORMAT = new ThreadLocalDateFormat("yyyyMMddHH");

    public CrossCollecter() {
        super();
    }

    private static CrossCollecter instance = new CrossCollecter();

    public static CrossCollecter getInstance() {
        return instance;
    }

    @Override
    public String category() {
        return "cross";
    }

    @Override
    public String zkListenPath() {
        return "/common/monitor/cross";
    }

    @Override
    protected void handle(Map<String, Object> map) {
        super.handle(map);
        if (isEnable()) {
            this.bufferedSize = getBufferedSize(map, BUFFERED_SIZE);
        }
    }

    @Override
    public boolean timer() {
        return true;
    }

    @Override
    public void onEvent() {
        fuse.compareAndSet(false, true);
        List<String> tmp = new ArrayList<String>(buffered);
        buffered.clear();
        fuse.compareAndSet(true, false);

        for (String str : tmp) {
            log(str);
        }
        tmp.clear();
    }

    /**
     * 被调用方 输出格式：clientida:clientIdb:yyyymmddhh
     * 
     * @param projectName
     * @param time
     */
    public void call(String projectName, Date time) {
        if (!isEnable()) {
            return;
        }

        try {
            StringBuilder log = new StringBuilder();
            log.append(ProjectId.getClientId());
            log.append(":");
            log.append(ProjectIdMap.clientId(projectName));
            log.append(":");
            log.append(DATAFORMAT.get().format(time));

            String message = log.toString();
            if (fuse.get() || !buffered.offer(message)) {
                log(message);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private int getBufferedSize(Map<String, Object> map, int defaultValue) {
        Integer bufferedSize = (Integer) map.get("bufferedSize");
        return (bufferedSize != null) ? bufferedSize.intValue() : defaultValue;
    }

}
