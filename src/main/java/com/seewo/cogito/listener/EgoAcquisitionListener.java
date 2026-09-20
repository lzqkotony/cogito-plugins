// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/** 阻止非白名单玩家通过发放、拾取或容器交互获得受限 E.G.O.。 */
public final class EgoAcquisitionListener implements Listener {

    private final CogitoPlugin plugin;

    public EgoAcquisitionListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        CustomItem item = plugin.items().identify(event.getItem().getItemStack());
        if (item == null || plugin.ego().canAcquire(player, item)) {
            return;
        }
        event.setCancelled(true);
        event.getItem().setPickupDelay(40);
        deny(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isAllowed(player, event.getCurrentItem()) && isAllowed(player, event.getCursor())) {
            return;
        }
        event.setCancelled(true);
        deny(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isAllowed(player, event.getOldCursor())) {
            return;
        }
        event.setCancelled(true);
        deny(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int removed = plugin.ego().purgeRestrictedItems(player);
        if (removed > 0) {
            Messages.send(player, "<red>你持有的受限 E.G.O. 已按白名单移除（共 "
                    + removed + " 件）");
        }
    }

    private boolean isAllowed(Player player, ItemStack stack) {
        CustomItem item = plugin.items().identify(stack);
        return item == null || plugin.ego().canAcquire(player, item);
    }

    private void deny(Player player) {
        Messages.send(player, "<red>该 E.G.O. 仅限获取白名单玩家使用，OP 权限也不能绕过");
    }
}
