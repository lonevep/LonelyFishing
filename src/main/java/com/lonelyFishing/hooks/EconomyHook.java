package com.lonelyFishing.hooks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Vault 经济对接 (反射方式, 不强制编译依赖)。
 */
public class EconomyHook {

    private final Logger logger;
    private boolean enabled = false;
    private Object economy;          // net.milkbowl.vault.economy.Economy
    private Method depositMethod;    // depositPlayer(OfflinePlayer, double) 或兼容重载

    public EconomyHook(Logger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void init() {
        enabled = false;
        economy = null;
        depositMethod = null;
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object servicesManager = Bukkit.getServicesManager();
            Method getRegistration = servicesManager.getClass().getMethod("getRegistration", Class.class);
            Object registration = getRegistration.invoke(servicesManager, economyClass);
            if (registration == null) {
                logger.info("[LonelyFishing] Vault 已安装但未找到经济 Provider");
                return;
            }
            // RegisteredServiceProvider.getProvider()
            // 直接强转 (Vault 为软依赖, 运行期类存在则可; 否则反射)
            if (registration instanceof RegisteredServiceProvider) {
                RegisteredServiceProvider rsp = (RegisteredServiceProvider) registration;
                economy = rsp.getProvider();
            } else {
                economy = registration.getClass().getMethod("getProvider").invoke(registration);
            }
            if (economy == null) return;

            // 查找 depositPlayer 重载
            depositMethod = findDeposit(economy.getClass());
            if (depositMethod == null) {
                logger.warning("[LonelyFishing] Vault depositPlayer 方法未找到");
                return;
            }
            enabled = true;
            logger.info("[LonelyFishing] 已对接 Vault 经济: " + vault.getDescription().getVersion());
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] Vault 对接失败: " + t.getMessage());
        }
    }

    private Method findDeposit(Class<?> clazz) {
        // 优先 OfflinePlayer, double
        try {
            return clazz.getMethod("depositPlayer", OfflinePlayer.class, double.class);
        } catch (NoSuchMethodException ignored) {}
        try {
            return clazz.getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
        } catch (NoSuchMethodException ignored) {}
        try {
            return clazz.getMethod("depositPlayer", String.class, double.class);
        } catch (NoSuchMethodException ignored) {}
        return null;
    }

    /** 存款。成功返回 true。 */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled || player == null || amount <= 0 || depositMethod == null) return false;
        try {
            Class<?>[] params = depositMethod.getParameterTypes();
            Object arg;
            if (params[0] == String.class) {
                arg = player.getName();
            } else {
                arg = player;
            }
            depositMethod.invoke(economy, arg, amount);
            return true;
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] Vault 存款失败: " + t.getMessage());
            return false;
        }
    }
}
