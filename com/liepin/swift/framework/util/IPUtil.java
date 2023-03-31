package com.liepin.swift.framework.util;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 包装从Servlet里获取地区信息
 * 
 * @author yuanxl
 * 
 */
public class IPUtil {

    // /**
    // * 获取请求ip，直接从remoteAddr获取
    // *
    // * @param request
    // * @return
    // */
    // public static String getIpAddr(final HttpServletRequest request) {
    // return request.getRemoteAddr();
    // }

    /**
     * 获取请求ip 逻辑：<br>
     * 1. 先从header<X-Real-IP> 里获取用户连接的真实ip<br>
     * 2. 如果为空，再从header<X-Forwarded-For> 里获取逗号隔开的第1位ip，判断是否私有ip，如果不是就返回<br>
     * 3. 否则就返回RemoteAddr（其实是nginx ip，兜底的，其实不对的）<br>
     * <p>
     * 如果不对打debug日志<br>
     * 
     * @param request
     * @return
     */
    public static String getIpAddr(final HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.trim().length() != 0) {
            return ip.trim();
        }

        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.indexOf(',') > 0) {
            String[] tmp = ip.split("[,]");
            for (int i = 0; tmp != null && i < tmp.length; i++) {
                if (tmp[i] != null && tmp[i].length() > 0 && !"unknown".equalsIgnoreCase(tmp[i])) {
                    ip = tmp[i].trim();
                    break;
                }
            }
        }
        if (!isUnkown(ip) && !isPrivateIp(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 获取请求ip 逻辑：<br>
     * 1. 先从header<X-Real-IP> 里获取用户连接的真实ip<br>
     * 2. 如果为空，再从header<X-Forwarded-For> 里获取逗号隔开的第1位ip，判断是否私有ip，如果不是就返回<br>
     * 3. 否则就返回null<br>
     * 
     * @param headers
     * @return
     */
    public static String getIpAddr(final Map<String, String> headers) {
        String ip = headers.get("X-Real-IP");
        if (ip != null && ip.trim().length() != 0) {
            return ip.trim();
        }

        ip = headers.get("X-Forwarded-For");
        if (ip != null && ip.indexOf(',') > 0) {
            String[] tmp = ip.split("[,]");
            for (int i = 0; tmp != null && i < tmp.length; i++) {
                if (tmp[i] != null && tmp[i].length() > 0 && !"unknown".equalsIgnoreCase(tmp[i])) {
                    ip = tmp[i].trim();
                    break;
                }
            }
        }
        if (!isUnkown(ip) && !isPrivateIp(ip)) {
            return ip;
        }

        return null;
    }

    private static boolean isUnkown(String ip) {
        if (ip == null || ip.trim().length() == 0 || "unknown".equalsIgnoreCase(ip.trim())) {
            return true;
        }
        return false;
    }

    private static final int[][] PRIVATE_IPA = new int[][] { { 10, 0, 0, 0 }, { 10, 255, 255, 255 } };
    private static final int[][] PRIVATE_IPB = new int[][] { { 172, 16, 0, 0 }, { 172, 31, 255, 255 } };
    private static final int[][] PRIVATE_IPC = new int[][] { { 192, 168, 0, 0 }, { 192, 168, 255, 255 } };

    /**
     * 私有IP地址范围：<br>
     * A: 10.0.0.0~10.255.255.255 即10.0.0.0/8<br>
     * B: 172.16.0.0~172.31.255.255即172.16.0.0/12<br>
     * C: 192.168.0.0~192.168.255.255 即192.168.0.0/16<br>
     * 
     * @param ip
     * @return
     */
    public static boolean isPrivateIp(String ip) {
        int pos = ip.indexOf(":");
        if (pos != -1) {
            ip = ip.substring(0, pos);
        }
        String[] array = ip.split("\\.");
        // ipv6返回false
        if (array.length != 4) {
            return false;
        }
        int[] actual = new int[4];
        for (int i = 0; i < array.length; i++) {
            actual[i] = Integer.parseInt(array[i]);
        }
        return contain(PRIVATE_IPA, actual) || contain(PRIVATE_IPB, actual) || contain(PRIVATE_IPC, actual);
    }

    /**
     * ip格式字符串转长整型
     * 
     * @param strIp
     * @return
     */
    public static long ipToLong(String strIp) {
        long[] ip = new long[4];
        // 先找到IP地址字符串中.的位置
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);
        // 将每个.之间的字符串转换成整型
        ip[0] = Long.parseLong(strIp.substring(0, position1));
        ip[1] = Long.parseLong(strIp.substring(position1 + 1, position2));
        ip[2] = Long.parseLong(strIp.substring(position2 + 1, position3));
        ip[3] = Long.parseLong(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    /**
     * 长整型转ip格式字符串
     * 
     * @param longIp
     * @return
     */
    public static String longToIP(long longIp) {
        StringBuilder sb = new StringBuilder();
        // 直接右移24位
        sb.append(String.valueOf((longIp >>> 24)));
        sb.append(".");
        // 将高8位置0，然后右移16位
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        // 将高16位置0，然后右移8位
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        // 将高24位置0
        sb.append(String.valueOf((longIp & 0x000000FF)));
        return sb.toString();
    }

    private static boolean contain(int[][] expect, int[] actual) {
        int[] start = expect[0];
        int[] end = expect[1];
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] < start[i] || actual[i] > end[i]) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static StringBuilder logHeader(final HttpServletRequest request) {
        StringBuilder log = new StringBuilder("Request Header: ");
        Enumeration<String> headerNames = request.getHeaderNames();
        int i = 0;
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (i++ != 0) {
                log.append(", ");
            }
            log.append(name).append("=").append(request.getHeader(name));
        }
        return log;
    }

}
