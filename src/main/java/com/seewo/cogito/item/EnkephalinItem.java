// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.item;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 脑啡肽的操作入口（造物 / 识别 / 统计 / 扣除 / 发放）。
 *
 * <p>物品定义现在统一放在 items.yml 的 {@code pe} 项里，本类只是它在命令与 GUI 层的外壳，
 * 这样既保留原有调用方式，又不用把材质、描述、堆叠这些写死在代码里。
 */
public final class EnkephalinItem {

    /** items.yml 里脑啡肽的 id。 */
    public static final String ITEM_ID = "pe";

    private final CogitoPlugin plugin;

    public EnkephalinItem(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    private CustomItem definition() {
        CustomItem item = plugin.items().find(ITEM_ID);
        if (item == null) {
            throw new IllegalStateException("items.yml 里缺少 " + ITEM_ID + " 物品定义");
        }
        return item;
    }

    public ItemStack create(int amount) {
        return definition().create(amount);
    }

    public boolean isEnkephalin(ItemStack stack) {
        return definition().matches(stack);
    }

    /** 统计玩家身上（含护甲/副手槽）的脑啡肽总数。 */
    public int count(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isEnkephalin(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** 从玩家身上扣除脑啡肽，返回实际扣除数量（不够就扣多少算多少）。 */
    public int remove(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;
        int removed = 0;

        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isEnkephalin(item)) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            int left = item.getAmount() - take;
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(left);
                inventory.setItem(slot, item);
            }
            remaining -= take;
            removed += take;
        }
        return removed;
    }

    /** 给玩家脑啡肽；背包放不下就掉在脚下。 */
    public void give(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        int perStack = definition().maxStackSize();
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            int size = Math.min(perStack, remaining);
            stacks.add(create(size));
            remaining -= size;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) {
            Location location = player.getLocation();
            for (ItemStack stack : leftover.values()) {
                player.getWorld().dropItemNaturally(location, stack);
            }
            Messages.send(player, "<yellow>背包放不下，多出来的脑啡肽掉在你脚下了");
        }
    }

    /** 统一标签：cogito:item。 */
    public NamespacedKey tagKey() {
        return new NamespacedKey(plugin, "item");
    }

    public Material material() {
        return definition().material();
    }

    public Enchantment enchantment() {
        return definition().enchantment();
    }

    public int customModelData() {
        return definition().customModelData();
    }
}
