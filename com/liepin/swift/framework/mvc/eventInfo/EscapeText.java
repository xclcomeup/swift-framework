package com.liepin.swift.framework.mvc.eventInfo;

import java.util.HashMap;
import java.util.Map;

import com.liepin.common.conf.PropUtil;

public class EscapeText {

    /**
     * 需要模糊的日志输出字段列表
     * <p>
     * 格式： key: matchStart | value: matchEnd
     */
    private static final Map<String, String> secretProperties = new HashMap<String, String>();
    private static final String PROMPT_MESSAGE = "Forbidden Access";

    static {
        // 从config.properties读取过滤的字符串key
        String value = PropUtil.getInstance().get("secret.key.properties");
        if (value != null && value.trim().length() != 0) {
            String[] array = value.split(",");
            setSecretProperties(array);
        }
    }

    private static void setSecretProperties(String[] array) {
        for (String key : array) {
            regSecret(key);
        }
    }

    /**
     * 设置匹配规则
     * 
     * @param key
     */
    private static void regSecret(String key) {
        secretProperties.put("\"" + key + "\":\"", "\"");
        secretProperties.put("\\\"" + key + "\\\":\\\"", "\\\"");
    }

    /**
     * 模糊字段内容
     * 
     * @param value
     * @return
     */
    private static String blur(String value) {
        if (value == null || value.trim().length() == 0) {
            return value;
        }
        int length = value.length();
        if (length < 5) {
            return blur(length);
        }

        int blurCnt = (length % 2 == 0) ? length / 2 : length / 2 + 1;
        int begin = (length - blurCnt) / 2;
        char[] charArray = value.toCharArray();
        int count = 0;
        boolean start = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charArray.length; i++) {
            if (i == begin) {
                start = true;
            }
            if (start) {
                sb.append("*");
                count++;
            } else {
                sb.append(charArray[i]);
            }
            if (count >= blurCnt) {
                start = false;
            }
        }
        return sb.toString();
    }

    private static String blur(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    /**
     * 混淆
     * 
     * @param message
     * @return
     */
    public static String confuseChars(String message) {
        if (secretProperties.size() == 0) {
            return message;
        }
        StringBuilder log = new StringBuilder(message);
        for (Map.Entry<String, String> entry : secretProperties.entrySet()) {
            String tag = entry.getKey();
            int start = -1;
            int fromIndex = 0;
            while ((start = log.indexOf(tag, fromIndex)) != -1) {
                int end = log.indexOf(entry.getValue(), start + tag.length());
                if (end == -1) {
                    break;
                }
                String value = log.substring(start + tag.length(), end);
                log.replace(start + tag.length(), end, blur(value));
                fromIndex = end;
            }
        }
        return log.toString();
    }

    public static String confuseAndIgnoreChars(String chars, int max) {
        if (chars == null) {
            return "";
        } else {
            int ignoredChars = chars.length() - max;
            if (ignoredChars > 0) {
                String output = confuseChars(chars.substring(0, max));
                return output + "(... more " + ignoredChars + " chars ignored)";
            } else {
                return confuseChars(chars);
            }
        }
    }

    /**
     * 
     * {"clientId":"10062","currentUserId":"118811","data":
     * "{\"text\":\"ab奖励c\",\"_enumSchema\":null}"}<br>
     * =><br>
     * {"clientId":"10062","currentUserId":"118811","data":"Forbidden
     * Access"}<br>
     * 
     * @param text
     * @return
     */
    public static String ignoreChars4Data(String text) {
        int pos = text.indexOf("\"data\":");
        if (pos != -1) {
            return text.substring(0, pos + 7) + "\"" + PROMPT_MESSAGE + "\"}";
        }
        return text;
    }

}
