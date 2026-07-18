package com.lonelyFishing.util;

import java.util.Random;

/**
 * 随机数工具。支持解析 "3" 或 "1_10" 形式的区间
 */
public final class RandomUtil {

    private static final Random RANDOM = new Random();

    private RandomUtil() {}

    /**
     * 解析区间字符串并返回一个随机整数。
     * "3"     -> 3
     * "1_10"  -> 1..10 (含两端)
     * 解析失败或非法时返回 1
     */
    public static int parseRange(String spec) {
        if (spec == null || spec.trim().isEmpty()) return 1;
        spec = spec.trim();
        int idx = spec.indexOf('_');
        if (idx < 0) {
            try {
                return Math.max(1, Integer.parseInt(spec));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        try {
            int min = Integer.parseInt(spec.substring(0, idx).trim());
            int max = Integer.parseInt(spec.substring(idx + 1).trim());
            if (min > max) { int t = min; min = max; max = t; }
            if (max < 1) max = 1;
            if (min < 1) min = 1;
            return min + RANDOM.nextInt(max - min + 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static int nextInt(int bound) {
        if (bound <= 0) return 0;
        return RANDOM.nextInt(bound);
    }

    public static Random random() {
        return RANDOM;
    }
}
