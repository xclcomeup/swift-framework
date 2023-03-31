package com.liepin.swift.framework.mvc.compress;

import javax.servlet.http.HttpServletRequest;

public interface IContentEncodingHandler {

    public String decompress(final HttpServletRequest request) throws Exception;

}
