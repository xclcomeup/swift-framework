package com.liepin.swift.framework.monitor.tomcat;

public class SocketBean {

    /**
     * established、syn-sent、syn-recv、fin-wait-1、fin-wait-2、time-wait、closed、
     * close-wait、last-ack、listen、closing
     * 
     */
    private String state;
    private int recvQ;
    private int sendQ;
    private String localAddress;
    private String peerAddress;

    public SocketBean() {
    }

    public SocketBean(String[] array) {
        this.state = array[0];
        this.recvQ = Integer.parseInt(array[1]);
        this.sendQ = Integer.parseInt(array[2]);
        this.localAddress = getAddress(array[3]);
        this.peerAddress = getAddress(array[4]);
    }

    private String getAddress(String value) {
        // ::ffff:10.10.10.16:57101
        try {
            return value.split("\\:")[3];
        } catch (Exception e) {
        }
        return "unknown";
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getRecvQ() {
        return recvQ;
    }

    public void setRecvQ(int recvQ) {
        this.recvQ = recvQ;
    }

    public int getSendQ() {
        return sendQ;
    }

    public void setSendQ(int sendQ) {
        this.sendQ = sendQ;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    @Override
    public String toString() {
        return "SocketBean [state=" + state + ", recvQ=" + recvQ + ", sendQ=" + sendQ + ", localAddress="
                + localAddress + ", peerAddress=" + peerAddress + "]";
    }

}
