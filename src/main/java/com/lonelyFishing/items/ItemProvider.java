package com.lonelyFishing.items;

import com.lonelyFishing.hooks.MythicMobsHook;
import com.lonelyFishing.hooks.NeigeItemsHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 物品门面: 依据 source (mm / ni) 调用对应物品库获取物品
 */
public class ItemProvider {

    private final MythicMobsHook mm;
    private final NeigeItemsHook ni;

    public ItemProvider(MythicMobsHook mm, NeigeItemsHook ni) {
        this.mm = mm;
        this.ni = ni;
    }

    public ItemStack getItemStack(String source, String itemId, Player player) {
        if (itemId == null) return null;
        if ("mm".equalsIgnoreCase(source) && mm != null && mm.isEnabled()) {
            return mm.getItemStack(itemId, player);
        }
        if ("ni".equalsIgnoreCase(source) && ni != null && ni.isEnabled()) {
            return ni.getItemStack(itemId, player);
        }
        return null;
    }
}
