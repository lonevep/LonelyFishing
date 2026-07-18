package com.lonelyFishing.config;

import com.lonelyFishing.items.GroupItem;
import com.lonelyFishing.items.ItemGroup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析 itemGroups.yml -> 内存 ItemGroup 集合
 */
public class ItemGroupManager {

    private final Map<String, ItemGroup> groups = new HashMap<String, ItemGroup>();

    public void load(FileConfiguration config) {
        groups.clear();
        if (config == null) return;
        ConfigurationSection root = config.getConfigurationSection("item-groups");
        if (root == null) return;
        for (String groupId : root.getKeys(false)) {
            ConfigurationSection gSec = root.getConfigurationSection(groupId);
            if (gSec == null) continue;
            ConfigurationSection itemsSec = gSec.getConfigurationSection("items");
            List<GroupItem> items = new ArrayList<GroupItem>();
            if (itemsSec != null) {
                for (String entryKey : itemsSec.getKeys(false)) {
                    ConfigurationSection iSec = itemsSec.getConfigurationSection(entryKey);
                    if (iSec == null) continue;
                    GroupItem gi = parseItem(entryKey, iSec);
                    if (gi != null) items.add(gi);
                }
            }
            groups.put(groupId, new ItemGroup(groupId, items));
        }
    }

    private GroupItem parseItem(String id, ConfigurationSection s) {
        String source = s.getString("source", "mm");
        String itemId = s.getString("item-id", id);
        double weight = s.getDouble("weight", 0);
        // 留空 (未配置) 则为 null, 表示不给予
        Double money = s.contains("money") ? s.getDouble("money") : null;
        Integer points = s.contains("points") ? s.getInt("points") : null;
        Integer exp = s.contains("exp") ? s.getInt("exp") : null;
        List<String> commands = s.getStringList("commands");
        return new GroupItem(id, source, itemId, weight, money, points, exp, commands);
    }

    public ItemGroup getGroup(String id) {
        return groups.get(id);
    }

    public Map<String, ItemGroup> getAll() {
        return groups;
    }
}
