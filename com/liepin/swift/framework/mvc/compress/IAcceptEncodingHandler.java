package com.liepin.swift.framework.mvc.compress;

import javax.servlet.http.HttpServletResponse;

public interface IAcceptEncodingHandler {

    public void compress(final HttpServletResponse response, String content) throws Exception;

}
