package com.liepin.swift.framework.limit;

public class LimitContext {

    private String url;
    private String initClientId;
    private String lastClientId;
    private String originalIp;
    private String currentUserId;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInitClientId() {
        return initClientId;
    }

    public void setInitClientId(String initClientId) {
        this.initClientId = initClientId;
    }

    public String getLastClientId() {
        return lastClientId;
    }

    public void setLastClientId(String lastClientId) {
        this.lastClientId = lastClientId;
    }

    public String getOriginalIp() {
        return originalIp;
    }

    public void setOriginalIp(String originalIp) {
        this.originalIp = originalIp;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public String toString() {
        return "[url=" + url + ", initClientId=" + initClientId + ", lastClientId=" + lastClientId + ", originalIp="
                + originalIp + ", currentUserId=" + currentUserId + "]";
    }

}
