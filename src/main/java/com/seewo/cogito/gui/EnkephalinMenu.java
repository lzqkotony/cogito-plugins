// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 「脑啡肽箱子」——箱子样式的 GUI 主界面（/cogito gui）。
 *
 * <p>5 行 45 格的摆放：
 * <pre>
 *   11 兑换 ×1      13 脑啡肽信息    15 兑换 ×8
 *   21 兑换 ×64     23 我的余额      30 命令帮助
 *   32 管理:配置    34 管理:重载     40 关闭
 * </pre>
 *
 * <p>兑换按钮的档位来自 config.yml 的 {@code gui.exchange-amounts}，最多放三个，
 * 超过单次上限的部分会被配置里的 {@code economy.max-per-exchange} 截断。
 */
public final class EnkephalinMenu extends Menu {

    private static final int[] EXCHANGE_SLOTS = {11, 15, 21};
    private static final int SLOT_INFO = 13;
    private static final int SLOT_BALANCE = 23;
    private static final int SLOT_HELP = 30;
    private static final int SLOT_ADMIN_INFO = 32;
    private static final int SLOT_ADMIN_RELOAD = 34;
    private static final int SLOT_CLOSE = 40;

    private static final String PERMISSION_ADMIN = "cogito.admin";

    private final Player viewer;

    public EnkephalinMenu(CogitoPlugin plugin, Player viewer) {
        super(plugin,
                // 布局按 5 行设计，配置里写小了会自动抬到 5 行
                Math.max(5, plugin.getConfig().getInt("gui.rows", 5)),
                plugin.getConfig().getString("gui.title",
                        "<gradient:#22d3a8:#3b82f6>脑啡肽箱子</gradient>"));
        this.viewer = viewer;
    }

    @Override
    protected void build() {
        fillBorder(icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));

        double price = price();
        int held = plugin().enkephalinItem().count(viewer);

        set(SLOT_INFO, infoItem(held, price));

        set(SLOT_BALANCE, balanceItem(), (player, event) -> {
            refresh();
            Messages.send(player, "<gray>余额已刷新");
        });

        List<Integer> amounts = exchangeAmounts();
        for (int index = 0; index < amounts.size(); index++) {
            int amount = amounts.get(index);
            set(EXCHANGE_SLOTS[index], exchangeItem(amount, price),
                    (player, event) -> exchange(player, amount));
        }

        set(SLOT_HELP, icon(Material.BOOK, "<aqua>命令帮助", List.of(
                "<gray>/enkephalin <dark_gray>- 查看持有量（聊天栏）",
                "<gray>/exchange <数量> <dark_gray>- 用钱兑换脑啡肽",
                "<gray>/cogito gui <dark_gray>- 打开这个箱子",
                "<dark_gray>点这里也可以，效果一样")),
                (player, event) -> {
                    player.closeInventory();
                    Messages.raw(player, "<gray>—— 脑啡肽命令 ——");
                    Messages.raw(player, "<yellow>/enkephalin <gray>- 查看自己持有的数量");
                    Messages.raw(player, "<yellow>/exchange <数量> <gray>- 用钱兑换脑啡肽");
                    Messages.raw(player, "<yellow>/cogito gui <gray>- 打开箱子界面");
                });

        if (viewer.hasPermission(PERMISSION_ADMIN)) {
            set(SLOT_ADMIN_INFO, icon(Material.REDSTONE, "<red>管理：配置信息", List.of(
                    "<gray>材质：<white>" + plugin().enkephalinItem().material(),
                    "<gray>标签：<white>" + plugin().enkephalinItem().tagKey(),
                    "<gray>附魔：<white>" + plugin().enkephalinItem().enchantment(),
                    "<gray>单价：<white>" + format(price) + " <gray>/ 个",
                    "<dark_gray>点击输出到聊天栏")),
                    (player, event) -> {
                        player.closeInventory();
                        sendConfigTo(player);
                    });

            set(SLOT_ADMIN_RELOAD, icon(Material.REPEATER, "<red>管理：重载配置", List.of(
                    "<gray>重新读取 <white>config.yml",
                    "<dark_gray>等价于 /enkephalin reload")),
                    (player, event) -> {
                        plugin().reloadPluginConfig();
                        Messages.send(player, "<green>配置已重载");
                        refresh();
                    });
        }

        set(SLOT_CLOSE, icon(Material.BARRIER, "<red>关闭", List.of("<dark_gray>点击关闭箱子")),
                (player, event) -> player.closeInventory());
    }

    // ------------------------------------------------------------ 界面物品

    private ItemStack infoItem(int held, double price) {
        int shown = Math.min(64, Math.max(1, held));
        ItemStack stack = plugin().enkephalinItem().create(shown);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Messages.of("<gray>你当前持有：<white>" + held + "</white> 个"));
            lore.add(Messages.of("<gray>兑换单价：<white>" + format(price) + "</white> / 个"));
            lore.add(Messages.of("<gray>单次上限：<white>" + maxPerExchange() + "</white> 个"));
            lore.add(Component.empty());
            lore.add(Messages.of("<dark_gray>脑啡肽用于 E.G.O 开发与提取"));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack balanceItem() {
        boolean ready = plugin().vault().isReady();
        String economy = ready ? plugin().vault().economy().getName()
                : "<red>未连接（需要 Vault + 经济插件）";
        return icon(Material.SUNFLOWER, "<gold>我的余额", List.of(
                "<gray>经济系统：<white>" + economy,
                "<gray>余额：<white>" + format(ready ? plugin().vault().balance(viewer) : 0D),
                "<gray>还能兑换：<white>" + affordable(price()) + "</white> 个",
                "<dark_gray>点击刷新"));
    }

    private ItemStack exchangeItem(int amount, double price) {
        double cost = price * amount;
        boolean enough = plugin().vault().isReady() && plugin().vault().has(viewer, cost);
        Material material = amount >= 64 ? Material.GOLD_BLOCK
                : amount >= 8 ? Material.GOLD_INGOT
                : Material.GOLD_NUGGET;
        return icon(material, "<yellow>兑换 ×" + amount, List.of(
                "<gray>花费：<white>" + format(cost) + (enough ? "" : " <red>（余额不足）"),
                "<gray>得到：<white>" + amount + "</white> 个脑啡肽",
                "",
                enough ? "<green>点击兑换" : "<red>点击后会有提示"));
    }

    private ItemStack icon(Material material, String name, List<String> lore, ItemFlag... flags) {
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
            if (flags.length > 0) {
                meta.addItemFlags(flags);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // -------------------------------------------------------------- 行为

    private void exchange(Player player, int amount) {
        if (!plugin().vault().isReady()) {
            Messages.send(player, "<red>服务器没有可用的经济系统：需要安装 Vault + 一个经济插件");
            return;
        }

        int count = Math.min(amount, maxPerExchange());
        double cost = price() * count;

        if (!plugin().vault().has(player, cost)) {
            Messages.send(player, "<red>余额不足：需要 <white>" + plugin().vault().format(cost)
                    + "</white>，你当前有 <white>" + plugin().vault().format(plugin().vault().balance(player)));
            refresh();
            return;
        }
        if (!plugin().vault().withdraw(player, cost)) {
            Messages.send(player, "<red>扣款失败，请稍后再试");
            refresh();
            return;
        }

        plugin().enkephalinItem().give(player, count);
        Messages.send(player, "<green>兑换成功：<white>" + plugin().vault().format(cost)
                + "</white> → <white>" + count + "</white> 个脑啡肽 <gray>（单向兑换，不可逆）");
        refresh();
    }

    private void sendConfigTo(Player player) {
        Messages.raw(player, "<gray>—— 脑啡肽物品配置 ——");
        Messages.raw(player, "<gray>材质：<white>" + plugin().enkephalinItem().material()
                + " <gray>| 标签：<white>" + plugin().enkephalinItem().tagKey()
                + " <gray>| 附魔：<white>" + plugin().enkephalinItem().enchantment());
        Messages.raw(player, "<gray>CustomModelData：<white>" + plugin().enkephalinItem().customModelData());
        Messages.raw(player, "<gray>兑换价：<white>" + format(price())
                + " <gray>/ 个，单次上限 <white>" + maxPerExchange());
    }

    // -------------------------------------------------------------- 配置

    private double price() {
        return plugin().getConfig().getDouble("economy.price-per-enkephalin", 100D);
    }

    private int maxPerExchange() {
        return Math.max(1, plugin().getConfig().getInt("economy.max-per-exchange", 64));
    }

    private int affordable(double price) {
        if (!plugin().vault().isReady() || price <= 0D) {
            return 0;
        }
        return (int) Math.floor(plugin().vault().balance(viewer) / price);
    }

    private List<Integer> exchangeAmounts() {
        List<Integer> configured = plugin().getConfig().getIntegerList("gui.exchange-amounts");
        if (configured.isEmpty()) {
            configured = List.of(1, 8, 64);
        }
        List<Integer> result = new ArrayList<>(EXCHANGE_SLOTS.length);
        for (int amount : configured) {
            if (amount <= 0) {
                continue;
            }
            result.add(Math.min(amount, maxPerExchange()));
            if (result.size() == EXCHANGE_SLOTS.length) {
                break;
            }
        }
        if (result.isEmpty()) {
            result.add(1);
        }
        return result;
    }

    private String format(double amount) {
        return plugin().vault().isReady()
                ? plugin().vault().format(amount)
                : String.format(Locale.ROOT, "%.2f", amount);
    }
}
