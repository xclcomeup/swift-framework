package com.liepin.swift.framework.sign;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.liepin.common.other.MD5Util;

/**
 * API 签名验证类
 * 
 * @author yuanxl
 * 
 */
public class APISign {

    private static final Logger logger = Logger.getLogger(APISign.class);

    private static final long EXPIRE_TIME = 30 * 60 * 1000; // 30分钟

    private static final int FREQ = 100;// 截取100个字符

    /**
     * 生成请求参数的签名值
     * <p>
     * 调用签名时内置请求时间：timestamp，用户发起请求时的unix时间戳。<br>
     * 作用：本次请求签名的有效时间为该时间戳+30分钟，用于防止 replay 型攻击。<br>
     * 为保证防止replay攻击算法的正确有效，请保证客户端系统时间正确。<br>
     * <p>
     * 签名算法:<br>
     * 请求参数（特殊排序规则）＋ 请求时间 ＋ 密钥 AES加密 生成签名<br>
     * 将所有参数（包括GET或POST的参数，但不包含签名字段）格式化为“key=value”格式，如“k1=v1”、“k2=v2”、“k3=v3”；<br>
     * 如果value值字符长度超过100，则截取前100个字符作为签名<br>
     * 将格式化好的参数键值对以自定义词典顺序排列后，拼接在一起，如“k1：v1，k2：v2，k3：v3”<br>
     * <br>
     * MD5拼接好的字符串形成string,然后在加上时间戳和密钥，AES后即为签名的值：<br>
     * sign=AES(MD5($k1=$v1$k2=$v2$k3=$v3), $timestamp)<br>
     * 
     * @param params 请求参数
     * @return
     */
    public String sign(final Map<String, Object> params) {
        // 序列化
        String paramsSerialize = serialize(params);

        // 签名
        String md5Serialize = MD5Util.MD5EncodeWithUtf8(paramsSerialize);

        // 请求时间
        long timestamp = System.currentTimeMillis();
        StringBuilder content = new StringBuilder();
        content.append(md5Serialize).append("|").append(timestamp);

        // 加密
        try {
            byte[] sign = encryptCipher.doFinal(content.toString().getBytes());
            return Base64.encodeBase64String(sign);
        } catch (Exception e) {
            logger.error("APISign sign data=" + params + " fail", e);
        }
        return null;
    }

    /**
     * 验证签名的合法性
     * 
     * @param params
     * @param sign
     * @return
     */
    public boolean verify(final Map<String, Object> params, String sign) {
        byte[] bytes = Base64.decodeBase64(sign);
        try {
            // 解密
            byte[] doFinal = decryptCipher.doFinal(bytes);
            String[] data = new String(doFinal).split("\\|");

            // 验证请求实效性
            if ((System.currentTimeMillis() - Long.parseLong(data[1])) > expireTime) {
                return false;
            }

            // 验证请求合法性
            String paramsSerialize = serialize(params);
            String md5Serialize = MD5Util.MD5EncodeWithUtf8(paramsSerialize);
            return (md5Serialize.equals(data[0])) ? true : false;
        } catch (Exception e) {
            logger.error("APISign verify data=" + params + ", sign=" + sign + " fail", e);
        }
        return false;
    }

    /**
     * 密钥算法
     */
    private static final String CIPHER_ALGORITHM = "AES";

    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private long expireTime;

    /**
     * 创建签名类
     * 
     * @param secureKey 密钥，长度必须为16位
     */
    public APISign(String secureKey) throws Exception {
        this(secureKey, EXPIRE_TIME);
    }

    /**
     * 创建签名类
     * 
     * @param secureKey
     * @param expireTime 单位毫秒
     * @throws Exception
     */
    public APISign(String secureKey, long expireTime) throws Exception {
        /**
         * 生成密钥
         */
        SecretKeySpec key = new SecretKeySpec(secureKey.getBytes("UTF-8"), CIPHER_ALGORITHM);

        /**
         * 创建加密密码器
         */
        this.encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        this.encryptCipher.init(Cipher.ENCRYPT_MODE, key); // 初始化，设置为加密模式

        /**
         * 创建解密密码器
         */
        this.decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        this.decryptCipher.init(Cipher.DECRYPT_MODE, key); // 初始化，设置为解密模式
        this.expireTime = expireTime;
    }

    /**
     * 序列化词典
     */
    private static final Map<Character, Integer> WEIGHT_DICT = new HashMap<Character, Integer>();
    /**
     * 排序算法
     */
    private static final Comparator<String> PARAMS_SORT_COMPARATOR = newComparator();

    static {
        WEIGHT_DICT.put('a', 8);
        WEIGHT_DICT.put('b', 32);
        WEIGHT_DICT.put('c', 20);
        WEIGHT_DICT.put('d', 2);
        WEIGHT_DICT.put('e', 10);
        WEIGHT_DICT.put('f', 42);
        WEIGHT_DICT.put('g', 16);
        WEIGHT_DICT.put('h', 24);
        WEIGHT_DICT.put('i', 29);
        WEIGHT_DICT.put('j', 11);
        WEIGHT_DICT.put('k', 22);
        WEIGHT_DICT.put('l', 26);
        WEIGHT_DICT.put('m', 48);
        WEIGHT_DICT.put('n', 4);
        WEIGHT_DICT.put('o', 27);
        WEIGHT_DICT.put('p', 15);
        WEIGHT_DICT.put('q', 50);
        WEIGHT_DICT.put('r', 0);
        WEIGHT_DICT.put('s', 3);
        WEIGHT_DICT.put('t', 30);
        WEIGHT_DICT.put('u', 46);
        WEIGHT_DICT.put('v', 14);
        WEIGHT_DICT.put('w', 37);
        WEIGHT_DICT.put('x', 49);
        WEIGHT_DICT.put('y', 51);
        WEIGHT_DICT.put('z', 1);
        WEIGHT_DICT.put('A', 18);
        WEIGHT_DICT.put('B', 28);
        WEIGHT_DICT.put('C', 43);
        WEIGHT_DICT.put('D', 21);
        WEIGHT_DICT.put('E', 35);
        WEIGHT_DICT.put('F', 19);
        WEIGHT_DICT.put('G', 5);
        WEIGHT_DICT.put('H', 25);
        WEIGHT_DICT.put('I', 39);
        WEIGHT_DICT.put('J', 12);
        WEIGHT_DICT.put('K', 41);
        WEIGHT_DICT.put('L', 23);
        WEIGHT_DICT.put('M', 36);
        WEIGHT_DICT.put('N', 7);
        WEIGHT_DICT.put('O', 17);
        WEIGHT_DICT.put('P', 6);
        WEIGHT_DICT.put('Q', 33);
        WEIGHT_DICT.put('R', 44);
        WEIGHT_DICT.put('S', 34);
        WEIGHT_DICT.put('T', 47);
        WEIGHT_DICT.put('U', 40);
        WEIGHT_DICT.put('V', 13);
        WEIGHT_DICT.put('W', 31);
        WEIGHT_DICT.put('X', 38);
        WEIGHT_DICT.put('Y', 45);
        WEIGHT_DICT.put('Z', 9);
    }

    private static String serialize(final Map<String, Object> params) {
        TreeMap<String, Object> sortMap = new TreeMap<String, Object>(PARAMS_SORT_COMPARATOR);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sortMap.put(entry.getKey(), entry.getValue());
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(subString(entry.getValue().toString(), FREQ));
        }
        return sb.toString();
    }

    private static String subString(String value, int freq) {
        int length = value.length();
        if (length <= freq) {
            return value;
        }
        return value.substring(0, freq);
    }

    private static Comparator<String> newComparator() {
        return new Comparator<String>() {

            @Override
            public int compare(String s1, String s2) {
                char[] arr1 = s1.toCharArray();
                char[] arr2 = s2.toCharArray();
                if (arr1.length > arr2.length) {
                    return 1;
                }
                if (arr1.length < arr2.length) {
                    return -1;
                }
                for (int i = 0; i < arr1.length; i++) {
                    char c1 = arr1[i];
                    char c2 = arr2[i];
                    Integer w1 = WEIGHT_DICT.get(c1);
                    Integer w2 = WEIGHT_DICT.get(c2);
                    if (w1 == null && w2 == null) {
                        continue;
                    }
                    if (w1 == null) {
                        return -1;
                    }
                    if (w2 == null) {
                        return 1;
                    }
                    if (w1.intValue() == w2.intValue()) {
                        continue;
                    }
                    return (w1.intValue() > w2.intValue()) ? 1 : -1;
                }
                return 0;
            }

        };
    }

}
