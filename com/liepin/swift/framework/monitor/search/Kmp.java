package com.liepin.swift.framework.monitor.search;

/**
 * 
 * 背景简介：KMP算法用来处理字符串匹配的。给你A,B两个字符串，检查B串是否是A串的子串，类似于Java的String.indexOf("")。
 * 之所以叫做KMP，是因为这个算法是由Knuth、Morris、Pratt三个提出来的，取了这三个人的名字的头一个字母。
 * 
 * 原理介绍：找到匹配失败时的最合适的回退位置，而不是简单的回退到子串的第一个字符（常规的枚举查找方式，是简单的回退到子串的第一个字符，
 * ），即可提高查找的效率。因此为了找到这个合适的位置，先对子串预处理，从而得到一个回退位置的数组。
 * 
 * @author yuanxl
 * @date 2017-9-3 下午11:02:55
 */
public class Kmp {

    /**
     * 对子串加以预处理，从而找到匹配失败时子串回退的位置 找到匹配失败时的最合适的回退位置，而不是回退到子串的第一个字符，即可提高查找的效率
     * 因此为了找到这个合适的位置，先对子串预处理，从而得到一个回退位置的数组
     * 
     * @param 待查找子串的char数组
     * @return
     */
    private static int[] preProcess(char[] array) {
        int size = array.length;
        int[] p = new int[size];
        p[0] = 0;
        int j = 0;
        // 每循环一次，就会找到一个回退位置
        for (int i = 1; i < size; i++) {
            // 当找到第一个匹配的字符时，即j>0时才会执行这个循环
            // 或者说p2中的j++会在p1之前执行（限于第一次执行的条件下）
            // p1
            while (j > 0 && array[j] != array[i]) {
                j = p[j - 1];
            }
            // p2，由此可以看出，只有当子串中含有重复字符时，回退的位置才会被优化
            if (array[j] == array[i]) {
                j++;
            }
            // 找到一个回退位置j，把其放入P[i]中
            p[i] = j;
        }
        return p;
    }

    /**
     * 查找子字符串在目标字符串中出现的次数
     * 
     * @param parStr
     * @param subStr
     * @return
     */
    public static int find(String parStr, String subStr) {
        int subSize = subStr.length();
        int parSize = parStr.length();
        char[] B = subStr.toCharArray();
        char[] A = parStr.toCharArray();
        int[] P = preProcess(B);
        int j = 0;
        int k = 0;
        for (int i = 0; i < parSize; i++) {
            // 当找到第一个匹配的字符时，即j>0时才会执行这个循环
            // 或者说p2中的j++会在p1之前执行（限于第一次执行的条件下）
            // p1
            while (j > 0 && B[j] != A[i]) {
                // 找到合适的回退位置
                j = P[j - 1];
            }
            // p2 找到一个匹配的字符
            if (B[j] == A[i]) {
                j++;
            }
            // 输出匹配结果，并且让比较继续下去
            if (j == subSize) {
                j = P[j - 1];
                k++;
            }
        }
        return k;
    }

}
