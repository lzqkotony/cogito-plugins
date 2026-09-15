// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 把箱子 GUI 的点击转交给 {@link Menu}，并保证菜单里的物品是幽灵物品：
 * 点不走、拖不走、也不会被 shift 搬进背包。
 */
public final class MenuListener implements Listener {

    public MenuListener(CogitoPlugin plugin) {
        // 目前用不到 plugin；保留参数是为了以后要记日志/统计时不用改调用方
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        // 菜单范围内的一切操作都不生效（包括点到下半部分自己背包的那一下）
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // 只处理点在菜单上半部分的那一下
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof Menu)) {
            return;
        }
        menu.handleClick(player, event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
