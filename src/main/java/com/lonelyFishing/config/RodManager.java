package com.lonelyFishing.config;

import com.lonelyFishing.hooks.MythicMobsHook;
import com.lonelyFishing.hooks.NeigeItemsHook;
import com.lonelyFishing.rods.FishingRod;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析 rods.yml -> 内存 FishingRod 集合, 并提供手中鱼竿匹配
 */
public class RodManager {

    private final MythicMobsHook mm;
    private final NeigeItemsHook ni;
    private final Map<String, FishingRod> rods = new HashMap<String, FishingRod>();

    public RodManager(MythicMobsHook mm, NeigeItemsHook ni) {
        this.mm = mm;
        this.ni = ni;
    }

    public void load(FileConfiguration config) {
        rods.clear();
        if (config == null) return;
        ConfigurationSection root = config.getConfigurationSection("rods");
        if (root == null) return;
        for (String rodId : root.getKeys(false)) {
            ConfigurationSection rSec = root.getConfigurationSection(rodId);
            if (rSec == null) continue;
            FishingRod rod = parseRod(rodId, rSec);
            if (rod != null) rods.put(rodId, rod);
        }
    }

    private FishingRod parseRod(String id, ConfigurationSection s) {
        String source = s.getString("source", "mm");
        String itemId = s.getString("item-id", "");
        List<String> groupIds = s.getStringList("item-groups");
        ConfigurationSection m = s.getConfigurationSection("multipliers");
        double moneyMul = m != null ? m.getDouble("money", 1.0) : 1.0;
        double pointsMul = m != null ? m.getDouble("points", 1.0) : 1.0;
        double expMul = m != null ? m.getDouble("exp", 1.0) : 1.0;
        String reelCount = s.getString("reel-count", "1");
        List<String> commands = s.getStringList("commands");
        // 基础钓上鱼概率 (0-1), 不填默认 1.0 (100%); 配合 lore-bonuses 做乘法叠加
        double catchRate = s.getDouble("catch-rate", 1.0);
        // 鱼咬钩前等待时间 / 咬钩后拉杆窗口 (ticks 区间 "min_max"), 留空则用原版
        // 仅 1.9+ 服务端支持精确控制, 1.8 反射找不到方法会静默跳过
        String waitTime = s.getString("wait-time", null);
        String lureTime = s.getString("lure-time", null);
        // 倍率提示自定义 (留空则用 messages.yml 默认)
        String mulInfo = s.getString("multiplier-info", null);
        String mulMoney = s.getString("multiplier-money", null);
        String mulPoints = s.getString("multiplier-points", null);
        String mulExp = s.getString("multiplier-exp", null);
        // 自定义变量倍率 (变量名 -> 倍率) 与消息覆盖 (变量名 -> 模板), 对应 config.yml 的 custom-variables
        Map<String, Double> varMul = new HashMap<String, Double>();
        Map<String, String> varMsg = new HashMap<String, String>();
        ConfigurationSection vmSec = s.getConfigurationSection("variable-multipliers");
        if (vmSec != null) {
            for (String varName : vmSec.getKeys(false)) {
                varMul.put(varName, vmSec.getDouble(varName, 1.0));
            }
        }
        ConfigurationSection vsSec = s.getConfigurationSection("variable-messages");
        if (vsSec != null) {
            for (String varName : vsSec.getKeys(false)) {
                varMsg.put(varName, vsSec.getString(varName, null));
            }
        }
        if (itemId.isEmpty()) return null;
        return new FishingRod(id, source, itemId, groupIds, moneyMul, pointsMul, expMul, reelCount, commands, catchRate, waitTime, lureTime, mulInfo, mulMoney, mulPoints, mulExp, varMul, varMsg);
    }

    /**
     * 匹配玩家手中的鱼竿, 返回对应配置; 不匹配返回 null。
     * 直接通过 mm / ni 物品库的内部名/节点名进行判定。
     */
    public FishingRod match(ItemStack held) {
        if (held == null) return null;
        for (FishingRod rod : rods.values()) {
            String src = rod.getSource();
            if ("mm".equalsIgnoreCase(src) && mm != null && mm.isEnabled()) {
                String name = mm.getInternalName(held);
                if (name != null && name.equalsIgnoreCase(rod.getItemId())) return rod;
            } else if ("ni".equalsIgnoreCase(src) && ni != null && ni.isEnabled()) {
                String name = ni.getItemId(held);
                if (name != null && name.equalsIgnoreCase(rod.getItemId())) return rod;
            }
        }
        return null;
    }

    public Collection<FishingRod> all() {
        return rods.values();
    }

    public FishingRod get(String id) {
        return rods.get(id);
    }
}
