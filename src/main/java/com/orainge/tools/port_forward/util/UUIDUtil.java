package com.orainge.tools.port_forward.util;

import java.util.UUID;

/**
 * UUID 工具类
 *
 * @author orainge
 * @since 2021/11/19
 */
public class UUIDUtil {
    /**
     * 生成短 UUID 的字符数组
     */
    private static final String[] UUID_CHAR_ARRAY = new String[]{
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B",
            "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    /**
     * 获取短 UUID
     */
    public static String getShortUuid() {
        StringBuilder b = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        for (int i = 0; i < 8; i++) {
            int j = i << 2;
            String str = uuid.substring(j, j + 4);
            int x = Integer.parseInt(str, 16);
            b.append(UUID_CHAR_ARRAY[x % 0x3E]);
        }

        return b.toString();
    }
}
