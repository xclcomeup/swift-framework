package com.liepin.swift.framework.mvc.compress;

import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public enum AcceptEncoding implements IAcceptEncodingHandler {

    gzip() {

        @Override
        public void compress(final HttpServletResponse response, String content)
                throws Exception {
            response.addHeader("Content-Encoding", "gzip");
            GZIPOutputStream gzipos = new GZIPOutputStream(response.getOutputStream());
            gzipos.write(content.getBytes("UTF-8"));
            gzipos.flush();
            gzipos.close();
        }

    },
    deflate() {

        @Override
        public void compress(final HttpServletResponse response, String content)
                throws Exception {
            throw new UnsupportedOperationException("Accept-Encoding:\"deflate\" 压缩算法暂不支持, 敬请期待!");
        }

    },
    br() {

        @Override
        public void compress(final HttpServletResponse response, String content)
                throws Exception {
            throw new UnsupportedOperationException("Accept-Encoding:\"br\" 压缩算法暂不支持, 敬请期待!");
        }

    };

    public static AcceptEncoding support(String contentEncoding) {
        return Optional.ofNullable(contentEncoding).map(t -> {
            // FIXME 暂时只支持gzip
            return (t.trim().toLowerCase().indexOf("gzip") != -1) ? AcceptEncoding.gzip : null;
        }).orElse(null);
    }

    public static AcceptEncoding support(final HttpServletRequest request) {
        return support(request.getHeader("accept-encoding"));
    }

}
