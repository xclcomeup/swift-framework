package com.liepin.swift.framework.security.firewall;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

public class StatusCodeStrictHttpFirewall extends StrictHttpFirewall {

    private static final String KEY = "RequestRejectedException_Happen";

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
        FirewalledRequest firewalledRequest = null;
        try {
            firewalledRequest = super.getFirewalledRequest(request);
            Optional.ofNullable(request.getAttribute("javax.servlet.error.exception")).ifPresent(e -> {
                if (e instanceof RequestRejectedException) {
                    current().put(KEY, (RequestRejectedException) e);
                }
            });
        } catch (RequestRejectedException e) {
            current().put(KEY, e);
        }
        return firewalledRequest;
    }

    @Override
    public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
        RequestRejectedException requestRejectedException = (RequestRejectedException) current().remove(KEY);
        if (Objects.nonNull(requestRejectedException)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            throw requestRejectedException;
        }
        return super.getFirewalledResponse(response);
    }

    private static Map<String, Object> current() {
        return context.get();
    }

    private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<Map<String, Object>>() {
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

}
