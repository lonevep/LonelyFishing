package com.lonelyFishing.security;

/**
 * 字符串轻量加密工具。
 * 将敏感字符串 (如作者名) 以 XOR 加密形式存储于字节码中, 运行时解密,
 * 避免明文直接出现在反编译结果里。无缓存以避免长期驻留, 解密开销极低。
 */
public final class Sec {

    // 密钥 (仅用于异或)
    private static final int[] KEY = new int[]{76, 70, 35, 50, 48, 50, 52};

    // "lone_vep" 的 XOR 密文
    private static final int[] ENC_AUTHOR = new int[]{32, 41, 77, 87, 111, 68, 81, 60};

    private Sec() {}

    /** 解密作者名 */
    public static String author() {
        return decrypt(ENC_AUTHOR);
    }

    private static String decrypt(int[] enc) {
        char[] out = new char[enc.length];
        for (int i = 0; i < enc.length; i++) {
            out[i] = (char) (enc[i] ^ KEY[i % KEY.length]);
        }
        return new String(out);
    }
}
