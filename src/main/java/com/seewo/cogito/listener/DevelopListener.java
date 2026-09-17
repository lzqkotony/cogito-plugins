// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.gui.DevelopMenu;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 开发台与研发图纸的行为。
 *
 * <p>设计稿（2026-09-17）：
 * <ul>
 *   <li>开发台：爆炸抗性同黑曜石（炸不掉）、必须钻石镐及以上才能破坏、上方有全息文本</li>
 *   <li>图纸：右键解锁对应 E.G.O. 的研发能力（图纸消耗一张），之后研发不再需要图纸</li>
 * </ul>
 */
public final class DevelopListener implements Listener {

    public static final String TABLE_ID = "develop-table";
    private static final String BLUEPRINT_PREFIX = "blueprint-";

    private final CogitoPlugin plugin;

    public DevelopListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ 开发台方块

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        CustomItem item = plugin.items().identify(event.getItemInHand());
        if (item == null || !TABLE_ID.equals(item.id())) {
            return;
        }
        plugin.developTables().add(event.getBlockPlaced());
        Messages.send(event.getPlayer(), "<green>开发台已放置。<gray>右键打开研发界面。");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.developTables().isTable(block)) {
            return;
        }
        Player player = event.getPlayer();
        Material tool = player.getInventory().getItemInMainHand().getType();
        if (tool != Material.DIAMOND_PICKAXE && tool != Material.NETHERITE_PICKAXE) {
            event.setCancelled(true);
            Messages.send(player, "<red>开发台很硬，需要钻石镐及以上的镐子才能破坏。");
            return;
        }

        // 自己处理掉落，保证掉出来的是"开发台"物品而不是普通工作台
        event.setCancelled(true);
        plugin.developTables().remove(block);
        block.setType(Material.AIR);
        CustomItem definition = plugin.items().find(TABLE_ID);
        if (definition != null) {
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().dropItemNaturally(center, definition.create(1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(plugin.developTables()::isTable);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(plugin.developTables()::isTable);
    }

    // ------------------------------------------------------------ 右键行为

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();

        Block clicked = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clicked != null
                && plugin.developTables().isTable(clicked) && !player.isSneaking()) {
            event.setCancelled(true);
            new DevelopMenu(plugin, player).open(player);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (unlockWithBlueprint(player, player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 手持图纸右键：解锁该套 E.G.O. 的研发能力。
     *
     * <p>图纸**不是消耗品**——每套 E.G.O. 有自己专属的一张，右键解锁后图纸留在玩家手上。
     */
    private boolean unlockWithBlueprint(Player player, ItemStack hand) {
        CustomItem item = plugin.items().identify(hand);
        if (item == null || !item.id().startsWith(BLUEPRINT_PREFIX)) {
            return false;
        }
        String setId = item.id().substring(BLUEPRINT_PREFIX.length());
        EgoSetDefinition set = plugin.ego().set(setId);
        if (set == null) {
            Messages.send(player, "<red>这张图纸对应的套装不存在：" + setId);
            return true;
        }

        PlayerProfile profile = plugin.data().cached(player.getUniqueId());
        if (profile == null) {
            profile = plugin.data().join(player);
        }
        if (profile.isEgoUnlocked(set.id())) {
            Messages.send(player, "<yellow>你已经解锁过 <white>" + set.displayName() + "</white> 的研发能力");
            return true;
        }

        profile.unlockEgo(set.id());
        plugin.data().saveAsync(profile);

        Messages.send(player, "<green>已解锁 <white>" + set.displayName()
                + "</white> 的研发能力。<gray>图纸会留在你手上，之后在开发台研发只需消耗脑啡肽。");
        return true;
    }
}
