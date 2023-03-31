package com.liepin.swift.framework.mvc.compress;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.liepin.common.other.GzipUtil;

public enum ContentEncoding implements IContentEncodingHandler {

    gzip() {

        @Override
        public String decompress(HttpServletRequest request) throws Exception {
            return GzipUtil.uncompress(request.getInputStream());
        }

    },
    deflate() {

        @Override
        public String decompress(HttpServletRequest request) throws Exception {
            throw new UnsupportedOperationException("Content-Encoding:\"deflate\" 压缩算法暂不支持, 敬请期待!");
        }

    },
    br() {

        @Override
        public String decompress(HttpServletRequest request) throws Exception {
            throw new UnsupportedOperationException("Content-Encoding:\"br\" 压缩算法暂不支持, 敬请期待!");
        }

    };

    public static ContentEncoding support(String contentEncoding) {
        return Optional.ofNullable(contentEncoding).map(t -> {
            switch (t.trim().toLowerCase()) {
                case "gzip":
                    return ContentEncoding.gzip;
                case "deflate":
                    return ContentEncoding.deflate;
                case "br":
                    return ContentEncoding.br;
                default:
                    return null;
            }
        }).orElse(null);
    }

    public static ContentEncoding support(final HttpServletRequest request) {
        return support(request.getHeader("content-encoding"));
    }

}
