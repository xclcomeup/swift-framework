package com.liepin.swift.framework.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.liepin.common.datastructure.ThreadLocalRandom;

public class CommonUtil {

    private static final char[] zimu = "abcdefghijkmnopqrstuvwsyz0123456789".toCharArray();

    public static String randomLetterOrDigit(int length) {
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < length; j++) {
            builder.append(zimu[ThreadLocalRandom.current().nextInt(zimu.length)]);
        }
        return builder.toString();
    }

    public static String intercept(String chars, int max) {
        int ignoredCharsLength = chars.length() - max;
        return (ignoredCharsLength > 0)
                ? chars.substring(0, max) + "(... more " + ignoredCharsLength + " chars ignored)" : chars;
    }

    public static byte[] inputStreamToByte(InputStream in) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int count;
        while ((count = in.read(data, 0, 4096)) != -1) {
            outStream.write(data, 0, count);
        }
        return outStream.toByteArray();
    }

    public static boolean ignore(String line) {
        return line.trim().length() == 0 || line.trim().startsWith("#");
    }

}
