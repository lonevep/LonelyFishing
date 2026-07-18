package com.lonelyFishing.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * NeigeItems 物品库对接 (反射方式, 不强制编译依赖)。
 *
 * Kotlin 版 NeigeItems:
 *   pers.neige.neigeitems.manager.ItemManager.INSTANCE.getItemStack(id, player) -> ItemStack
 *   pers.neige.neigeitems.utils.ItemUtils.getItemId(item) -> String  (非NI物品返回null)
 */
public class NeigeItemsHook {

    private final Logger logger;
    private boolean enabled = false;

    private Object itemManagerInstance;     // ItemManager.INSTANCE
    private Method getItemStackWithPlayer;  // getItemStack(String, Player)
    private Method getItemStackNoPlayer;    // getItemStack(String)

    private Object itemUtilsInstance;       // ItemUtils.INSTANCE (若为 object)
    private Method getItemId;               // getItemId(ItemStack)

    public NeigeItemsHook(Logger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void init() {
        enabled = false;
        itemManagerInstance = null;
        getItemStackWithPlayer = null;
        getItemStackNoPlayer = null;
        itemUtilsInstance = null;
        getItemId = null;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NeigeItems");
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            Class<?> itemManagerClass = Class.forName("pers.neige.neigeitems.manager.ItemManager");
            // Kotlin object 单例: 取 INSTANCE 静态字段
            try {
                itemManagerInstance = itemManagerClass.getField("INSTANCE").get(null);
            } catch (NoSuchFieldException e) {
                // 非 object, 直接用类当实例 (静态方法)
                itemManagerInstance = null;
            }
            // 优先 getItemStack(String, Player)
            try {
                getItemStackWithPlayer = itemManagerClass.getMethod("getItemStack", String.class, Player.class);
            } catch (NoSuchMethodException e) {
                getItemStackWithPlayer = null;
            }
            try {
                getItemStackNoPlayer = itemManagerClass.getMethod("getItemStack", String.class);
            } catch (NoSuchMethodException e) {
                getItemStackNoPlayer = null;
            }

            Class<?> itemUtilsClass = Class.forName("pers.neige.neigeitems.utils.ItemUtils");
            try {
                getItemId = itemUtilsClass.getMethod("getItemId", ItemStack.class);
            } catch (NoSuchMethodException e) {
                getItemId = null;
            }
            // 若 getItemId 是实例方法 (object), 取 INSTANCE
            if (getItemId == null) {
                try {
                    itemUtilsInstance = itemUtilsClass.getField("INSTANCE").get(null);
                    getItemId = itemUtilsClass.getMethod("getItemId", ItemStack.class);
                } catch (Throwable ignored) {}
            }

            if (getItemStackWithPlayer == null && getItemStackNoPlayer == null) {
                logger.warning("[LonelyFishing] NeigeItems 物品方法未找到, 对接失败");
                return;
            }
            enabled = true;
            logger.info("[LonelyFishing] 已对接 NeigeItems: " + plugin.getDescription().getVersion());
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] NeigeItems 对接失败: " + t.getMessage());
        }
    }

    /**
     * 从物品库生成一个物品。失败返回 null。
     * 部分版本返回 List<ItemStack>, 此时取第一个。
     */
    public ItemStack getItemStack(String id, Player player) {
        if (!enabled || id == null) return null;
        try {
            Object result;
            if (getItemStackWithPlayer != null) {
                result = getItemStackWithPlayer.invoke(itemManagerInstance, id, player);
            } else {
                result = getItemStackNoPlayer.invoke(itemManagerInstance, id);
            }
            if (result == null) return null;
            if (result instanceof ItemStack) return (ItemStack) result;
            if (result instanceof List) {
                List<?> list = (List<?>) result;
                if (list.isEmpty()) return null;
                Object first = list.get(0);
                return (first instanceof ItemStack) ? (ItemStack) first : null;
            }
            return null;
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] NeigeItems 获取物品失败 [" + id + "]: " + t.getMessage());
            return null;
        }
    }

    /**
     * 识别物品对应的 NeigeItems 物品 ID。非 NI 物品返回 null。
     */
    public String getItemId(ItemStack item) {
        if (!enabled || item == null || getItemId == null) return null;
        try {
            Object result;
            if (itemUtilsInstance != null) {
                result = getItemId.invoke(itemUtilsInstance, item);
            } else {
                result = getItemId.invoke(null, item);
            }
            return result == null ? null : result.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
