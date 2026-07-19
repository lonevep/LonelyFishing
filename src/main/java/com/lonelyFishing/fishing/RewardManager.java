package com.lonelyFishing.fishing;

import com.lonelyFishing.hooks.EconomyHook;
import com.lonelyFishing.hooks.PointsHook;
import com.lonelyFishing.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 奖励发放: 金币(Vault) / 点券(PlayerPoints) / 经验 / 指令
 * 指令前缀 (大小写不敏感):
 *   [OP]        以 OP 玩家身份执行后续指令
 *   [CONSOLE]   以控制台身份执行后续指令
 *   [BroadCast] 向全服公告后续文本 (按颜色码上色, 非指令)
 *   [tell]      仅向钓上鱼的玩家发送后续文本 (按颜色码上色, 非指令)
 *   (无前缀)    默认以控制台执行
 * 所有前缀均支持 %player_name% 占位符替换
 */
public class RewardManager {

    private final EconomyHook economy;
    private final PointsHook points;

    public RewardManager(EconomyHook economy, PointsHook points) {
        this.economy = economy;
        this.points = points;
    }

    public boolean economyEnabled() {
        return economy != null && economy.isEnabled();
    }

    public boolean pointsEnabled() {
        return points != null && points.isEnabled();
    }

    public boolean giveMoney(Player p, double amount) {
        if (amount <= 0) return false;
        if (!economyEnabled()) return false;
        return economy.deposit(p, amount);
    }

    public boolean givePoints(Player p, int amount) {
        if (amount <= 0) return false;
        if (!pointsEnabled()) return false;
        return points.give(p, amount);
    }

    public void giveExp(Player p, int amount) {
        if (amount <= 0) return;
        p.giveExp(amount);
    }

    /**
     * 执行指令列表, 支持 [OP] / [CONSOLE] / [BroadCast] / [tell] 前缀, 支持 %player_name%。
     * [BroadCast] 与 [tell] 是消息发送 (会按 & 颜色码上色), 其余前缀是命令执行 (原样下发)。
     */
    public void executeCommands(Player p, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = raw.replace("%player_name%", p.getName());
            String trimmed = cmd.trim();
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("[OP]")) {
                String actual = trimmed.substring(trimmed.indexOf(']') + 1).trim();
                runAsOp(p, actual);
            } else if (upper.startsWith("[CONSOLE]")) {
                String actual = trimmed.substring(trimmed.indexOf(']') + 1).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), actual);
            } else if (upper.startsWith("[BROADCAST]")) {
                // 全服公告: 文本上色后广播, 而非作为指令下发
                String msg = trimmed.substring(trimmed.indexOf(']') + 1).trim();
                if (!msg.isEmpty()) Bukkit.broadcastMessage(ColorUtil.color(msg));
            } else if (upper.startsWith("[TELL]")) {
                // 仅发给钓上鱼的玩家: 文本上色后私发
                String msg = trimmed.substring(trimmed.indexOf(']') + 1).trim();
                if (!msg.isEmpty()) p.sendMessage(ColorUtil.color(msg));
            } else {
                // 未声明前缀, 默认以控制台执行
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
            }
        }
    }

    /** 临时赋予 OP 身份执行指令, 执行完毕恢复 (try/finally 保证恢复) */
    private void runAsOp(Player p, String command) {
        if (command.isEmpty()) return;
        boolean wasOp = p.isOp();
        try {
            if (!wasOp) p.setOp(true);
            Bukkit.dispatchCommand(p, command);
        } finally {
            if (!wasOp) p.setOp(false);
        }
    }
}
