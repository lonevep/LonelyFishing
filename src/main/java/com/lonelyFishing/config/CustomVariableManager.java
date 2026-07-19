package com.lonelyFishing.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析 config.yml 的 custom-variables 节点 -> 内存 CustomVariable 集合。
 *
 * 配置示例:
 *   custom-variables:
 *     ll_level:
 *       placeholder: "%ll_level-默认%"
 *       add-command: "ll add %player_name% {amount} 默认"
 *       message: "&a+{amount} 等级 &7(当前: &e{current}&7)"
 *
 * 变量在物品组 (itemGroups.yml) 中被引用为基础值, 在鱼竿 (rods.yml) 中被引用为倍率。
 * 最终增加量 = 物品基础值 * 鱼竿倍率。
 */
public class CustomVariableManager {

    private final Map<String, CustomVariable> variables = new HashMap<String, CustomVariable>();

    public void load(FileConfiguration config) {
        variables.clear();
        if (config == null) return;
        ConfigurationSection root = config.getConfigurationSection("custom-variables");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection vSec = root.getConfigurationSection(name);
            if (vSec == null) continue;
            String placeholder = vSec.getString("placeholder", "");
            String addCommand = vSec.getString("add-command", "");
            String message = vSec.getString("message", "");
            // placeholder 和 add-command 都为空则无意义, 跳过
            if (placeholder.isEmpty() && addCommand.isEmpty()) continue;
            variables.put(name, new CustomVariable(name, placeholder, addCommand, message));
        }
    }

    public CustomVariable get(String name) {
        return variables.get(name);
    }

    public Map<String, CustomVariable> getAll() {
        return variables;
    }
}