// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** 从玩家背包中装载一次性 E.G.O. 蓝图。 */
public final class BlueprintMenu extends Menu {

    private static final String PREFIX = "blueprint-";
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final Player viewer;

    public BlueprintMenu(CogitoPlugin plugin, Player viewer) {
        super(plugin, 5, "<gradient:#facc15:#a855f7>装载蓝图</gradient>");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        fillBorder(icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));
        Map<EgoSetDefinition, CustomItem> available = availableBlueprints();
        set(4, icon(Material.PAPER, "<gold>一次性蓝图", List.of(
                "<gray>点击装载后消耗 1 张",
                "<gray>该套研发能力绑定到当前玩家",
                "<dark_gray>已绑定玩家的蓝图不会再次消耗")));

        if (available.isEmpty()) {
            set(22, icon(Material.BARRIER, "<gray>背包里没有可装载蓝图", List.of(
                    "<dark_gray>可通过 /cogito give blueprint 获取")));
        }

        int index = 0;
        PlayerProfile profile = profile();
        for (Map.Entry<EgoSetDefinition, CustomItem> entry : available.entrySet()) {
            if (index >= SLOTS.length) {
                break;
            }
            EgoSetDefinition set = entry.getKey();
            CustomItem blueprint = entry.getValue();
            boolean unlocked = profile != null && profile.isEgoUnlocked(set.id());
            set(SLOTS[index++], blueprintIcon(blueprint, set, unlocked), (player, event) -> {
                if (unlocked) {
                    Messages.send(player, "<yellow>你已经装载过 <white>" + set.displayName() + "</white> 的蓝图");
                    return;
                }
                if (plugin().egoDevelopment().unlock(player, set, true)) {
                    new DirectedDevelopMenu(plugin(), player).open(player);
                } else {
                    refresh();
                }
            });
        }

        set(40, icon(Material.BARRIER, "<red>返回开发台", List.of("<dark_gray>回到上级界面")),
                (player, event) -> new DevelopMenu(plugin(), player).open(player));
    }

    private Map<EgoSetDefinition, CustomItem> availableBlueprints() {
        Map<EgoSetDefinition, CustomItem> result = new LinkedHashMap<>();
        for (CustomItem item : plugin().items().all()) {
            if (!item.id().startsWith(PREFIX) || item.countIn(viewer) <= 0) {
                continue;
            }
            EgoSetDefinition set = plugin().ego().set(item.id().substring(PREFIX.length()));
            if (set != null && set.developable()) {
                result.putIfAbsent(set, item);
            }
        }
        return result;
    }

    private ItemStack blueprintIcon(CustomItem blueprint, EgoSetDefinition set, boolean unlocked) {
        ItemStack stack = blueprint.create(1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.of((unlocked ? "<green>" : "<yellow>") + "蓝图 · " + set.displayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Messages.of("<gray>背包持有：<white>" + blueprint.countIn(viewer) + " 张"));
            lore.add(Messages.of(unlocked ? "<green>已装载并与你绑定" : "<gray>点击装载并消耗 1 张"));
            lore.add(Messages.of("<dark_gray>装载后可在定向开发中选择护甲或武器"));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
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
