// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 开发台界面：列出所有 E.G.O. 套装，点已解锁的就能研发。
 *
 * <p>研发规则（2026-09-17 设计稿）：图纸只负责**解锁**研发能力，解锁之后每次研发都消耗
 * {@code ego.yml → develop.cost} 个脑啡肽，不再需要图纸。
 */
public final class DevelopMenu extends Menu {

    private final Player viewer;

    public DevelopMenu(CogitoPlugin plugin, Player viewer) {
        super(plugin, 5, plugin.getConfig().getString("develop.title",
                "<gradient:#22d3a8:#facc15>开发台</gradient>"));
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        fillBorder(icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));

        PlayerProfile profile = profile();
        int held = plugin().enkephalinItem().count(viewer);

        set(4, icon(Material.SUNFLOWER, "<gold>当前脑啡肽", List.of(
                "<gray>持有：<white>" + held + "</white> 个",
                "<gray>选中套装上的数字是研发一次的消耗",
                "<dark_gray>右键图纸可解锁对应套装")),
                (player, event) -> refresh());

        int index = 0;
        for (EgoSetDefinition set : plugin().ego().sets()) {
            int slot = 10 + (index % 4) * 2 + (index / 4) * 9;
            index++;
            boolean unlocked = profile != null && profile.isEgoUnlocked(set.id());
            set(slot, setIcon(set, unlocked, held, profile), (player, event) -> {
                if (!unlocked) {
                    Messages.send(player, "<yellow>这套 E.G.O. 还没解锁：先用 <white>E.G.O. 研发图纸</white> 右键解锁");
                    return;
                }
                develop(player, set);
            });
        }

        set(40, icon(Material.BARRIER, "<red>关闭", List.of("<dark_gray>点击关闭开发台")),
                (player, event) -> player.closeInventory());
    }

    private ItemStack setIcon(EgoSetDefinition set, boolean unlocked, int held, PlayerProfile profile) {
        int cost = set.developCost();
        boolean affordable = held >= cost;

        ItemStack base = null;
        for (CustomItem item : plugin().ego().itemsForSet(set.id())) {
            if (item.egoPiece() == com.seewo.cogito.ego.EgoPiece.HELMET) {
                base = item.create(1);
                break;
            }
        }
        if (base == null) {
            base = new ItemStack(unlocked ? Material.NETHERITE_HELMET : Material.GRAY_DYE);
        }

        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.of((unlocked ? "<green>" : "<dark_gray>") + set.displayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Messages.of("<gray>统一抗性 x = <white>" + set.resistance()));
            if (unlocked) {
                lore.add(Messages.of("<gray>研发消耗：<white>" + cost + "</white> 个脑啡肽"
                        + (affordable ? "" : " <red>（不足）")));
                lore.add(Messages.of("<dark_gray>武器与饰品不计入防具件数"));
                lore.add(Component.empty());
                lore.add(Messages.of(affordable ? "<yellow>点击研发整套 E.G.O." : "<red>脑啡肽不足"));
            } else {
                lore.add(Messages.of("<red>未解锁"));
                lore.add(Messages.of("<gray>需要：<white>" + set.displayName() + " 专属图纸"));
                lore.add(Messages.of("<dark_gray>右键图纸解锁，图纸不会消耗"));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            base.setItemMeta(meta);
        }
        return base;
    }

    private void develop(Player player, EgoSetDefinition set) {
        PlayerProfile profile = profile();
        if (profile == null || !profile.isEgoUnlocked(set.id())) {
            Messages.send(player, "<red>这套 E.G.O. 还没有解锁");
            return;
        }
        int cost = set.developCost();
        int held = plugin().enkephalinItem().count(player);
        if (held < cost) {
            Messages.send(player, "<red>脑啡肽不足：需要 <white>" + cost + "</white>，你只有 <white>" + held);
            refresh();
            return;
        }
        int removed = plugin().enkephalinItem().remove(player, cost);
        if (removed < cost) {
            Messages.send(player, "<red>扣除失败，请再试一次");
            refresh();
            return;
        }

        int given = 0;
        for (CustomItem item : plugin().ego().itemsForSet(set.id())) {
            give(player, item.create(1));
            given++;
        }
        Messages.send(player, "<green>研发完成：<white>" + set.displayName() + "</white> 共 "
                + given + " 件 <gray>（消耗 " + cost + " 脑啡肽）");
        refresh();
    }

    private void give(Player player, ItemStack stack) {
        if (!player.getInventory().addItem(stack).isEmpty()) {
            Location location = player.getLocation();
            player.getWorld().dropItemNaturally(location, stack);
        }
    }

    private PlayerProfile profile() {
        PlayerProfile profile = plugin().data().cached(viewer.getUniqueId());
        return profile != null ? profile : plugin().data().join(viewer);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.of(name));
            if (!lore.isEmpty()) {
                List<Component> lines = new ArrayList<>(lore.size());
                for (String line : lore) {
                    lines.add(line.isEmpty() ? Component.empty() : Messages.of(line));
                }
                meta.lore(lines);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
