package com.lonelyFishing.rods;

/**
 * 多钓条数配置: 当对应 lore 命中时, 以 chance 的概率一次性钓到 amount 条鱼。
 * 多个命中的条目之间取 amount 最大者 (默认 1 条)。
 */
public class MultiCatch {

    private final double chance;  // 命中概率 (0-1)
    private final int amount;     // 一次性钓上的条数

    public MultiCatch(double chance, int amount) {
        this.chance = chance;
        this.amount = amount;
    }

    public double getChance() { return chance; }
    public int getAmount() { return amount; }
}
