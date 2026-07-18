package com.lonelyFishing.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色与文本工具
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** 转换 & 颜色代码 */
    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<String>();
        if (input == null) return out;
        for (String s : input) {
            out.add(color(s));
        }
        return out;
    }
}
