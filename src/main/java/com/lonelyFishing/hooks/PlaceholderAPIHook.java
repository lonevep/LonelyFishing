package com.lonelyFishing.hooks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * PlaceholderAPI 对接 (反射方式, 不强制编译依赖)。
 * 用于读取自定义变量的当前值 (如 %ll_level-默认%)。
 * PAPI 本身只读, 不能直接修改变量值; 变量值的增加由变量所属插件的指令完成
 * (本插件通过 CustomVariable.addCommand 模板下发指令)。
 * PAPI 全版本 (1.8-最新) 兼容。
 */
public class PlaceholderAPIHook {

    private final Logger logger;
    private boolean enabled = false;
    private Method setPlaceholdersMethod;  // setPlaceholders(OfflinePlayer, String) 静态方法

    public PlaceholderAPIHook(Logger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void init() {
        enabled = false;
        setPlaceholdersMethod = null;
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return;
        }
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            // 查找 setPlaceholders(OfflinePlayer, String) 静态方法 (PAPI 2.x 签名稳定)
            for (Method m : papiClass.getMethods()) {
                if (!"setPlaceholders".equals(m.getName())) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2
                        && (params[0] == OfflinePlayer.class || params[0] == org.bukkit.entity.Player.class)
                        && params[1] == String.class) {
                    setPlaceholdersMethod = m;
                    break;
                }
            }
            if (setPlaceholdersMethod == null) {
                logger.warning("[LonelyFishing] PlaceholderAPI setPlaceholders 方法未找到");
                return;
            }
            enabled = true;
            logger.info("[LonelyFishing] 已对接 PlaceholderAPI: " + papi.getDescription().getVersion());
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] PlaceholderAPI 对接失败: " + t.getMessage());
        }
    }

    /**
     * 替换字符串中的 PAPI 占位符 (如 %ll_level-默认% -> 当前值)。
     * PAPI 未安装或对接失败时原样返回 (不抛异常)。
     */
    public String setPlaceholders(OfflinePlayer player, String text) {
        if (!enabled || text == null || text.isEmpty()) return text;
        try {
            Object result = setPlaceholdersMethod.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (Throwable t) {
            return text;
        }
    }
}
