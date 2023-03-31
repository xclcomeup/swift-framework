package com.liepin.swift.framework.monitor.proc.io;

import java.io.File;
import java.text.MessageFormat;

import com.liepin.common.file.IoUtil;
import com.liepin.common.file.IoUtil.FileLoading;

public class ProcForIo {

    private static final String IO_FILE_LINUX = "/proc/{0}/io";

    /**
     * 返回当前进程使用IO情况
     * 
     * @param pid
     * @return
     */
    public static IoBean print(String pid) {
        final IoBean ioBean = new IoBean();
        String path = MessageFormat.format(IO_FILE_LINUX, new Object[] { pid });
        IoUtil.load(new File(path), "UTF-8", new FileLoading() {

            @Override
            public boolean row(String line, int n) {
                ioBean.parse(line);
                return true;
            }

        });
        return ioBean;
    }

}
