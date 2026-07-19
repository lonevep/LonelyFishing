package com.lonelyFishing.config;

import com.lonelyFishing.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集中加载与访问 config.yml / rods.yml / itemGroups.yml / messages.yml
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration general;
    private FileConfiguration rods;
    private FileConfiguration itemGroups;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 释放默认配置 (仅在文件不存在时) */
    public void saveDefaults() {
        saveDefault("config.yml");
        saveDefault("rods.yml");
        saveDefault("itemGroups.yml");
        saveDefault("messages.yml");
    }

    private void saveDefault(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    /** 重新加载全部配置 */
    public void reload() {
        saveDefaults();
        general = load("config.yml");
        rods = load("rods.yml");
        itemGroups = load("itemGroups.yml");
        messages = load("messages.yml");
    }

    private FileConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        // 合并 jar 内默认值作为兜底 (不覆盖用户已写的键)
        InputStream in = plugin.getResource(name);
        if (in != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            yaml.setDefaults(def);
        }
        return yaml;
    }

    public FileConfiguration getGeneral() { return general; }
    public FileConfiguration getRods() { return rods; }
    public FileConfiguration getItemGroups() { return itemGroups; }

    public boolean blockVanillaRods() {
        return general != null && general.getBoolean("block-vanilla-rods", true);
    }

    public int messageCooldown() {
        return general != null ? general.getInt("message-cooldown", 3) : 3;
    }

    public boolean dropWhenFull() {
        return general != null && general.getBoolean("drop-when-full", true);
    }

    public String prefix() {
        return ColorUtil.color(messages == null ? "" : messages.getString("prefix", ""));
    }

    /** 取消息原文 (已上色), 不带 prefix */
    public String msg(String key) {
        return ColorUtil.color(messages == null ? "" : messages.getString(key, ""));
    }

    /** 取多行消息列表 (已上色), 不带 prefix; 可选占位符替换 */
    public List<String> msgList(String key, String... pairs) {
        List<String> out = new ArrayList<String>();
        if (messages == null) return out;
        for (String line : messages.getStringList(key)) {
            out.add(ColorUtil.color(replace(line, pairs)));
        }
        return out;
    }

    /** 取消息并替换占位符 (已上色, 带 prefix) */
    public String format(String key, String... pairs) {
        String raw = messages == null ? "" : messages.getString(key, "");
        raw = replace(raw, pairs);
        return prefix() + ColorUtil.color(raw);
    }

    /**
     * 对任意模板字符串做占位符替换 + 上色 (不带 prefix)。
     * 用于鱼竿自定义消息等非 messages.yml 来源的模板。
     * 对已上色字符串再次 color 是幂等的 (translateAlternateColorCodes 只处理 &).
     */
    public String formatTemplate(String template, String... pairs) {
        String s = template == null ? "" : template;
        s = replace(s, pairs);
        return ColorUtil.color(s);
    }

    /** 把 {k} -> v 形式的占位符替换掉 */
    private static String replace(String src, String... pairs) {
        if (src == null) return "";
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            src = src.replace("{" + e.getKey() + "}", e.getValue());
        }
        return src;
    }
}
