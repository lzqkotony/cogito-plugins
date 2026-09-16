// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.item;

import com.seewo.cogito.CogitoPlugin;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/** 物品注册表：从 items.yml 读取所有 Cogito 物品定义。 */
public final class ItemRegistry {

    private final CogitoPlugin plugin;
    private final NamespacedKey itemKey;
    private final Map<String, CustomItem> items = new LinkedHashMap<>();
    private final Map<String, String> aliasToId = new HashMap<>();

    public ItemRegistry(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "item");
    }

    /** 读取（或首次释放）items.yml。 */
    public void load() {
        items.clear();
        aliasToId.clear();

        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("items");
        if (root == null) {
            plugin.getLogger().warning("items.yml 里没有 items 段落，物品注册表为空");
            return;
        }

        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) {
                continue;
            }
            String id = rawId.toLowerCase(Locale.ROOT);
            CustomItem item = new CustomItem(plugin, itemKey, id, section);
            items.put(id, item);
            for (String alias : section.getStringList("aliases")) {
                if (!alias.isBlank()) {
                    aliasToId.put(alias.toLowerCase(Locale.ROOT), id);
                }
            }
        }
        plugin.getLogger().info("已注册 " + items.size() + " 个物品：" + String.join(", ", items.keySet()));
    }

    /** 把别处（例如 ego.yml）生成的物品加入统一注册表。 */
    public void register(CustomItem item) {
        if (item == null || item.id() == null || item.id().isBlank()) {
            return;
        }
        items.put(item.id().toLowerCase(Locale.ROOT), item);
    }

    /** 统一物品 NBT 标签键。 */
    public NamespacedKey itemKey() {
        return itemKey;
    }

    /** 按 id 或别名查物品（大小写不敏感）。 */
    public CustomItem find(String idOrAlias) {
        if (idOrAlias == null) {
            return null;
        }
        String key = idOrAlias.toLowerCase(Locale.ROOT);
        CustomItem direct = items.get(key);
        if (direct != null) {
            return direct;
        }
        String mapped = aliasToId.get(key);
        return mapped == null ? null : items.get(mapped);
    }

    /** 识别一个物品属于哪个注册物品；不是 Cogito 物品就返回 null。 */
    public CustomItem identify(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        for (CustomItem item : items.values()) {
            if (item.matches(stack)) {
                return item;
            }
        }
        return null;
    }

    public Collection<CustomItem> all() {
        return items.values();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
