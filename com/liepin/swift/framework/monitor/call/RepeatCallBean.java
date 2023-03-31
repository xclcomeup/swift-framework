package com.liepin.swift.framework.monitor.call;

import java.util.List;

public class RepeatCallBean {

    private String remoteProject;
    private String url;
    private List<String> track;
    private String paramString;

    public String getRemoteProject() {
        return remoteProject;
    }

    public void setRemoteProject(String remoteProject) {
        this.remoteProject = remoteProject;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getTrack() {
        return track;
    }

    public void setTrack(List<String> track) {
        this.track = track;
    }

    public String getParamString() {
        return paramString;
    }

    public void setParamString(String paramString) {
        this.paramString = paramString;
    }

    @Override
    public String toString() {
        return "RepeatCallBean [remoteProject=" + remoteProject + ", url=" + url + ", track=" + track
                + ", paramString=" + paramString + "]";
    }

}
