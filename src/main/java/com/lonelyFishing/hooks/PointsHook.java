package com.lonelyFishing.hooks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * PlayerPoints 点券对接 (反射方式, 不强制编译依赖)。
 */
public class PointsHook {

    private final Logger logger;
    private boolean enabled = false;
    private Object api;          // PlayerPointsAPI
    private Method giveMethod;   // give(UUID, int) 或 give(String, int)

    public PointsHook(Logger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void init() {
        enabled = false;
        api = null;
        giveMethod = null;
        Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pp == null || !pp.isEnabled()) {
            return;
        }
        try {
            Method getAPI = pp.getClass().getMethod("getAPI");
            api = getAPI.invoke(pp);
            if (api == null) return;
            // give(UUID, int)
            try {
                giveMethod = api.getClass().getMethod("give", UUID.class, int.class);
            } catch (NoSuchMethodException e) {
                try {
                    giveMethod = api.getClass().getMethod("give", String.class, int.class);
                } catch (NoSuchMethodException e2) {
                    logger.warning("[LonelyFishing] PlayerPoints give 方法未找到");
                    return;
                }
            }
            enabled = true;
            logger.info("[LonelyFishing] 已对接 PlayerPoints: " + pp.getDescription().getVersion());
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] PlayerPoints 对接失败: " + t.getMessage());
        }
    }

    /** 给予点券。成功返回 true。 */
    public boolean give(OfflinePlayer player, int amount) {
        if (!enabled || player == null || amount <= 0 || giveMethod == null) return false;
        try {
            Class<?>[] params = giveMethod.getParameterTypes();
            Object arg;
            if (params[0] == String.class) {
                arg = player.getName();
            } else {
                arg = player.getUniqueId();
            }
            giveMethod.invoke(api, arg, amount);
            return true;
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] PlayerPoints 给予点券失败: " + t.getMessage());
            return false;
        }
    }
}
