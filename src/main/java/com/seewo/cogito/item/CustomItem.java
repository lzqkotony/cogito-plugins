// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.item;

import com.google.common.collect.ImmutableMultimap;
import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.DamageChannel;
import com.seewo.cogito.ego.EgoPiece;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

/**
 * 一个注册物品的定义（见 items.yml 与 ego.yml）。
 *
 * <p>身份靠 NBT 标签：新物品写 {@code cogito:item = <id>}；
 * E.G.O. 物品另外写 set / piece / damage_channel，且创建时就清除附魔与原版属性修饰符。
 * 为了不让服务器里已有的老物品失效，{@code legacy-tags} 里列出的标签也认（且不校验材质）。
 */
public final class CustomItem {

    private final String id;
    private final NamespacedKey itemKey;
    private final NamespacedKey egoSetKey;
    private final NamespacedKey egoPieceKey;
    private final NamespacedKey damageChannelKey;
    private final List<NamespacedKey> legacyKeys;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Enchantment enchantment;
    private final int enchantmentLevel;
    private final boolean hideEnchants;
    private final boolean glint;
    private final int customModelData;
    private final int stackSize;
    private final boolean stackable;
    private final boolean placeable;
    private final boolean craftable;
    private final PotionType potionType;
    private final String egoSetId;
    private final EgoPiece egoPiece;
    private final DamageChannel damageChannel;
    private final boolean enchantable;
    private final boolean unbreakable;
    private final boolean removeVanillaAttributes;

    public CustomItem(CogitoPlugin plugin, NamespacedKey itemKey, String id, ConfigurationSection section) {
        this(plugin, itemKey, id, section, null, null, null);
    }

    public CustomItem(
            CogitoPlugin plugin,
            NamespacedKey itemKey,
            String id,
            ConfigurationSection section,
            String egoSetId,
            EgoPiece egoPiece,
            DamageChannel damageChannel) {
        this.id = id;
        this.itemKey = itemKey;
        this.egoSetKey = new NamespacedKey(plugin, "ego_set");
        this.egoPieceKey = new NamespacedKey(plugin, "ego_piece");
        this.damageChannelKey = new NamespacedKey(plugin, "damage_channel");
        this.egoSetId = egoSetId == null ? null : egoSetId.toLowerCase(Locale.ROOT);
        this.egoPiece = egoPiece;
        this.damageChannel = damageChannel;

        Material parsed = Material.matchMaterial(String.valueOf(section.getString("material", "STONE")));
        if (parsed == null) {
            plugin.getLogger().warning("items.yml: " + id + " 的 material 无效，回退到 STONE");
            parsed = Material.STONE;
        }
        this.material = parsed;

        this.displayName = section.getString("display-name", "<white>" + id);
        this.lore = section.getStringList("lore");
        Enchantment parsedEnchantment = resolveEnchantment(plugin, id, section.getString("enchantment", ""));
        this.enchantment = this.egoSetId == null ? parsedEnchantment : null;
        this.enchantmentLevel = Math.max(1, section.getInt("enchantment-level", 1));
        this.hideEnchants = section.getBoolean("hide-enchants", true);
        this.glint = section.getBoolean("glint", true);
        this.customModelData = section.getInt("custom-model-data", 0);
        this.stackSize = Math.max(0, section.getInt("stack-size", 0));
        this.stackable = this.egoSetId == null && section.getBoolean("stackable", true);
        this.placeable = section.getBoolean("placeable", false);
        this.craftable = section.getBoolean("craftable", false);
        this.potionType = resolvePotionType(plugin, id, section.getString("potion-type", ""));
        this.enchantable = section.getBoolean("enchantable", this.egoSetId == null);
        this.unbreakable = section.getBoolean("unbreakable", this.egoSetId != null);
        this.removeVanillaAttributes = section.getBoolean("remove-vanilla-attributes", this.egoSetId != null);

        this.legacyKeys = new ArrayList<>();
        for (String legacy : section.getStringList("legacy-tags")) {
            if (!legacy.isBlank()) {
                legacyKeys.add(new NamespacedKey(plugin, legacy.toLowerCase(Locale.ROOT).trim()));
            }
        }
    }

    private static Enchantment resolveEnchantment(CogitoPlugin plugin, String id, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Enchantment enchantment = Registry.ENCHANTMENT.get(key(name));
        if (enchantment == null) {
            plugin.getLogger().warning("items.yml: " + id + " 的附魔 " + name + " 无效，已忽略");
        }
        return enchantment;
    }

    private static PotionType resolvePotionType(CogitoPlugin plugin, String id, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        PotionType type = Registry.POTION.get(key(name));
        if (type == null) {
            plugin.getLogger().warning("items.yml: " + id + " 的药水类型 " + name + " 无效，已忽略");
        }
        return type;
    }

    private static NamespacedKey key(String name) {
        return NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_'));
    }

    /** 造一个该物品（数量会按它自己的堆叠上限裁剪）。 */
    public ItemStack create(int amount) {
        int limit = maxStackSize();
        int size = Math.max(1, Math.min(limit, amount));
        ItemStack stack = new ItemStack(material, size);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.displayName(Messages.of(displayName));
        if (!lore.isEmpty()) {
            List<Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(Messages.of(line));
            }
            meta.lore(lines);
        }
        if (potionType != null && meta instanceof PotionMeta potionMeta) {
            potionMeta.setBasePotionType(potionType);
        }
        if (enchantment != null) {
            meta.addEnchant(enchantment, enchantmentLevel, true);
        }
        if (hideEnchants && enchantment != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (glint) {
            meta.setEnchantmentGlintOverride(Boolean.TRUE);
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        if (egoSetId != null) {
            meta.removeEnchantments();
            meta.setUnbreakable(unbreakable);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (unbreakable) {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
            if (removeVanillaAttributes) {
                // 空 multimap 会覆盖材质自带的护甲值 / 韧性属性。
                meta.setAttributeModifiers(ImmutableMultimap.of());
            }
            meta.getPersistentDataContainer().set(egoSetKey, PersistentDataType.STRING, egoSetId);
            if (egoPiece != null) {
                meta.getPersistentDataContainer().set(egoPieceKey, PersistentDataType.STRING, egoPiece.name());
            }
            if (damageChannel != null) {
                meta.getPersistentDataContainer().set(
                        damageChannelKey, PersistentDataType.STRING, damageChannel.name());
            }
        }

        // 只有需要改动原版堆叠上限时才写这个组件（药水默认 1，写 64 才能堆叠）
        if (limit != Math.max(1, material.getMaxStackSize())) {
            meta.setMaxStackSize(limit);
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);

        stack.setItemMeta(meta);
        return stack;
    }

    /** 判断一个物品是不是本物品。 */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        // 旧标签优先且不校验材质：老版本用别的材质做过同一种物品也能认出来
        for (NamespacedKey legacy : legacyKeys) {
            Byte tag = meta.getPersistentDataContainer().get(legacy, PersistentDataType.BYTE);
            if (tag != null && tag == (byte) 1) {
                return true;
            }
        }
        if (stack.getType() != material) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return id.equals(value);
    }

    /** 这个物品实际能堆多少。 */
    public int maxStackSize() {
        if (!stackable) {
            return 1;
        }
        int natural = Math.max(1, material.getMaxStackSize());
        return stackSize > 0 ? Math.min(64, stackSize) : natural;
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public Enchantment enchantment() {
        return enchantment;
    }

    public int customModelData() {
        return customModelData;
    }

    public boolean placeable() {
        return placeable;
    }

    public boolean craftable() {
        return craftable;
    }

    public String egoSetId() {
        return egoSetId;
    }

    public EgoPiece egoPiece() {
        return egoPiece;
    }

    public DamageChannel damageChannel() {
        return damageChannel;
    }

    public boolean enchantable() {
        return enchantable;
    }

    public boolean unbreakable() {
        return unbreakable;
    }

    public boolean removeVanillaAttributes() {
        return removeVanillaAttributes;
    }
}
