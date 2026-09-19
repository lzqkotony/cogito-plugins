// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** 开发台主页：定向研发、随机研发占位和一次性蓝图装载。 */
public final class DevelopMenu extends Menu {

    private final Player viewer;

    public DevelopMenu(CogitoPlugin plugin, Player viewer) {
        super(plugin, 3, plugin.getConfig().getString("develop.title",
                "<gradient:#22d3a8:#facc15>开发台</gradient>"));
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        fillBorder(icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));
        int held = plugin().enkephalinItem().count(viewer);
        set(4, icon(Material.SUNFLOWER, "<gold>当前脑啡肽 <white>" + held, List.of(
                "<gray>定向研发会按档位消耗脑啡肽",
                "<dark_gray>失败同样消耗")));

        set(11, icon(Material.NETHERITE_HELMET, "<gradient:#22d3a8:#facc15>定向开发", List.of(
                "<gray>选择护甲或武器，再进行四档研发",
                "<dark_gray>普通 / 进阶 / 高级 / 完全")),
                (player, event) -> new DirectedDevelopMenu(plugin(), player).open(player));

        set(13, icon(Material.END_PORTAL_FRAME, "<light_purple>随机开发", List.of(
                "<gray>敬请期待")),
                (player, event) -> Messages.send(player, "<yellow>随机开发仍在施工中，敬请期待"));

        set(15, icon(Material.PAPER, "<gradient:#facc15:#a855f7>装载蓝图", List.of(
                "<gray>从背包装载一次性专属蓝图",
                "<dark_gray>装载后与该玩家绑定")),
                (player, event) -> new BlueprintMenu(plugin(), player).open(player));

        set(22, icon(Material.BARRIER, "<red>关闭", List.of("<dark_gray>关闭开发台")),
                (player, event) -> player.closeInventory());
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
