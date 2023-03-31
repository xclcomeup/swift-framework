package com.liepin.swift.framework.controller;

import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.liepin.swift.core.consts.Const;
import com.liepin.swift.core.log.MonitorLogger;
import com.liepin.swift.framework.mvc.DownloadContentType;
import com.liepin.swift.framework.mvc.filter.ServletHolder;
import com.liepin.swift.framework.mvc.upload.UploadFileDto;
import com.liepin.swift.framework.mvc.util.RequestUtil;
import com.liepin.swift.framework.util.CommonUtil;

/**
 * 基础controller
 * 
 * @author yuanxl
 * 
 */
public abstract class AbstractController {

    protected final Logger catalinaLog = Logger.getLogger(getClass());

    protected final MonitorLogger monitorLog = MonitorLogger.getInstance();

    private HttpServletRequest getRequest() {
        return ServletHolder.getHttpServletRequest();
    }

    private HttpServletResponse getResponse() {
        return ServletHolder.getHttpServletResponse();
    }

    /**
     * 获取APP请求协议数据
     * 
     * @param key
     * @return
     */
    protected final Object getAgreeData(String key) {
        Map<String, Object> input = RequestUtil.getInput(getRequest());
        return (input != null) ? input.get(key) : null;
    }

    /**
     * 获取主体data={json}串
     * 
     * @return
     */
    protected final String getDataJson() {
        Map<String, Object> input = RequestUtil.getInput(getRequest());
        return (String) input.get(Const.DATA);
    }

    /**
     * 获取http header信息
     * 
     * @param name
     * @return
     */
    protected final String getHeader(String name) {
        return getRequest().getHeader(name);
    }

    /**
     * 获取http header信息
     * 
     * @param name
     * @return
     */
    protected final Enumeration<String> getHeaders(String name) {
        return (Enumeration<String>) getRequest().getHeaders(name);
    }

    /**
     * 获取所有http header names
     * 
     * @return
     */
    protected final Enumeration<String> getHeaderNames() {
        return (Enumeration<String>) getRequest().getHeaderNames();
    }

    /**
     * 设置返回http header信息
     * 
     * @param name
     * @param value
     */
    protected final void setHeader(String name, String value) {
        getResponse().setHeader(name, value);
    }

    protected final void addDateHeader(String name, long date) {
        getResponse().addDateHeader(name, date);
    }

    protected final void setDateHeader(String name, long date) {
        getResponse().setDateHeader(name, date);
    }

    protected final long getDateHeader(String name) {
        return getRequest().getDateHeader(name);
    }

    protected final void setContentType(String type) {
        getResponse().setContentType(type);
    }

    /**
     * 获取所有cookie
     * 
     * @return
     */
    protected final Cookie[] readCookies() {
        return getRequest().getCookies();
    }

    /**
     * 设置一个或多个cookie
     * 
     * @param cookies
     */
    protected final void writeCookies(Cookie... cookies) {
        HttpServletResponse response = getResponse();
        for (Cookie cookie : cookies) {
            response.addCookie(cookie);
        }
    }

    /**
     * 针对一个域名设置一个或多个cookie
     * 
     * @param domain 设置域名
     * @param cookies
     */
    protected final void writeCookies(String domain, Cookie... cookies) {
        for (Cookie cookie : cookies) {
            cookie.setDomain(domain);
        }
        writeCookies(cookies);
    }

    /**
     * 针对一个域名、生命周期设置一个或多个cookie
     * 
     * @param domain 设置域名
     * @param expiry 生命周期
     * @param cookies
     */
    protected final void writeCookies(String domain, int expiry, Cookie... cookies) {
        for (Cookie cookie : cookies) {
            cookie.setDomain(domain);
            cookie.setMaxAge(expiry);
        }
        writeCookies(cookies);
    }

    /**
     * 删除一个或多个cookie
     * 
     * @param cookies
     */
    protected final void deleteCookies(Cookie... cookies) {
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(0);
        }
        writeCookies(cookies);
    }

    /**
     * 设置关闭浏览器时清除一个或多个cookie
     * 
     * @param cookies
     */
    protected final void setSessionCookies(Cookie... cookies) {
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(-1);
        }
        writeCookies(cookies);
    }

    /**
     * 设置HTTPOnly属性的cookie
     * 
     * @param cookie
     */
    protected final void writeHttpOnlyCookie(Cookie cookie) {
        StringBuilder builder = new StringBuilder();
        builder.append(cookie.getName() + "=" + cookie.getValue() + ";");
        if (cookie.getMaxAge() >= 0) {
            builder.append("Max-Age=" + cookie.getMaxAge() + ";");
        }
        builder.append("Domain=" + cookie.getDomain() + ";");
        builder.append("Path=" + cookie.getPath() + ";");
        builder.append("Version=" + cookie.getVersion() + ";");
        if (cookie.getSecure()) {
            builder.append("Secure;");
        }
        builder.append("HTTPOnly;");
        getResponse().addHeader("Set-Cookie", builder.toString());
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    protected final List<UploadFileDto> getUploadFileDto() {
        Map<String, Object> input = RequestUtil.getInput(getRequest());
        return (List<UploadFileDto>) input.get(Const.UPLOAD_FILE);
    }

    /**
     * 下载文件
     * <p>
     * 文件名：随机10位字母加数字组合<br>
     * content-type: application/octet-stream;charset=utf-8<br>
     * 
     * @param bytes
     * @throws IOException
     */
    protected final void download(byte[] bytes) throws IOException {
        download(bytes, CommonUtil.randomLetterOrDigit(10), DownloadContentType.DEFAULT.getValue());
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名<br>
     * content-type: application/octet-stream;charset=utf-8<br>
     * 
     * @param bytes
     * @param filename
     * @throws IOException
     */
    protected final void download(byte[] bytes, String filename) throws IOException {
        download(bytes, filename, DownloadContentType.DEFAULT.getValue());
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @throws IOException
     */
    protected final void download(byte[] bytes, String filename, DownloadContentType contentType) throws IOException {
        download(bytes, filename, contentType.getValue());
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @throws IOException
     */
    protected final void download(byte[] bytes, String filename, String contentType) throws IOException {
        download(bytes, filename, contentType, false);
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @param acao
     * @throws IOException
     */
    protected final void download(byte[] bytes, String filename, String contentType, boolean acao) throws IOException {
        download(bytes, filename, contentType, acao, true);
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @param acao
     * @param close 是否关闭连接
     * @throws IOException
     */
    protected final void download(byte[] bytes, String filename, String contentType, boolean acao, boolean close)
            throws IOException {
        Optional.ofNullable(bytes).orElseThrow(() -> new RuntimeException("输出字节流为空"));
        HttpServletResponse response = getResponse();
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.toLowerCase().indexOf("msie") > 0
                || userAgent.toLowerCase().indexOf("rv:11.0") > 0 || userAgent.toLowerCase().indexOf("edge") > 0)) {// IE
            filename = URLEncoder.encode(filename, "UTF-8");
            filename = filename.replaceAll("\\+", "%20");// 处理空格变+问题
            // if ("https".equals(request.getScheme())) {// https
            // response.setHeader("Content-Transfer-Encoding", "binary");
            // response.setHeader("Cache-Control",
            // "must-revalidate, post-check=0, pre-check=0");
            // response.setHeader("Pragma", "public");
            // }
        } else {
            filename = new String(filename.getBytes("UTF-8"), "ISO_8859_1");
        }
        response.setContentType(contentType);
        response.setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Content-Length", bytes.length + "");
        if (acao) {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(response.getOutputStream());
            bos.write(bytes);
            if (!close) {
                bos.flush();
            }
        } finally {
            if (close && bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 下载文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @throws IOException
     */
    protected final void download(InputStream is, String filename, String contentType) throws IOException {
        download(is, -1, filename, contentType);
    }

    protected final void download(InputStream is, int length, String filename, String contentType) throws IOException {
        Optional.ofNullable(is).orElseThrow(() -> new RuntimeException("输出流为空"));
        HttpServletResponse response = getResponse();
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.toLowerCase().indexOf("msie") > 0
                || userAgent.toLowerCase().indexOf("rv:11.0") > 0 || userAgent.toLowerCase().indexOf("edge") > 0)) {// IE
            filename = URLEncoder.encode(filename, "UTF-8");
            filename = filename.replaceAll("\\+", "%20");// 处理空格变+问题
            // if ("https".equals(request.getScheme())) {// https
            // response.setHeader("Content-Transfer-Encoding", "binary");
            // response.setHeader("Cache-Control",
            // "must-revalidate, post-check=0, pre-check=0");
            // response.setHeader("Pragma", "public");
            // }
        } else {
            filename = new String(filename.getBytes("UTF-8"), "ISO_8859_1");
        }
        response.setContentType(contentType);
        response.setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
        if (length != -1) {
            response.setHeader("Content-Length", length + "");
        }

        ServletOutputStream outputStream = response.getOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n = 0;
        try {
            while (-1 != (n = is.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * 浏览器内嵌显示一个文件
     * <p>
     * 需要指定文件名和content-type<br>
     * 
     * @param bytes
     * @param filename
     * @param contentType
     * @throws IOException
     */
    protected final void display(byte[] bytes, String filename, String contentType) throws IOException {
        Optional.ofNullable(bytes).orElseThrow(() -> new RuntimeException("输出字节流为空"));
        HttpServletResponse response = getResponse();
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.toLowerCase().indexOf("msie") > 0
                || userAgent.toLowerCase().indexOf("rv:11.0") > 0 || userAgent.toLowerCase().indexOf("edge") > 0)) {// IE
            filename = URLEncoder.encode(filename, "UTF-8");
            filename = filename.replaceAll("\\+", "%20");// 处理空格变+问题
            // if ("https".equals(request.getScheme())) {// https
            // response.setHeader("Content-Transfer-Encoding", "binary");
            // response.setHeader("Cache-Control",
            // "must-revalidate, post-check=0, pre-check=0");
            // response.setHeader("Pragma", "public");
            // }
        } else {
            filename = new String(filename.getBytes("UTF-8"), "ISO_8859_1");
        }

        response.setContentType(contentType);
        response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");
        response.setHeader("Content-Length", bytes.length + "");

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(response.getOutputStream());
            bos.write(bytes);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected final void display(InputStream is, String filename, String contentType, int length) throws IOException {
        Optional.ofNullable(is).orElseThrow(() -> new RuntimeException("输出流为空"));
        HttpServletResponse response = ServletHolder.getHttpServletResponse();
        HttpServletRequest request = ServletHolder.getHttpServletRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.toLowerCase().indexOf("msie") > 0
                || userAgent.toLowerCase().indexOf("rv:11.0") > 0 || userAgent.toLowerCase().indexOf("edge") > 0)) {// IE
            filename = URLEncoder.encode(filename, "UTF-8");
            filename = filename.replaceAll("\\+", "%20");// 处理空格变+问题
        } else {
            filename = new String(filename.getBytes("UTF-8"), "ISO_8859_1");
        }

        response.setContentType(contentType);
        response.setHeader("Content-disposition", "inline; filename=" + filename);
        response.setHeader("Content-Length", length + "");

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n = 0;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(response.getOutputStream());
            while (-1 != (n = is.read(buffer))) {
                bos.write(buffer, 0, n);
            }
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }

    }

    /**
     * 将BufferedImage输出到响应流里
     * 
     * @param im
     * @param formatName
     * @throws IOException
     */
    protected final void writeImage(RenderedImage im, String formatName) throws IOException {
        ImageIO.write(im, formatName, getResponse().getOutputStream());
    }

    /**
     * 输出响应内容
     * 
     * @param content
     * @throws IOException
     */
    protected final void write(String content) throws IOException {
        HttpServletResponse response = getResponse();
        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.flush();
    }

    /**
     * 返回status code状态
     * 
     * @param code
     */
    protected final void returnStatus(int code) {
        HttpServletResponse response = getResponse();
        response.setStatus(code);
    }

    /**
     * 返回404状态
     */
    @Deprecated
    protected final void return404() {
        HttpServletResponse response = getResponse();
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 域内301跳转
     * 
     * @param location 相对地址
     */
    protected final void redirect301(String location) throws IOException {
        redirect301(location, true);
    }

    /**
     * 域内302跳转
     * 
     * @param location 相对地址
     */
    protected final void redirect302(String location) throws IOException {
        redirect302(location, true);
    }

    /**
     * 域内301跳转
     * 
     * @param location 相对地址
     * @param needQuery 是否带上请求参数
     */
    protected final void redirect301(String location, boolean needQuery) throws IOException {
        redirect(HttpServletResponse.SC_MOVED_PERMANENTLY, location, needQuery);
    }

    /**
     * 域内302跳转
     * 
     * @param location 相对地址
     * @param needQuery 是否带上请求参数
     */
    protected final void redirect302(String location, boolean needQuery) throws IOException {
        redirect(HttpServletResponse.SC_MOVED_TEMPORARILY, location, needQuery);
    }

    /**
     * 域内跳转
     * 
     * @param status HTTP状态码
     * @param location 跳转相对地址
     * @param needQuery 是否带上请求参数
     * @throws IOException
     */
    protected final void redirect(int status, String location, boolean needQuery) throws IOException {
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();

        response.setHeader("Location", togetherUrlRequest(request, location, needQuery));
        response.setStatus(status);
        response.sendRedirect(togetherUrlRequest2(request, location, needQuery));
    }

    /**
     * 跨域跳转
     * <p>
     * 
     * @param status HTTP状态码
     * @param url 跳转绝对地址
     * @param needQuery 是否带上请求参数
     * @throws IOException
     */
    protected final void redirectCrossDomain(int status, String url, boolean needQuery) throws IOException {
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();

        String value = request.getQueryString();
        String location = (needQuery && value != null) ? url + "?" + value : url;
        response.setHeader("Location", location);
        response.setStatus(status);
        response.sendRedirect(location);
    }

    protected final String getPathInfo() {
        return getRequest().getPathInfo();
    }

    protected final String getContextPath() {
        return getRequest().getContextPath();
    }

    protected final String getRequestURI() {
        return getRequest().getRequestURI();
    }

    protected final String getRequestURL() {
        return getRequest().getRequestURL().toString();
    }

    protected final String getServletPath() {
        return getRequest().getServletPath();
    }

    private String togetherUrlRequest(HttpServletRequest request, String location, boolean needQuery) {
        String value = request.getQueryString();
        return request.getScheme() + "://" + request.getHeader("host")
                + ((needQuery && value != null) ? location + "?" + value : location);
    }

    private String togetherUrlRequest2(HttpServletRequest request, String location, boolean needQuery) {
        String value = request.getQueryString();
        return (needQuery && value != null) ? location + "?" + value : location;
    }

    /**
     * CAT 提供了自定义的URL的name功能，只要在HttpServletRequest的设置一个Attribute，
     * 在业务运行代码中加入如下code可以自定义URL下name，这样可以进行自动聚合。
     * 
     * @param name
     */
    protected final void catUriMerge(String name) {
        getRequest().setAttribute("cat-page-uri", name);
    }

    protected final Enumeration<?> getParameterNames() {
        return getRequest().getParameterNames();
    }

    @Deprecated
    protected final String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    @Deprecated
    protected final Map<?, ?> getParameterMap() {
        return getRequest().getParameterMap();
    }

}
