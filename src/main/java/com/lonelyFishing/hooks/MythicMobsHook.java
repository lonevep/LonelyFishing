package com.lonelyFishing.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * MythicMobs 物品库对接 (反射方式, 兼容 v4 / v5, 不强制编译依赖)。
 *
 * v5: io.lumine.mythic.bukkit.MythicBukkit.inst().getItemManager()
 *      - getItem(String) -> Optional<MythicItem>
 *      - MythicItem.generateItemStack(int) -> AbstractItemStack (运行期为 BukkitItemStack)
 *      - BukkitItemStack.build() -> org.bukkit.inventory.ItemStack
 *      - getMythicTypeFromItem(ItemStack) -> String (内部名, 非MM物品返回null)
 * v4: io.lumine.xikage.mythicmobs.MythicMobs.inst().getItemManager()
 *      - getItemStack(String) -> ItemStack
 */
public class MythicMobsHook {

    private final Logger logger;
    private boolean enabled = false;

    // 反射缓存
    private Object itemManager;
    private Method getItemOpt;       // Optional<MythicItem> getItem(String)
    private Method optIsPresent;
    private Method optGet;
    private Method generateItemStack;// AbstractItemStack generateItemStack(int)
    private Method buildItem;        // ItemStack build()  (v5)
    private Method getMythicType;    // String getMythicTypeFromItem(ItemStack) (v5)

    // v4 路径
    private Method v4GetItemStack;   // ItemStack getItemStack(String)

    public MythicMobsHook(Logger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void init() {
        enabled = false;
        itemManager = null;
        getItemOpt = null;
        optIsPresent = null;
        optGet = null;
        generateItemStack = null;
        buildItem = null;
        getMythicType = null;
        v4GetItemStack = null;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            // 优先尝试 v5
            if (tryV5()) {
                enabled = true;
                logger.info("[LonelyFishing] 已对接 MythicMobs v5: " + plugin.getDescription().getVersion());
                return;
            }
        } catch (Throwable ignored) {}
        try {
            if (tryV4()) {
                enabled = true;
                logger.info("[LonelyFishing] 已对接 MythicMobs v4: " + plugin.getDescription().getVersion());
            }
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] MythicMobs 对接失败: " + t.getMessage());
        }
    }

    private boolean tryV5() throws Throwable {
        Class<?> mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
        Method inst = mythicBukkit.getMethod("inst");
        Object core = inst.invoke(null);
        Method getItemManager = core.getClass().getMethod("getItemManager");
        itemManager = getItemManager.invoke(core);

        getItemOpt = itemManager.getClass().getMethod("getItem", String.class);
        Class<?> optClass = getItemOpt.getReturnType();
        optIsPresent = optClass.getMethod("isPresent");
        optGet = optClass.getMethod("get");

        // generateItemStack(int) 位于 MythicItem 上
        generateItemStack = null;
        Class<?> mythicItemClass = Class.forName("io.lumine.mythic.core.items.MythicItem");
        for (Method m : mythicItemClass.getMethods()) {
            if (m.getName().equals("generateItemStack") && m.getParameterTypes().length == 1
                    && m.getParameterTypes()[0] == int.class) {
                generateItemStack = m;
                break;
            }
        }
        if (generateItemStack == null) return false;

        // build() 存在于 AbstractItemStack 实现类 (BukkitItemStack 继承 ItemFactory)
        // 运行期对象上有 build() 方法, 这里不预先绑定, 调用时反射获取

        // getMythicTypeFromItem(ItemStack)
        try {
            getMythicType = itemManager.getClass().getMethod("getMythicTypeFromItem", ItemStack.class);
        } catch (NoSuchMethodException e) {
            getMythicType = null;
        }
        return true;
    }

    private boolean tryV4() throws Throwable {
        Class<?> mythicMobs = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
        Method inst = mythicMobs.getMethod("inst");
        Object core = inst.invoke(null);
        Method getItemManager = core.getClass().getMethod("getItemManager");
        itemManager = getItemManager.invoke(core);
        v4GetItemStack = itemManager.getClass().getMethod("getItemStack", String.class);
        // v4 物品识别: 无直接API, 后续走 isSimilar 兜底
        return true;
    }

    /**
     * 从物品库生成一个物品。失败返回 null。
     */
    public ItemStack getItemStack(String id, Player player) {
        if (!enabled || id == null) return null;
        try {
            if (v4GetItemStack != null) {
                Object result = v4GetItemStack.invoke(itemManager, id);
                if (result instanceof ItemStack) {
                    return (ItemStack) result;
                }
                return null;
            }
            // v5
            Object opt = getItemOpt.invoke(itemManager, id);
            if (opt == null) return null;
            boolean present = (Boolean) optIsPresent.invoke(opt);
            if (!present) return null;
            Object mythicItem = optGet.invoke(opt);
            Object abstractStack = generateItemStack.invoke(mythicItem, 1);
            if (abstractStack == null) return null;
            // 若运行期就是 ItemStack (部分实现) 直接返回
            if (abstractStack instanceof ItemStack) {
                return (ItemStack) abstractStack;
            }
            // 否则调用 build()
            Method build = abstractStack.getClass().getMethod("build");
            Object built = build.invoke(abstractStack);
            return (built instanceof ItemStack) ? (ItemStack) built : null;
        } catch (Throwable t) {
            logger.warning("[LonelyFishing] MythicMobs 获取物品失败 [" + id + "]: " + t.getMessage());
            return null;
        }
    }

    /**
     * 识别物品对应的 MythicMobs 内部名。非 MM 物品返回 null。
     */
    public String getInternalName(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            if (getMythicType != null) {
                Object result = getMythicType.invoke(itemManager, item);
                return result == null ? null : result.toString();
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
