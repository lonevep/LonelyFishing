package com.lonelyFishing.config;

import com.lonelyFishing.rods.LoreBonus;
import com.lonelyFishing.rods.MultiCatch;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 解析 config.yml 的 lore-bonuses 节点, 并根据鱼竿 ItemStack 的 lore 匹配出加成列表。
 *
 * 匹配规则: 把鱼竿 lore 与配置中的 lore 关键字都 stripColor 后做 contains 子串匹配,
 *           避免 & 颜色码干扰。
 */
public class LoreBonusManager {

    private List<LoreBonus> bonuses = Collections.<LoreBonus>emptyList();

    public void load(FileConfiguration config) {
        List<LoreBonus> list = new ArrayList<LoreBonus>();
        if (config != null) {
            // 用 getMapList 而非 getList, 元素以 Map 形式返回, 避免 MemorySection 类型转换问题
            for (Map<?, ?> raw : config.getMapList("lore-bonuses")) {
                LoreBonus b = parseBonusMap(raw);
                if (b != null) list.add(b);
            }
        }
        this.bonuses = list;
    }

    private LoreBonus parseBonusMap(Map<?, ?> m) {
        Object loreObj = m.get("lore");
        if (loreObj == null) return null;
        String lore = loreObj.toString();
        if (lore.isEmpty()) return null;

        double bonus = 0.0;
        Object bo = m.get("catch-rate-bonus");
        if (bo instanceof Number) bonus = ((Number) bo).doubleValue();

        List<MultiCatch> multi = new ArrayList<MultiCatch>();
        Object mcObj = m.get("multi-catch");
        if (mcObj instanceof List) {
            for (Object e : (List<?>) mcObj) {
                if (!(e instanceof Map)) continue;
                Map<?, ?> em = (Map<?, ?>) e;
                double chance = 0.0;
                int amount = 1;
                Object co = em.get("chance");
                if (co instanceof Number) chance = ((Number) co).doubleValue();
                Object ao = em.get("amount");
                if (ao instanceof Number) amount = ((Number) ao).intValue();
                if (amount > 1) multi.add(new MultiCatch(chance, amount));
            }
        }
        return new LoreBonus(lore, bonus, multi);
    }

    /**
     * 返回鱼竿上命中的全部 LoreBonus 列表 (按 lore 子串匹配, 已 stripColor)。
     * 鱼竿无 lore / 无 ItemMeta / 不匹配时返回空列表。
     */
    public List<LoreBonus> getBonuses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.<LoreBonus>emptyList();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return Collections.<LoreBonus>emptyList();
        List<String> itemLore = meta.getLore();
        if (itemLore == null || itemLore.isEmpty()) return Collections.<LoreBonus>emptyList();

        // 预处理鱼竿 lore: stripColor 一次
        List<String> stripped = new ArrayList<String>(itemLore.size());
        for (String line : itemLore) {
            stripped.add(line == null ? "" : ChatColor.stripColor(line));
        }

        List<LoreBonus> matched = new ArrayList<LoreBonus>();
        for (LoreBonus b : bonuses) {
            String key = ChatColor.stripColor(b.getLore());
            if (key == null || key.isEmpty()) continue;
            for (String line : stripped) {
                if (line.contains(key)) {
                    matched.add(b);
                    break;
                }
            }
        }
        return matched;
    }

    public List<LoreBonus> all() {
        return bonuses;
    }
}
