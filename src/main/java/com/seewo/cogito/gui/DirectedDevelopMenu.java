// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.EgoDevelopCategory;
import com.seewo.cogito.ego.EgoDevelopTier;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** 定向研发：选择套装的护甲/武器，再选择四档风险进行研发。 */
public final class DirectedDevelopMenu extends Menu {

    private static final int[] LIST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };
    private static final EgoDevelopTier[] TIERS = {
            EgoDevelopTier.NORMAL,
            EgoDevelopTier.ADVANCED,
            EgoDevelopTier.HIGH,
            EgoDevelopTier.FULL
    };

    private final Player viewer;
    private String selectedSetId;
    private EgoDevelopCategory selectedCategory;
    private int page;

    public DirectedDevelopMenu(CogitoPlugin plugin, Player viewer) {
        super(plugin, 6, "<gradient:#22d3a8:#facc15>定向开发</gradient>");
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        fillBorder(icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));
        List<Entry> entries = entries();
        int pageCount = Math.max(1, (entries.size() + LIST_SLOTS.length - 1) / LIST_SLOTS.length);
        page = Math.max(0, Math.min(page, pageCount - 1));

        int held = plugin().enkephalinItem().count(viewer);
        Entry selected = selected(entries);
        set(4, icon(Material.SUNFLOWER, "<gold>当前脑啡肽 <white>" + held, List.of(
                selected == null
                        ? "<gray>请选择要研发的 E.G.O."
                        : "<gray>已选择：<white>" + selected.set().displayName()
                                + " <gray>· <white>" + selected.category().displayName(),
                "<dark_gray>蓝图一次性装载，研发失败也会消耗脑啡肽")));

        int start = page * LIST_SLOTS.length;
        for (int index = 0; index < LIST_SLOTS.length && start + index < entries.size(); index++) {
            Entry entry = entries.get(start + index);
            boolean unlocked = isUnlocked(entry.set());
            boolean active = selected != null
                    && selected.set().id().equals(entry.set().id())
                    && selected.category() == entry.category();
            set(LIST_SLOTS[index], entryIcon(entry, unlocked, active, held), (player, event) -> {
                if (!unlocked) {
                    Messages.send(player, "<red>尚未装载 <white>" + entry.set().displayName()
                            + "</white> 的蓝图，请先回开发台选择「装载蓝图」");
                    return;
                }
                selectedSetId = entry.set().id();
                selectedCategory = entry.category();
                refresh();
            });
        }

        if (entries.isEmpty()) {
            set(22, icon(Material.GRAY_DYE, "<gray>暂无可研发 E.G.O.", List.of()));
        }

        for (int index = 0; index < TIERS.length; index++) {
            EgoDevelopTier tier = TIERS[index];
            set(38 + index * 2, tierIcon(tier, selected, held), (player, event) -> {
                Entry current = selected(entries());
                if (current == null) {
                    Messages.send(player, "<yellow>请先选择一个护甲或武器项目");
                    return;
                }
                plugin().egoDevelopment().attempt(player, current.set(), current.category(), tier);
                refresh();
            });
        }

        set(45, page > 0
                        ? icon(Material.ARROW, "<yellow>上一页", List.of("<gray>第 " + (page + 1) + " / " + pageCount + " 页"))
                        : icon(Material.GRAY_DYE, "<dark_gray>上一页", List.of()),
                (player, event) -> {
                    if (page > 0) {
                        page--;
                        refresh();
                    }
                });
        set(47, icon(Material.BOOK, "<gray>第 <white>" + (page + 1) + " / " + pageCount + " <gray>页",
                List.of("<dark_gray>共 " + entries.size() + " 个研发项目")));
        set(49, icon(Material.BARRIER, "<red>返回开发台", List.of("<dark_gray>回到上级界面")),
                (player, event) -> new DevelopMenu(plugin(), player).open(player));
        set(53, page + 1 < pageCount
                        ? icon(Material.ARROW, "<yellow>下一页", List.of("<gray>第 " + (page + 2) + " / " + pageCount + " 页"))
                        : icon(Material.GRAY_DYE, "<dark_gray>下一页", List.of()),
                (player, event) -> {
                    if (page + 1 < pageCount) {
                        page++;
                        refresh();
                    }
                });
    }

    private List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (EgoSetDefinition set : plugin().ego().developableSets()) {
            for (EgoDevelopCategory category : EgoDevelopCategory.values()) {
                if (set.canDevelop(category)
                        && !plugin().ego().itemsForSet(set.id(), category).isEmpty()) {
                    result.add(new Entry(set, category));
                }
            }
        }
        return result;
    }

    private Entry selected(List<Entry> entries) {
        if (selectedSetId == null || selectedCategory == null) {
            return null;
        }
        for (Entry entry : entries) {
            if (entry.set().id().equals(selectedSetId) && entry.category() == selectedCategory) {
                return entry;
            }
        }
        return null;
    }

    private boolean isUnlocked(EgoSetDefinition set) {
        PlayerProfile profile = profile();
        return profile != null && profile.isEgoUnlocked(set.id());
    }

    private ItemStack entryIcon(Entry entry, boolean unlocked, boolean active, int held) {
        ItemStack stack = firstItem(entry);
        if (stack == null) {
            stack = new ItemStack(Material.GRAY_DYE);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String prefix = active ? "<yellow>▶ " : unlocked ? "<green>" : "<dark_gray>";
        meta.displayName(Messages.of(prefix + entry.set().displayName()
                + " <gray>· " + entry.category().displayName()));
        List<Component> lore = new ArrayList<>();
        int baseCost = entry.set().developCost(entry.category());
        lore.add(Messages.of("<gray>基础脑啡肽 x = <white>" + baseCost));
        if (!unlocked) {
            lore.add(Messages.of("<red>未装载蓝图"));
            lore.add(Messages.of("<gray>需要：<white>" + entry.set().displayName() + " 专属蓝图"));
        } else {
            lore.add(Messages.of("<gray>你持有：<white>" + held));
            lore.add(Component.empty());
            for (EgoDevelopTier tier : TIERS) {
                int cost = plugin().egoDevelopment().cost(entry.set(), entry.category(), tier);
                String color = held >= cost ? "<gray>" : "<red>";
                lore.add(Messages.of(color + tier.displayName() + " <dark_gray>| <white>"
                        + cost + " 脑啡肽 <dark_gray>| <white>" + tier.successPercent() + "% 成功"));
            }
            lore.add(Component.empty());
            lore.add(Messages.of(active ? "<yellow>已选中，请在下方选择档位" : "<yellow>点击选择"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack tierIcon(EgoDevelopTier tier, Entry selected, int held) {
        if (selected == null) {
            return icon(Material.GRAY_DYE, "<dark_gray>" + tier.displayName(), List.of("<gray>先选择研发项目"));
        }
        int cost = plugin().egoDevelopment().cost(selected.set(), selected.category(), tier);
        boolean affordable = held >= cost;
        Material material = switch (tier) {
            case NORMAL -> Material.IRON_INGOT;
            case ADVANCED -> Material.GOLD_INGOT;
            case HIGH -> Material.DIAMOND;
            case FULL -> Material.NETHERITE_INGOT;
        };
        return icon(material,
                (affordable ? "<green>" : "<red>") + tier.displayName(),
                List.of(
                        "<gray>费用倍率：<white>" + Math.round(tier.costMultiplier() * 100) + "%",
                        "<gray>消耗：<white>" + cost + " 脑啡肽" + (affordable ? "" : " <red>（不足）"),
                        "<gray>成功率：<white>" + tier.successPercent() + "%",
                        "<dark_gray>失败也消耗脑啡肽",
                        "",
                        affordable ? "<yellow>点击开始研发" : "<red>脑啡肽不足"));
    }

    private ItemStack firstItem(Entry entry) {
        for (CustomItem item : plugin().ego().itemsForSet(entry.set().id(), entry.category())) {
            return item.create(1);
        }
        return null;
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

    private record Entry(EgoSetDefinition set, EgoDevelopCategory category) {
    }
}
