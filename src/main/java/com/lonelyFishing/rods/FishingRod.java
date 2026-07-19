package com.lonelyFishing.rods;

import com.lonelyFishing.util.RandomUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final double catchRate;       // 基础钓上鱼概率 (0-1, 默认 1.0 = 100%)
    private final String waitTimeSpec;    // 鱼咬钩前等待时间区间 "100_300" (ticks), null 用原版
    private final String lureTimeSpec;    // 鱼咬钩后拉杆窗口区间 "200_400" (ticks), null 用原版
    // 倍率提示自定义 (null/空 则用 messages.yml 默认)
    private final String multiplierInfo;     // 合三为一提示, 占位符 {money}{points}{exp}{player}
    private final String multiplierMoney;    // 金币独立提示
    private final String multiplierPoints;   // 点券独立提示
    private final String multiplierExp;      // 经验独立提示
    // 自定义变量倍率/消息覆盖 (变量名 -> 倍率/消息模板), 对应 config.yml 的 custom-variables
    private final Map<String, Double> variableMultipliers;  // 变量名 -> 倍率 (留空默认 1.0)
    private final Map<String, String> variableMessages;     // 变量名 -> 消息模板 (留空则用 config.yml 默认 message)

    public FishingRod(String id, String source, String itemId, List<String> itemGroupIds,
                      double moneyMultiplier, double pointsMultiplier, double expMultiplier,
                      String reelCountSpec, List<String> commands, double catchRate,
                      String waitTimeSpec, String lureTimeSpec,
                      String multiplierInfo, String multiplierMoney,
                      String multiplierPoints, String multiplierExp,
                      Map<String, Double> variableMultipliers,
                      Map<String, String> variableMessages) {
        this.id = id;
        this.source = source;
        this.itemId = itemId;
        this.itemGroupIds = itemGroupIds == null ? new ArrayList<String>() : itemGroupIds;
        this.moneyMultiplier = moneyMultiplier;
        this.pointsMultiplier = pointsMultiplier;
        this.expMultiplier = expMultiplier;
        this.reelCountSpec = reelCountSpec;
        this.commands = commands == null ? new ArrayList<String>() : commands;
        this.catchRate = catchRate;
        this.waitTimeSpec = waitTimeSpec;
        this.lureTimeSpec = lureTimeSpec;
        this.multiplierInfo = multiplierInfo;
        this.multiplierMoney = multiplierMoney;
        this.multiplierPoints = multiplierPoints;
        this.multiplierExp = multiplierExp;
        this.variableMultipliers = variableMultipliers == null
                ? Collections.<String, Double>emptyMap() : variableMultipliers;
        this.variableMessages = variableMessages == null
                ? Collections.<String, String>emptyMap() : variableMessages;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public String getItemId() { return itemId; }
    public List<String> getItemGroupIds() { return Collections.unmodifiableList(itemGroupIds); }
    public double getMoneyMultiplier() { return moneyMultiplier; }
    public double getPointsMultiplier() { return pointsMultiplier; }
    public double getExpMultiplier() { return expMultiplier; }
    public List<String> getCommands() { return commands; }
    /** 基础钓上鱼概率 (0-1), 不含 lore 加成 */
    public double getCatchRate() { return catchRate; }
    public String getWaitTimeSpec() { return waitTimeSpec; }
    public String getLureTimeSpec() { return lureTimeSpec; }
    public String getMultiplierInfo() { return multiplierInfo; }
    public String getMultiplierMoney() { return multiplierMoney; }
    public String getMultiplierPoints() { return multiplierPoints; }
    public String getMultiplierExp() { return multiplierExp; }
    public Map<String, Double> getVariableMultipliers() { return variableMultipliers; }
    public Map<String, String> getVariableMessages() { return variableMessages; }

    /** 依据 reelCountSpec 随机一次本周期所需的收竿次数 */
    public int rollReelCount() {
        return RandomUtil.parseRange(reelCountSpec);
    }
}
