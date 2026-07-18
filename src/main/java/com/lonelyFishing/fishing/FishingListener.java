package com.lonelyFishing.fishing;

import com.lonelyFishing.config.ConfigManager;
import com.lonelyFishing.config.ItemGroupManager;
import com.lonelyFishing.config.RodManager;
import com.lonelyFishing.items.GroupItem;
import com.lonelyFishing.items.ItemGroup;
import com.lonelyFishing.items.ItemProvider;
import com.lonelyFishing.rods.FishingRod;
import com.lonelyFishing.util.RandomUtil;
import com.lonelyFishing.util.WeightedSelector;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 钓鱼事件处理核心:
 *  - FISHING (抛竿): 拦截非自定义鱼竿 (block-vanilla-rods), 自定义鱼竿建立会话
 *  - CAUGHT_FISH (收竿): 计数, 达到所需次数后按权重抽奖、给予物品/金币/点券/经验/指令
 */
public class FishingListener implements Listener {

    private final ConfigManager config;
    private final RodManager rodManager;
    private final ItemGroupManager groupManager;
    private final ItemProvider itemProvider;
    private final RewardManager rewards;

    private final Map<UUID, FishingSession> sessions = new HashMap<UUID, FishingSession>();
    private final Map<UUID, Long> noRodCooldown = new HashMap<UUID, Long>();

    public FishingListener(ConfigManager config, RodManager rodManager,
                           ItemGroupManager groupManager, ItemProvider itemProvider,
                           RewardManager rewards) {
        this.config = config;
        this.rodManager = rodManager;
        this.groupManager = groupManager;
        this.itemProvider = itemProvider;
        this.rewards = rewards;
    }

    public void clearSession(UUID uuid) {
        sessions.remove(uuid);
        noRodCooldown.remove(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack held = getItemInHand(player);
        // 无 use 权限的玩家不享受自定义鱼竿 (等价于手中无自定义鱼竿)
        FishingRod rod = hasUse(player) ? rodManager.match(held) : null;

        // ---- 抛竿阶段: 拦截非自定义鱼竿 ----
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (rod == null) {
                if (config.blockVanillaRods()) {
                    event.setCancelled(true);
                    sendNoRod(player);
                }
                return;
            }
            ensureSession(player, rod);
            return;
        }

        // ---- 收竿钓上鱼阶段 ----
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (rod == null) {
                // 非自定义鱼竿: 走原版 (block-vanilla-rods=false 才会到这)
                return;
            }
            // 物品组完全替换原版钓获物: 取消原版掉落 + 移除原版鱼实体 + 清零原版经验
            event.setCancelled(true);
            event.setExpToDrop(0);
            org.bukkit.entity.Entity caught = event.getCaught();
            if (caught instanceof org.bukkit.entity.Item) {
                ((org.bukkit.entity.Item) caught).remove();
            }
            handleCustomCatch(player, rod);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearSession(event.getPlayer().getUniqueId());
    }

    private void handleCustomCatch(Player player, FishingRod rod) {
        FishingSession session = ensureSession(player, rod);
        int current = session.increment();

        if (current < session.getRequiredCount()) {
            // 未达到所需次数, 仅提示进度
            player.sendMessage(config.format("reel-progress",
                    "progress", String.valueOf(current),
                    "required", String.valueOf(session.getRequiredCount())));
            return;
        }

        // 达到所需次数: 抽奖 + 发奖
        GroupItem picked = pickItem(rod);
        if (picked == null) {
            player.sendMessage(config.format("no-item-group"));
            session.reset(rod.getId(), rod.rollReelCount());
            return;
        }

        // 1. 物品
        ItemStack reward = itemProvider.getItemStack(picked.getSource(), picked.getItemId(), player);
        if (reward == null) {
            player.sendMessage(config.format("item-not-found",
                    "item-id", picked.getItemId(),
                    "source", picked.getSource()));
        } else {
            giveItem(player, reward);
        }

        // 2. 金币 (物品配置 * 鱼竿倍率)
        Double money = picked.getMoney();
        if (money != null) {
            double amount = money * rod.getMoneyMultiplier();
            if (rewards.giveMoney(player, amount)) {
                player.sendMessage(config.format("reward-money", "amount", formatNum(amount)));
            } else if (!rewards.economyEnabled()) {
                player.sendMessage(config.msg("economy-missing"));
            }
        }

        // 3. 点券
        Integer points = picked.getPoints();
        if (points != null) {
            int amount = (int) Math.round(points * rod.getPointsMultiplier());
            if (rewards.givePoints(player, amount)) {
                player.sendMessage(config.format("reward-points", "amount", String.valueOf(amount)));
            } else if (!rewards.pointsEnabled()) {
                player.sendMessage(config.msg("points-missing"));
            }
        }

        // 4. 经验
        Integer exp = picked.getExp();
        if (exp != null) {
            int amount = (int) Math.round(exp * rod.getExpMultiplier());
            rewards.giveExp(player, amount);
            player.sendMessage(config.format("reward-exp", "amount", String.valueOf(amount)));
        }

        // 5. 指令 (物品自身 + 鱼竿)
        rewards.executeCommands(player, picked.getCommands());
        rewards.executeCommands(player, rod.getCommands());

        // 6. 成功 + 倍率提示
        player.sendMessage(config.format("reel-success"));
        player.sendMessage(config.format("multiplier-info",
                "money", formatNum(rod.getMoneyMultiplier()),
                "points", formatNum(rod.getPointsMultiplier()),
                "exp", formatNum(rod.getExpMultiplier())));

        // 7. 重置会话, 重新随机下一次所需次数
        session.reset(rod.getId(), rod.rollReelCount());
    }

    /** 把鱼竿所列物品组的全部物品汇总成一个权重池抽奖 */
    private GroupItem pickItem(FishingRod rod) {
        List<GroupItem> pool = new ArrayList<GroupItem>();
        for (String gid : rod.getItemGroupIds()) {
            ItemGroup group = groupManager.getGroup(gid);
            if (group != null) pool.addAll(group.getItems());
        }
        if (pool.isEmpty()) return null;
        return WeightedSelector.select(pool, RandomUtil.random());
    }

    private FishingSession ensureSession(Player player, FishingRod rod) {
        UUID uuid = player.getUniqueId();
        FishingSession session = sessions.get(uuid);
        if (session == null || !rod.getId().equals(session.getRodId())) {
            session = new FishingSession(rod.getId(), rod.rollReelCount());
            sessions.put(uuid, session);
        }
        return session;
    }

    private void giveItem(Player player, ItemStack item) {
        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> overflow = inv.addItem(item);
        if (!overflow.isEmpty() && config.dropWhenFull()) {
            Location loc = player.getLocation();
            for (ItemStack extra : overflow.values()) {
                if (extra != null && extra.getAmount() > 0) {
                    player.getWorld().dropItemNaturally(loc, extra);
                }
            }
        }
    }

    private void sendNoRod(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = noRodCooldown.get(uuid);
        int cd = config.messageCooldown() * 1000;
        if (last != null && now - last < cd) return;
        noRodCooldown.put(uuid, now);
        player.sendMessage(config.format("no-rod"));
    }

    private boolean hasUse(Player player) {
        return player.hasPermission("lonelyfishing.use");
    }

    private ItemStack getItemInHand(Player player) {
        try {
            return player.getInventory().getItemInHand();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 整数去掉小数点 */
    private String formatNum(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
