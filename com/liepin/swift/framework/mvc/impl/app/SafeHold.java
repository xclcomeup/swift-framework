package com.liepin.swift.framework.mvc.impl.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class SafeHold {

    private static final Logger logger = Logger.getLogger(SafeHold.class);

    private String schema;
    private String publicKey;
    private String privateKey;
    private String aesKey;
    private List<String> whileList1;
    private Set<String> whileList2;
    private boolean startUp;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public List<String> getWhileList1() {
        return whileList1;
    }

    public void setWhileList1(List<String> whileList1) {
        this.whileList1 = whileList1;
        this.whileList2 = new HashSet<>(whileList1.size());
        whileList1.forEach(t -> {
            URL url;
            try {
                url = new URL(t);
                whileList2.add(url.getPath());
            } catch (MalformedURLException e) {
                logger.error("app encryption whileList format invalid " + t, e);
            }
        });
    }

    public Set<String> getWhileList2() {
        return whileList2;
    }

    public boolean isWhileList(String servletPath) {
        return !whileList2.contains(servletPath);
    }

    public boolean isStartUp() {
        return startUp;
    }

    public void setStartUp(boolean startUp) {
        this.startUp = startUp;
    }

    @Override
    public String toString() {
        return "SafeHold [schema=" + schema + ", publicKey=" + publicKey + ", privateKey=" + privateKey + ", aesKey="
                + aesKey + ", whileList1=" + whileList1 + ", whileList2=" + whileList2 + ", startUp=" + startUp + "]";
    }

}
