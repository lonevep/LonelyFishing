package com.lonelyFishing.util;

import java.util.List;
import java.util.Random;

/**
 * 权重抽奖工具
 */
public final class WeightedSelector {

    public interface Weighted {
        double getWeight();
    }

    private WeightedSelector() {}

    /**
     * 从带权重的列表中按权重随机选一个。权重需 > 0。
     * 全部权重非正时返回 null。
     */
    public static <T extends Weighted> T select(List<T> items, Random random) {
        if (items == null || items.isEmpty()) return null;
        double total = 0;
        for (T item : items) {
            if (item.getWeight() > 0) total += item.getWeight();
        }
        if (total <= 0) return null;
        double r = random.nextDouble() * total;
        double acc = 0;
        T last = null;
        for (T item : items) {
            if (item.getWeight() <= 0) continue;
            last = item;
            acc += item.getWeight();
            if (r < acc) return item;
        }
        return last;
    }
}
