package com.lonelyFishing.fishing;

/**
 * 玩家钓鱼会话: 记录当前使用的鱼竿、当前已收竿次数、本周期所需次数。
 * 每个玩家一份 (per-player)。达到所需次数后给奖并重新随机下一次所需次数。
 */
public class FishingSession {

    private String rodId;
    private int currentCount;
    private int requiredCount;

    public FishingSession(String rodId, int requiredCount) {
        this.rodId = rodId;
        this.requiredCount = requiredCount;
        this.currentCount = 0;
    }

    public String getRodId() { return rodId; }
    public int getCurrentCount() { return currentCount; }
    public int getRequiredCount() { return requiredCount; }

    /** 收竿一次, 返回最新计数 */
    public int increment() {
        return ++currentCount;
    }

    /** 重置 (给奖后调用), 重新随机本周期所需次数 */
    public void reset(String rodId, int requiredCount) {
        this.rodId = rodId;
        this.requiredCount = requiredCount;
        this.currentCount = 0;
    }
}
