package com.liepin.swift.framework.mvc.eventInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.liepin.common.conf.PropUtil;
import com.liepin.common.datastructure.ThreadLocalDateFormat;
import com.liepin.common.datastructure.ThreadLocalMessageFormat;
import com.liepin.swift.framework.util.StaggerTime;
import com.liepin.zookeeper.client.IZookeeperClient;
import com.liepin.zookeeper.client.enums.EnumChangedEvent;
import com.liepin.zookeeper.client.enums.EnumNamespace;
import com.liepin.zookeeper.client.listener.NewNodeListener;
import com.liepin.zookeeper.client.util.ZookeeperFactory;

public abstract class AbstractIOLogger {

    private final static Logger logger = Logger.getLogger(AbstractIOLogger.class);

    private static final String pattern = "timeOn={0}, status={1}, eclipse={2}ms, servletPath={3}, clientIP={4}, input={5}, output={6}";

    private static ThreadLocalDateFormat timeDateFormat = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS");

    private static ThreadLocalMessageFormat messageFormat = new ThreadLocalMessageFormat(pattern);
    /**
     * 显示字符数
     */
    protected static final int MAXCHARS_INPUT = 768;
    protected static final int MAXCHARS_OUTPUT = 512;

    /**
     * 需要全量打印事件日志的接口列表
     */
    private static final Set<String> PRINT_FULL_URLS = Collections.synchronizedSet(new HashSet<String>());

    /**
     * zookeeper节点名称
     */
    private static final String NODE_NAME = "common/log";
    private static final AtomicReference<Set<String>> SENSITIVE_URLS = new AtomicReference<Set<String>>();

    private static FullJournal fullJournal = new FullJournal();

    static {
        logger.info("The default log template is " + pattern);
        Iterator<String> iterator = PropUtil.getInstance().getKeys("eventinfo.fullprint.uri");
        while (iterator.hasNext()) {
            String value = PropUtil.getInstance().get(iterator.next());
            PRINT_FULL_URLS.add(value);
        }
        logger.info("The default print full log is " + PRINT_FULL_URLS);

        // 加载敏感接口名
        load();
        createListener();
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        Map<String, Object> data = ZookeeperFactory.useDefaultZookeeperWithoutException().getMap(EnumNamespace.PUBLIC,
                NODE_NAME);
        if (data != null && !data.isEmpty()) {
            List<String> list = (List<String>) data.get("sensitive_url");
            if (list != null) {
                SENSITIVE_URLS.set(new HashSet<String>(list));
            }
        }
        if (Objects.isNull(SENSITIVE_URLS.get())) {
            SENSITIVE_URLS.set(new HashSet<>());
        }
    }

    /**
     * 监听节点变化
     */
    private static void createListener() {
        ZookeeperFactory.useDefaultZookeeperWithoutException().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumNamespace.PUBLIC.getNamespace() + "/" + NODE_NAME;
            }

            @Override
            public void nodeChanged(IZookeeperClient zookeeperClient, EnumChangedEvent type) {
                // 错开时间
                StaggerTime.waited();
                load();
            }

        });
    }

    /**
     * 配置接口是否全量打印事件日志
     * 
     * @param actionPath
     * @param open
     */
    public static final void configFullLog(String actionPath, boolean open) {
        if (open) {
            PRINT_FULL_URLS.add(actionPath);
        } else {
            PRINT_FULL_URLS.remove(actionPath);
        }
    }

    /**
     * 事件日志打印
     * 
     * @param eventInfo 事件信息
     */
    public final void log(Event eventInfo) {
        if (eventInfo instanceof NullEvent) {
            return;
        }
        // 排除框架内部接口
        if (shield(eventInfo.getActionPath())) {
            return;
        }

        // 敏感接口data数据去掉打印
        if (SENSITIVE_URLS.get().contains(eventInfo.getActionPath())) {
            eventInfo.setInput(EscapeText.ignoreChars4Data(eventInfo.getInput()));
            eventInfo.setOutput(EscapeText.ignoreChars4Data(eventInfo.getOutput()));
        }

        // 定期全量采集
        fullJournal.log(eventInfo);

        if (PRINT_FULL_URLS.contains(eventInfo.getActionPath())) {
            eventInfo.setInput(EscapeText.confuseChars(eventInfo.getInput()));
            eventInfo.setOutput(EscapeText.confuseChars(eventInfo.getOutput()));
        } else {
            eventInfo.setInput(EscapeText.confuseAndIgnoreChars(eventInfo.getInput(), MAXCHARS_INPUT));
            eventInfo.setOutput(EscapeText.confuseAndIgnoreChars(eventInfo.getOutput(), MAXCHARS_OUTPUT));
        }

        String info = messageFormat.get().format(getVars(eventInfo));
        logger.info(info);
    }

    protected Object[] getVars(Event eventInfo) {
        return new Object[] { timeDateFormat.get().format(eventInfo.getStart()), eventInfo.getStatus(),
                eventInfo.getEclipse(), eventInfo.getActionPath(), eventInfo.getClientIP(), eventInfo.getInput(),
                eventInfo.getOutput() };
    }

    /**
     * 日志模板
     */
    protected String getTemplate() {
        return pattern;
    }

    /**
     * 打印日志过滤
     * 
     * @param output
     * @return
     */
    protected String filter(String output) {
        return output;
    }

    /**
     * 是否屏蔽url请求日志
     * <p>
     * 默认不屏蔽
     * 
     * @param url
     * @return
     */
    protected boolean shield(String url) {
        return false;
    }

}
