package com.lonelyFishing.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色与文本工具
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** 十六进制颜色: &#RRGGBB 或 <#RRGGBB> (仅 1.16+ 客户端显示为 RGB) */
    private static final Pattern HEX_PATTERN =
            Pattern.compile("(?i)(?:&#([0-9A-F]{6})|<#([0-9A-F]{6})>)");

    /** 转换 & 颜色代码与 &#RRGGBB / <#RRGGBB> 十六进制颜色 (1.16+) */
    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', translateHex(input));
    }

    /** 把 &#RRGGBB / <#RRGGBB> 转为 1.16+ 客户端可识别的 §x§R§R§G§G§B§B 格式 */
    private static String translateHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        if (!matcher.find()) return input;
        StringBuffer buffer = new StringBuffer();
        do {
            String hex = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            StringBuilder rep = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                rep.append('\u00a7').append(Character.toUpperCase(c));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rep.toString()));
        } while (matcher.find());
        matcher.appendTail(buffer);
        return buffer.toString();
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
