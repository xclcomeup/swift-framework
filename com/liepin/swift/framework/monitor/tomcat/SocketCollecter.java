package com.liepin.swift.framework.monitor.tomcat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liepin.common.conf.ProjectId;
import com.liepin.common.conf.SystemUtil;
import com.liepin.common.json.JsonUtil;
import com.liepin.common.other.DateUtil;
import com.liepin.router.discovery.ServicePortUtil;
import com.liepin.swift.framework.monitor.AbstractLogPlugin;

public class SocketCollecter extends AbstractLogPlugin {

    // sh -c 'ss -ant sport = :57101' 服务端
    // sh -c 'ss -ant dport = :57101' 客户端
    private static final String LOCAL_COMMAND_LINUX = "ss -ant sport = :{0}";
    // private static final String REMOTE_COMMAND_LINUX =
    // "ss -ant dport = :{0}";

    // 如： netstat -ant | grep ip:port
    private static final String LOCAL_COMMAND_LINUX1 = "netstat -ant | grep {0}:{1}";

    private int port;

    private SocketCollecter() {
        super();
        try {
            this.port = ServicePortUtil.getServerPort();
        } catch (Exception e) {
            setEnable(false);
        }
    }

    private static SocketCollecter instance = new SocketCollecter();

    public static SocketCollecter getInstance() {
        return instance;
    }

    @Override
    public String category() {
        return "socket";
    }

    @Override
    public String zkListenPath() {
        return "/common/monitor/socket";
    }

    @Override
    public boolean timer() {
        return true;
    }

    @Override
    protected boolean getEnable(Map<String, Object> map) {
        return SystemUtil.isLinux() && super.getEnable(map);
    }

    @Override
    public void onEvent() {
        List<SocketBean> beans = new ArrayList<SocketBean>();
        int acceptCount = (com.liepin.common.conf.SystemUtil.isOnDocker()) ? netstatInvoke(beans) : ssInvoke(beans);
        if (beans.size() > 0) {
            Map<String, Integer> socketClassification = new HashMap<String, Integer>();
            int recvQHeaped = 0;
            for (SocketBean bean : beans) {
                Integer num = socketClassification.get(bean.getState());
                if (num == null) {
                    socketClassification.put(bean.getState(), 1);
                } else {
                    socketClassification.put(bean.getState(), num + 1);
                }
                if (bean.getRecvQ() > 0) {
                    recvQHeaped++;
                }
            }

            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("ClientId", ProjectId.getClientId());
            map.put("Pod", com.liepin.common.conf.SystemUtil.getPod());
            map.put("Time", DateUtil.getCurrentDateTime());
            map.put("SocketClassification", socketClassification);
            map.put("AcceptCount", acceptCount);
            map.put("RecvQHeaped", recvQHeaped);
            String json = JsonUtil.toJson(map);
            log(json);
        }
    }

    private int ssInvoke(List<SocketBean> beans) {
        String command = MessageFormat.format(LOCAL_COMMAND_LINUX, new Object[] { "" + port });
        Process process = null;
        LineNumberReader input = null;
        int acceptCount = 0;
        try {
            String[] commands = new String[] { "sh", "-c", command };
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            process = pb.start();
            input = new LineNumberReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("State")) {
                    continue;
                }
                if (line.startsWith("LISTEN")) {
                    SocketBean listen = parse(line);
                    acceptCount = listen.getSendQ();
                    continue;
                }
                SocketBean bean = parse(line);
                beans.add(bean);
            }
        } catch (Throwable e) {
            // ignore
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
            }
            if (process != null) {
                process.destroy();
            }
        }
        return acceptCount;
    }

    private int netstatInvoke(List<SocketBean> beans) {
        String command = MessageFormat.format(LOCAL_COMMAND_LINUX1,
                new Object[] { com.liepin.common.conf.SystemUtil.getInNetworkIp(), "" + port });
        Process process = null;
        LineNumberReader input = null;
        try {
            String[] commands = new String[] { "sh", "-c", command };
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            process = pb.start();
            input = new LineNumberReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                SocketBean bean = parse1(line);
                if (bean != null) {
                    beans.add(bean);
                }
            }
        } catch (Throwable e) {
            // ignore
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
            }
            if (process != null) {
                process.destroy();
            }
        }
        return 100;// 获取不到，暂时写死配置
    }

    private SocketBean parse(String line) {
        // ESTAB 0 0 ::ffff:10.10.10.16:57101 ::ffff:10.10.102.100:18668
        String[] ret = new String[5];
        char[] charArray = line.trim().toCharArray();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : charArray) {
            if (' ' == c) {
                if (sb.length() > 0) {
                    ret[i++] = sb.toString();
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        ret[i++] = sb.toString();
        return new SocketBean(ret);
    }

    private SocketBean parse1(String line) {
        // tcp 0 0 ::ffff:10.201.37.66:8080 ::ffff:10.10.102.106:44002 TIME_WAIT
        String[] tmp = new String[6];
        char[] charArray = line.trim().toCharArray();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : charArray) {
            if (' ' == c) {
                if (sb.length() > 0) {
                    tmp[i++] = sb.toString();
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        tmp[i++] = sb.toString();
        if (!tmp[3].endsWith(com.liepin.common.conf.SystemUtil.getInNetworkIp() + ":" + port)) {
            return null;
        }
        String[] ret = new String[5];
        ret[0] = tmp[5];
        ret[1] = tmp[1];
        ret[2] = tmp[2];
        ret[3] = tmp[3];
        ret[4] = tmp[4];
        return new SocketBean(ret);
    }

}
