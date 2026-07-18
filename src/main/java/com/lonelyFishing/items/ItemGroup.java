package com.lonelyFishing.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 物品组: 包含若干带权重的物品
 */
public class ItemGroup {

    private final String id;
    private final List<GroupItem> items;

    public ItemGroup(String id, List<GroupItem> items) {
        this.id = id;
        this.items = items == null ? new ArrayList<GroupItem>() : items;
    }

    public String getId() { return id; }

    public List<GroupItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
