package com.liepin.swift.framework.mvc.filter.external;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liepin.swift.core.exception.BizException;

/**
 * 扩展uri字符集，默认UTF-8
 * 
 * @author yuanxl
 * @date 2015-11-11 下午04:45:57
 */
public class DefaultCharacterEncodingFilter extends ExternalFilter {

    @Override
    public boolean external(HttpServletRequest request, HttpServletResponse response) throws BizException {
        try {
            request.setCharacterEncoding(encoding());
        } catch (UnsupportedEncodingException e) {
        }
        response.setCharacterEncoding(encoding());
        return true;
    }

    /**
     * 设置字符集
     * 
     * @return
     */
    protected String encoding() {
        return CHARSET_UTF_8;
    }

}
