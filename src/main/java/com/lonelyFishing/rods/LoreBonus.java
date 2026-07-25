package com.lonelyFishing.rods;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 鱼竿 lore 加成配置:
 *  - lore             : 触发加成所需的 lore 子串 (匹配前会对双方都 stripColor)
 *  - catchRateBonus   : 钓上鱼概率加成, 0.1 表示 +10%。
 *                       计算方式: 最终概率 = 基础概率 * (1 + bonus), 多个命中条目做乘法叠加
 *                       例: 基础 0.8 + 加成 0.1 -> 0.8 * 1.1 = 0.88, 上限 1.0 (100%)
 *  - waitTimeBonus    : 加快钓到鱼速度, 0.1 表示加快 10%。
 *                       计算方式: 实际等待 = 鱼竿 wait-time * (1 - bonus), 多个条目加法累加后一次乘
 *                       例: wait-time 100 ticks + 加成 0.1 -> 100 * 0.9 = 90 ticks
 *  - lureTimeBonus    : 增加拉竿窗口时间, 0.1 表示 +10%。
 *                       计算方式: 实际窗口 = 鱼竿 lure-time * (1 + bonus), 多个条目加法累加后一次乘
 *                       例: lure-time 200 ticks + 加成 0.1 -> 200 * 1.1 = 220 ticks
 *  - moneyBonus       : 金币获取倍率加成, 0.1 表示 +10%。
 *                       计算方式: 最终倍率 = 鱼竿 money-multiplier * (1 + bonus), 多个条目加法累加
 *  - pointsBonus      : 点券获取倍率加成, 同上
 *  - expBonus         : 经验获取倍率加成, 同上
 *  - variableBonuses  : 自定义变量倍率加成 (变量名 -> 加成百分比), 0.1 表示 +10%。
 *                       计算方式: 最终倍率 = 鱼竿 variable-multipliers[变量] * (1 + bonus)
 *  - multiCatch       : 命中后按概率提升一次性钓上的条数 (取命中条目最大 amount)
 */
public class LoreBonus {

    private final String lore;
    private final double catchRateBonus;
    private final double waitTimeBonus;
    private final double lureTimeBonus;
    private final double moneyBonus;
    private final double pointsBonus;
    private final double expBonus;
    private final Map<String, Double> variableBonuses;
    private final List<MultiCatch> multiCatch;

    public LoreBonus(String lore, double catchRateBonus,
                     double waitTimeBonus, double lureTimeBonus,
                     double moneyBonus, double pointsBonus, double expBonus,
                     Map<String, Double> variableBonuses,
                     List<MultiCatch> multiCatch) {
        this.lore = lore;
        this.catchRateBonus = catchRateBonus;
        this.waitTimeBonus = waitTimeBonus;
        this.lureTimeBonus = lureTimeBonus;
        this.moneyBonus = moneyBonus;
        this.pointsBonus = pointsBonus;
        this.expBonus = expBonus;
        this.variableBonuses = variableBonuses == null
                ? Collections.<String, Double>emptyMap() : variableBonuses;
        this.multiCatch = multiCatch == null ? Collections.<MultiCatch>emptyList() : multiCatch;
    }

    public String getLore() { return lore; }
    public double getCatchRateBonus() { return catchRateBonus; }
    public double getWaitTimeBonus() { return waitTimeBonus; }
    public double getLureTimeBonus() { return lureTimeBonus; }
    public double getMoneyBonus() { return moneyBonus; }
    public double getPointsBonus() { return pointsBonus; }
    public double getExpBonus() { return expBonus; }
    public Map<String, Double> getVariableBonuses() { return variableBonuses; }
    public List<MultiCatch> getMultiCatch() { return multiCatch; }
}
