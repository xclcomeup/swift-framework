package com.liepin.swift.framework.monitor.proc.io;

public class IoBean {

    private long syscr = -1; // read()或者pread()总的调用次数
    private long syscw = -1; // write()或者pwrite()总的调用次数
    private long readBytes = -1; // 实际从磁盘中读取的字节总数
    private long writeBytes = -1; // 实际写入到磁盘中的字节总数

    public IoBean() {

    }

    public long getSyscr() {
        return syscr;
    }

    public void setSyscr(long syscr) {
        this.syscr = syscr;
    }

    public long getSyscw() {
        return syscw;
    }

    public void setSyscw(long syscw) {
        this.syscw = syscw;
    }

    public long getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    public long getWriteBytes() {
        return writeBytes;
    }

    public void setWriteBytes(long writeBytes) {
        this.writeBytes = writeBytes;
    }

    @Override
    public String toString() {
        return "IoBean [syscr=" + syscr + ", syscw=" + syscw + ", readBytes=" + readBytes + ", writeBytes="
                + writeBytes + "]";
    }

    public void parse(String line) {
        String[] array = line.split("\\:");
        if (array.length != 2) {
            return;
        }
        String name = array[0].trim();
        long value = Long.parseLong(array[1].trim());
        if ("syscr".equals(name)) {
            setSyscr(value);
        } else if ("syscw".equals(name)) {
            setSyscw(value);
        } else if ("read_bytes".equals(name)) {
            setReadBytes(value);
        } else if ("write_bytes".equals(name)) {
            setWriteBytes(value);
        }
    }

}
