// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.develop;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

/**
 * 开发台（自定义方块）的位置管理与全息文本。
 *
 * <p>Paper 插件没法注册真正的自定义方块，所以做法是：
 * <ul>
 *   <li>玩家放下"开发台"物品 → 原版放下一个工作台，插件把坐标记进**区块的 PDC**（跟着区块存档，不需要数据库）</li>
 *   <li>方块上方挂一个 {@link TextDisplay} 当"全息文本"，破坏时一起清掉</li>
 *   <li>是否开发台只看坐标记录；普通的玩家工作台不受影响</li>
 * </ul>
 */
public final class DevelopTableManager {

    private static final String SEPARATOR = ";";

    private final CogitoPlugin plugin;
    private final NamespacedKey tableKey;
    private final NamespacedKey hologramKey;

    public DevelopTableManager(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.tableKey = new NamespacedKey(plugin, "develop_table");
        this.hologramKey = new NamespacedKey(plugin, "develop_table_hologram");
    }

    public boolean isTable(Block block) {
        if (block == null) {
            return false;
        }
        return readKeys(block).contains(positionKey(block));
    }

    /** 登记一个开发台，并挂上全息文本。 */
    public void add(Block block) {
        Set<String> keys = readKeys(block);
        if (keys.add(positionKey(block))) {
            writeKeys(block, keys);
        }
        spawnHologram(block);
    }

    /** 注销开发台，并移除全息文本。 */
    public void remove(Block block) {
        Set<String> keys = readKeys(block);
        if (keys.remove(positionKey(block))) {
            writeKeys(block, keys);
        }
        removeHologram(block);
    }

    /** 区块里登记了几个开发台（调试用）。 */
    public int countIn(Block block) {
        return readKeys(block).size();
    }

    public void spawnHologram(Block block) {
        removeHologram(block);
        Location location = block.getLocation().add(0.5, 1.25, 0.5);
        String text = plugin.getConfig().getString("develop.hologram-text",
                "<gradient:#22d3a8:#facc15>开发台</gradient> <gray>· 右键研发");
        block.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(Messages.of(text));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(true);
            display.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    public void removeHologram(Block block) {
        Location center = block.getLocation().add(0.5, 1.25, 0.5);
        for (Entity entity : block.getWorld().getNearbyEntities(center, 1.5, 2.0, 1.5)) {
            if (entity instanceof TextDisplay
                    && entity.getPersistentDataContainer().has(hologramKey, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    private Set<String> readKeys(Block block) {
        Set<String> result = new LinkedHashSet<>();
        String raw = block.getChunk().getPersistentDataContainer().get(tableKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(SEPARATOR)) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    private void writeKeys(Block block, Set<String> keys) {
        if (keys.isEmpty()) {
            block.getChunk().getPersistentDataContainer().remove(tableKey);
        } else {
            block.getChunk().getPersistentDataContainer()
                    .set(tableKey, PersistentDataType.STRING, String.join(SEPARATOR, keys));
        }
    }

    private String positionKey(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
