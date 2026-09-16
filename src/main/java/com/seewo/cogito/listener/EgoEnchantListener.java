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
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

/** 阻止 E.G.O. 物品被附魔。 */
public final class EgoEnchantListener implements Listener {

    private final CogitoPlugin plugin;

    public EgoEnchantListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (isLocked(event.getItem())) {
            event.setCancelled(true);
            Messages.send(event.getEnchanter(), "<red>E.G.O. 物品不能被附魔");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (isLocked(first) || isLocked(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim().toLowerCase();
        if (!message.startsWith("/enchant ") && !message.startsWith("/minecraft:enchant ")) {
            return;
        }
        Player player = event.getPlayer();
        if (isLocked(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            Messages.send(player, "<red>E.G.O. 物品不能被附魔");
        }
    }

    private boolean isLocked(ItemStack stack) {
        CustomItem item = plugin.items().identify(stack);
        return item != null && !item.enchantable();
    }
}
