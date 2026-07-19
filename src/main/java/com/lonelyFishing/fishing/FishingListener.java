package com.lonelyFishing.fishing;

import com.lonelyFishing.config.ConfigManager;
import com.lonelyFishing.config.CustomVariable;
import com.lonelyFishing.config.CustomVariableManager;
import com.lonelyFishing.config.ItemGroupManager;
import com.lonelyFishing.config.LoreBonusManager;
import com.lonelyFishing.config.RodManager;
import com.lonelyFishing.hooks.PlaceholderAPIHook;
import com.lonelyFishing.items.GroupItem;
import com.lonelyFishing.items.ItemGroup;
import com.lonelyFishing.items.ItemProvider;
import com.lonelyFishing.rods.FishingRod;
import com.lonelyFishing.rods.LoreBonus;
import com.lonelyFishing.rods.MultiCatch;
import com.lonelyFishing.util.RandomUtil;
import com.lonelyFishing.util.WeightedSelector;
import org.bukkit.Bukkit;
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
import java.util.Random;
import java.util.UUID;
import java.lang.reflect.Method;

/**
 * 钓鱼事件处理核心:
 *  - FISHING (抛竿): 拦截非自定义鱼竿 (block-vanilla-rods), 自定义鱼竿建立会话
 *  - CAUGHT_FISH (收竿): 计数, 达到所需次数后
 *    1) 按鱼竿基础 catch-rate 与 lore 加成做乘法叠加得到最终钓率, 掷骰判定是否钓上
 *    2) 钓上时按 lore 多钓条数配置掷骰决定一次性钓上几条 (默认 1)
 *    3) 按条数循环: 权重抽奖 + 给物品 + 金币/点券/经验/指令
 */
public class FishingListener implements Listener {

    private final ConfigManager config;
    private final RodManager rodManager;
    private final ItemGroupManager groupManager;
    private final ItemProvider itemProvider;
    private final RewardManager rewards;
    private final LoreBonusManager loreBonuses;
    private final CustomVariableManager customVariables;
    private final PlaceholderAPIHook papi;

    private final Map<UUID, FishingSession> sessions = new HashMap<UUID, FishingSession>();
    private final Map<UUID, Long> noRodCooldown = new HashMap<UUID, Long>();

    public FishingListener(ConfigManager config, RodManager rodManager,
                           ItemGroupManager groupManager, ItemProvider itemProvider,
                           RewardManager rewards, LoreBonusManager loreBonuses,
                           CustomVariableManager customVariables, PlaceholderAPIHook papi) {
        this.config = config;
        this.rodManager = rodManager;
        this.groupManager = groupManager;
        this.itemProvider = itemProvider;
        this.rewards = rewards;
        this.loreBonuses = loreBonuses;
        this.customVariables = customVariables;
        this.papi = papi;
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

        // ---- 抛竿阶段: 拦截非自定义鱼竿 + 重置钓鱼会话 ----
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (rod == null) {
                if (config.blockVanillaRods()) {
                    event.setCancelled(true);
                    sendNoRod(player);
                }
                return;
            }
            // 每次抛竿都是新的钓鱼周期: 进度归零, 重新随机所需收竿次数
            // 保证玩家上一次未完成的拉竿进度不会带到本次钓鱼
            resetSession(player, rod);
            // 抛竿时设置鱼咬钩时间参数 (1.9+ 反射调用 setMinWaitTime 等, 1.8 静默跳过)
            applyFishTiming(event, rod);
            return;
        }

        // ---- 鱼脱钩/跑了 (FAILED_ATTEMPT): 进度归零, 重新开始拉竿周期 ----
        // 场景: 玩家拉了几次竿后鱼跑了, 本次拉竿进度归零, 下次鱼咬钩重新计数
        if (event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            if (rod == null) return;  // 非自定义鱼竿不处理
            resetSession(player, rod);
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
            // 关键: 只在完成钓鱼 (达到收竿次数且判定结束) 时才移除浮标;
            // 未达到次数时保留浮标, 让玩家在本次抛竿中继续等鱼咬钩、再次拉杆, 直到累计达到 reel-count
            if (handleCustomCatch(player, rod, held)) {
                removeHookForcefully(event);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearSession(event.getPlayer().getUniqueId());
    }

    /**
     * 处理一次收竿 (CAUGHT_FISH) 事件。
     * @return true = 本次钓鱼已结束 (达到次数且判定完成), 应移除浮标; false = 未达到次数, 保留浮标让玩家继续拉杆
     */
    private boolean handleCustomCatch(Player player, FishingRod rod, ItemStack held) {
        FishingSession session = ensureSession(player, rod);
        int current = session.increment();

        if (current < session.getRequiredCount()) {
            // 未达到所需次数, 仅提示进度, 不移除浮标 (让玩家继续等鱼咬钩、再次拉杆)
            player.sendMessage(config.format("reel-progress",
                    "progress", String.valueOf(current),
                    "required", String.valueOf(session.getRequiredCount())));
            return false;
        }

        // 达到所需次数: 先按最终钓率掷骰判定是否钓上
        double finalRate = computeCatchRate(rod, held);
        Random rng = RandomUtil.random();
        if (rng.nextDouble() >= finalRate) {
            // 没钓上: 提示 + 重置会话 (重新随机下一次所需次数) + 结束本次钓鱼
            player.sendMessage(config.format("no-catch"));
            session.reset(rod.getId(), rod.rollReelCount());
            return true;
        }

        // 钓上: 判定一次性钓上几条 (默认 1, 命中的 multi-catch 取最大 amount)
        int amount = computeMultiCatchAmount(held, rng);
        for (int i = 0; i < amount; i++) {
            rewardOnce(player, rod);
        }

        // 钓上总提示 (无论条数仅发一次)
        player.sendMessage(config.format("reel-success"));
        sendMultiplierInfo(player, rod);

        // 重置会话, 重新随机下一次所需次数
        session.reset(rod.getId(), rod.rollReelCount());
        return true;  // 完成钓鱼, 移除浮标
    }

    /**
     * 发送倍率提示: 鱼竿自定义 > messages.yml 默认。
     * 若独立三条 (money/points/exp) 任意非空 -> 发独立模式 (跳过 multiplier-info);
     * 否则 -> 发合三为一 multiplier-info。
     * 占位符 {money}{points}{exp}{player}
     */
    private void sendMultiplierInfo(Player player, FishingRod rod) {
        String moneyStr = formatNum(rod.getMoneyMultiplier());
        String pointsStr = formatNum(rod.getPointsMultiplier());
        String expStr = formatNum(rod.getExpMultiplier());

        String info = resolveMultiplierMsg(rod.getMultiplierInfo(), "multiplier-info", moneyStr, pointsStr, expStr, player.getName());
        String money = resolveMultiplierMsg(rod.getMultiplierMoney(), "multiplier-money", moneyStr, pointsStr, expStr, player.getName());
        String points = resolveMultiplierMsg(rod.getMultiplierPoints(), "multiplier-points", moneyStr, pointsStr, expStr, player.getName());
        String exp = resolveMultiplierMsg(rod.getMultiplierExp(), "multiplier-exp", moneyStr, pointsStr, expStr, player.getName());

        if (!money.isEmpty() || !points.isEmpty() || !exp.isEmpty()) {
            if (!money.isEmpty()) player.sendMessage(money);
            if (!points.isEmpty()) player.sendMessage(points);
            if (!exp.isEmpty()) player.sendMessage(exp);
        } else if (!info.isEmpty()) {
            player.sendMessage(info);
        }
    }

    /** 鱼竿模板非空 -> 用鱼竿的; 否则用 messages.yml 默认。都做变量替换 + 上色 */
    private String resolveMultiplierMsg(String rodTemplate, String defaultKey,
                                        String money, String points, String exp, String playerName) {
        String tpl = (rodTemplate != null && !rodTemplate.isEmpty()) ? rodTemplate : config.msg(defaultKey);
        return config.formatTemplate(tpl, "money", money, "points", points, "exp", exp, "player", playerName);
    }

    /**
     * 单次抽奖 + 发奖: 物品 / 金币 / 点券 / 经验 / 指令。
     * 不发 reel-success / multiplier-info (由调用方统一发一次)。
     */
    private void rewardOnce(Player player, FishingRod rod) {
        GroupItem picked = pickItem(rod);
        if (picked == null) {
            player.sendMessage(config.format("no-item-group"));
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

        // 6. 自定义变量 (物品基础值 * 鱼竿倍率, 通过指令增加; PAPI 读取当前值用于消息提示)
        giveCustomVariables(player, rod, picked);
    }

    /**
     * 发放自定义变量奖励: 遍历物品配置的 variables, 按 (基础值 * 鱼竿倍率) 计算最终增加量,
     * 执行变量的 add-command 增加变量值, 并发送消息提示。
     * - 倍率: 鱼竿 variable-multipliers[varName] > 1.0
     * - 消息: 鱼竿 variable-messages[varName] > config.yml custom-variables[varName].message
     * - 占位符: {amount}(最终增加量) {base}(基础值) {multiplier}(倍率) {current}(当前值, PAPI 读取) {player}
     * - PAPI 未安装时 {current} 为空, 变量增加指令仍会执行
     */
    private void giveCustomVariables(Player player, FishingRod rod, GroupItem picked) {
        Map<String, Double> itemVars = picked.getVariables();
        if (itemVars == null || itemVars.isEmpty()) return;
        Map<String, Double> rodMul = rod.getVariableMultipliers();
        Map<String, String> rodMsg = rod.getVariableMessages();
        for (Map.Entry<String, Double> e : itemVars.entrySet()) {
            String varName = e.getKey();
            double base = e.getValue();
            CustomVariable cv = customVariables == null ? null : customVariables.get(varName);
            if (cv == null) continue;  // config.yml 未配置此变量, 跳过
            double mul = (rodMul != null && rodMul.containsKey(varName)) ? rodMul.get(varName) : 1.0;
            double amount = base * mul;
            if (amount <= 0) continue;
            // 执行增加变量的指令 (替换 {amount} 和 %player_name%)
            String cmd = cv.getAddCommand();
            if (cmd != null && !cmd.isEmpty()) {
                String actual = cmd.replace("{amount}", formatNum(amount))
                                   .replace("%player_name%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), actual);
            }
            // 发送提示消息 (鱼竿覆盖 > config.yml 默认; 替换占位符 + 上色)
            String tpl = (rodMsg != null && rodMsg.containsKey(varName) && rodMsg.get(varName) != null)
                    ? rodMsg.get(varName) : cv.getMessage();
            if (tpl != null && !tpl.isEmpty()) {
                String current = "";
                if (papi != null && papi.isEnabled() && !cv.getPlaceholder().isEmpty()) {
                    current = papi.setPlaceholders(player, cv.getPlaceholder());
                }
                String formatted = config.formatTemplate(tpl,
                        "amount", formatNum(amount),
                        "base", formatNum(base),
                        "multiplier", formatNum(mul),
                        "current", current,
                        "player", player.getName());
                player.sendMessage(formatted);
            }
        }
    }

    /** 计算最终钓率: 基础 * Π(1 + bonus), 限制在 [0,1] */
    private double computeCatchRate(FishingRod rod, ItemStack held) {
        double rate = rod.getCatchRate();
        for (LoreBonus b : loreBonuses.getBonuses(held)) {
            rate *= (1.0 + b.getCatchRateBonus());
        }
        if (rate < 0.0) rate = 0.0;
        if (rate > 1.0) rate = 1.0;
        return rate;
    }

    /** 计算一次性钓上几条: 默认 1, 遍历命中 LoreBonus 的 multi-catch, 命中的取 amount 最大者 */
    private int computeMultiCatchAmount(ItemStack held, Random rng) {
        int amount = 1;
        for (LoreBonus b : loreBonuses.getBonuses(held)) {
            for (MultiCatch mc : b.getMultiCatch()) {
                if (rng.nextDouble() < mc.getChance()) {
                    if (mc.getAmount() > amount) amount = mc.getAmount();
                }
            }
        }
        return amount;
    }

    /** 通过反射获取 FishHook 实体, 兼容 1.8 (Fish) / 1.9+ (FishHook) 接口差异 */
    private Object getHook(PlayerFishEvent event) {
        try {
            return PlayerFishEvent.class.getMethod("getHook").invoke(event);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 设置鱼咬钩时间参数:
     *  - wait-time: 鱼咬钩前的等待时间 (减小=加快钓鱼速度)
     *  - lure-time: 鱼咬钩后玩家拉杆的时间窗口 (增大=有更多时间拉杆)
     * 仅 1.9+ 服务端的 FishHook 提供 setMinWaitTime/setMaxWaitTime/setMinLureTime/setMaxLureTime,
     * 1.8 反射找不到这些方法会静默跳过 (1.8 仅支持 setBiteChance, 此处不降级)。
     */
    private void applyFishTiming(PlayerFishEvent event, FishingRod rod) {
        Object hook = getHook(event);
        if (hook == null) return;
        int[] wait = RandomUtil.parseRangePair(rod.getWaitTimeSpec());
        int[] lure = RandomUtil.parseRangePair(rod.getLureTimeSpec());
        if (wait != null) {
            int lo = Math.max(0, wait[0]);
            int hi = Math.max(lo, wait[1]);
            invokeIntSetter(hook, "setMinWaitTime", lo);
            invokeIntSetter(hook, "setMaxWaitTime", hi);
        }
        if (lure != null) {
            int lo = Math.max(0, lure[0]);
            int hi = Math.max(lo, lure[1]);
            invokeIntSetter(hook, "setMinLureTime", lo);
            invokeIntSetter(hook, "setMaxLureTime", hi);
        }
    }

    private void invokeIntSetter(Object hook, String method, int value) {
        try {
            Method m = hook.getClass().getMethod(method, int.class);
            m.invoke(hook, value);
        } catch (Throwable t) {
            // 1.8 没有这些方法, 静默跳过
        }
    }

    /** 强制移除钓鱼浮标, 让玩家必须重新抛竿 (防止一次抛竿重复触发 CAUGHT_FISH 刷次数) */
    private void removeHookForcefully(PlayerFishEvent event) {
        Object hook = getHook(event);
        if (hook instanceof org.bukkit.entity.Entity) {
            ((org.bukkit.entity.Entity) hook).remove();
        }
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

    /**
     * 强制重置玩家钓鱼会话: 进度归零 + 重新随机所需收竿次数。
     * 在抛竿 (FISHING) 或鱼跑了 (FAILED_ATTEMPT) 时调用, 保证每次钓鱼周期独立,
     * 之前的拉竿进度不会带到新一轮钓鱼。
     */
    private void resetSession(Player player, FishingRod rod) {
        UUID uuid = player.getUniqueId();
        FishingSession session = sessions.get(uuid);
        if (session == null || !rod.getId().equals(session.getRodId())) {
            sessions.put(uuid, new FishingSession(rod.getId(), rod.rollReelCount()));
        } else {
            session.reset(rod.getId(), rod.rollReelCount());
        }
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
