package com.lonelyFishing.rods;

import java.util.Collections;
import java.util.List;

/**
 * 鱼竿 lore 加成配置:
 *  - lore             : 触发加成所需的 lore 子串 (匹配前会对双方都 stripColor)
 *  - catchRateBonus   : 钓上鱼概率加成, 0.1 表示 +10%。
 *                       计算方式: 最终概率 = 基础概率 * (1 + bonus)
 *                       例: 基础 0.8 + 加成 0.1 -> 0.8 * 1.1 = 0.88
 *  - multiCatch       : 命中后按概率提升一次性钓上的条数 (取命中条目最大 amount)
 */
public class LoreBonus {

    private final String lore;
    private final double catchRateBonus;
    private final List<MultiCatch> multiCatch;

    public LoreBonus(String lore, double catchRateBonus, List<MultiCatch> multiCatch) {
        this.lore = lore;
        this.catchRateBonus = catchRateBonus;
        this.multiCatch = multiCatch == null ? Collections.<MultiCatch>emptyList() : multiCatch;
    }

    public String getLore() { return lore; }
    public double getCatchRateBonus() { return catchRateBonus; }
    public List<MultiCatch> getMultiCatch() { return multiCatch; }
}
