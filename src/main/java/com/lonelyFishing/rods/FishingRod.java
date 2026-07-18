package com.lonelyFishing.rods;

import com.lonelyFishing.util.RandomUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义鱼竿配置
 */
public class FishingRod {

    private final String id;
    private final String source;          // mm / ni
    private final String itemId;          // 物品库节点名 (用于鱼竿判定)
    private final List<String> itemGroupIds;
    private final double moneyMultiplier;
    private final double pointsMultiplier;
    private final double expMultiplier;
    private final String reelCountSpec;   // "3" 或 "1_10"
    private final List<String> commands;  // 钓上物品后触发的指令

    public FishingRod(String id, String source, String itemId, List<String> itemGroupIds,
                      double moneyMultiplier, double pointsMultiplier, double expMultiplier,
                      String reelCountSpec, List<String> commands) {
        this.id = id;
        this.source = source;
        this.itemId = itemId;
        this.itemGroupIds = itemGroupIds == null ? new ArrayList<String>() : itemGroupIds;
        this.moneyMultiplier = moneyMultiplier;
        this.pointsMultiplier = pointsMultiplier;
        this.expMultiplier = expMultiplier;
        this.reelCountSpec = reelCountSpec;
        this.commands = commands == null ? new ArrayList<String>() : commands;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public String getItemId() { return itemId; }
    public List<String> getItemGroupIds() { return Collections.unmodifiableList(itemGroupIds); }
    public double getMoneyMultiplier() { return moneyMultiplier; }
    public double getPointsMultiplier() { return pointsMultiplier; }
    public double getExpMultiplier() { return expMultiplier; }
    public List<String> getCommands() { return commands; }

    /** 依据 reelCountSpec 随机一次本周期所需的收竿次数 */
    public int rollReelCount() {
        return RandomUtil.parseRange(reelCountSpec);
    }
}
