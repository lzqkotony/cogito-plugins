// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * 拦掉 Cogito 物品的「原版行为」。
 *
 * <p>设计稿要求：注册的物品不能像原型那样被使用——不能当成方块放下去、不能参与原版合成
 * （否则绿宝石拿去合成就能套利）。是否允许由 items.yml 的 {@code placeable} / {@code craftable} 控制。
 */
public final class ItemBehaviourListener implements Listener {

    private final CogitoPlugin plugin;

    public ItemBehaviourListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        CustomItem item = plugin.items().identify(event.getItemInHand());
        if (item == null || item.placeable()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Messages.send(player, "<red>" + displayName(item) + " <red>不能放置");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        CustomItem item = plugin.items().identify(event.getItem());
        if (item == null || !item.id().equalsIgnoreCase("cogito")) {
            return;
        }
        event.setCancelled(true);
        Messages.send(event.getPlayer(), "<red>Cogito 不能被饮用");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        // 插件自己注册的配方（例如开发台）允许使用自定义物品当材料
        if (recipe instanceof Keyed keyed
                && keyed.getKey().getNamespace().equalsIgnoreCase(plugin.getName().toLowerCase(Locale.ROOT))) {
            return;
        }
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            CustomItem item = plugin.items().identify(ingredient);
            if (item != null && !item.craftable()) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    private String displayName(CustomItem item) {
        return item.displayName();
    }
}
