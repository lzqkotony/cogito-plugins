// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.gui;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 箱子 GUI 基类。
 *
 * <p>子类只要在 {@link #build()} 里把物品铺进格子；想给格子加点击行为，就用
 * {@link #set(int, ItemStack, BiConsumer)} 绑定。{@link MenuListener} 负责把玩家的点击
 * 路由过来，同时把菜单里的物品锁住——拿不走、拖不动、shift 搬不走。
 *
 * <p><b>一个玩家一个实例</b>：菜单的 InventoryHolder 就是本对象，玩家打开的是同一个
 * Inventory，所以不要在多个玩家之间复用同一个 Menu，每次打开都 new 一个。
 */
public abstract class Menu implements InventoryHolder {

    private final CogitoPlugin plugin;
    private final Inventory inventory;
    private final Map<Integer, BiConsumer<Player, InventoryClickEvent>> actions = new HashMap<>();

    /**
     * @param rows             行数（1~6，每行 9 格）
     * @param titleMiniMessage 标题，MiniMessage 格式
     */
    protected Menu(CogitoPlugin plugin, int rows, String titleMiniMessage) {
        this.plugin = plugin;
        int size = Math.min(6, Math.max(1, rows)) * 9;
        this.inventory = Bukkit.createInventory(this, size, Messages.of(titleMiniMessage));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected CogitoPlugin plugin() {
        return plugin;
    }

    /** 铺菜单内容。会被重复调用（打开时、刷新时），实现里按空白菜单处理即可。 */
    protected abstract void build();

    /** 打开给某个玩家（会先重建一次内容）。 */
    public void open(Player player) {
        rebuild();
        player.openInventory(inventory);
    }

    /** 重建内容：清掉旧的物品与点击绑定，再执行一次 {@link #build()}。 */
    protected void rebuild() {
        actions.clear();
        inventory.clear();
        build();
    }

    /** 放一个纯装饰物品。 */
    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** 放一个可点击物品；action 为 null 表示纯装饰。 */
    protected void set(int slot, ItemStack item, BiConsumer<Player, InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    /** 用同一个物品把最外圈铺满，做出箱子的边框。 */
    protected void fillBorder(ItemStack filler) {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, filler);
                actions.remove(slot);
            }
        }
    }

    /** 刷新当前界面（物品数量、余额这些动态内容）。 */
    protected void refresh() {
        rebuild();
    }

    void handleClick(Player player, InventoryClickEvent event) {
        BiConsumer<Player, InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(player, event);
        }
    }
}
