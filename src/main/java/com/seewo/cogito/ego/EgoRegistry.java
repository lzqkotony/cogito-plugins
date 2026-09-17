// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** E.G.O. 配置与防具/武器识别。 */
public final class EgoRegistry {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final CogitoPlugin plugin;
    private final Map<String, EgoSetDefinition> sets = new LinkedHashMap<>();
    private int itemCount;

    public EgoRegistry(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    /** 读取或释放 ego.yml，并把生成的 E.G.O. 物品注册进统一物品表。 */
    public void load() {
        sets.clear();
        itemCount = 0;

        File file = new File(plugin.getDataFolder(), "ego.yml");
        if (!file.exists()) {
            plugin.saveResource("ego.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", true)) {
            plugin.getLogger().info("E.G.O. 系统已关闭（ego.yml: enabled: false）");
            return;
        }

        ConfigurationSection root = config.getConfigurationSection("sets");
        if (root == null) {
            plugin.getLogger().warning("ego.yml 里没有 sets 段落，E.G.O. 注册表为空");
            return;
        }

        for (String rawSetId : root.getKeys(false)) {
            ConfigurationSection setSection = root.getConfigurationSection(rawSetId);
            if (setSection == null) {
                continue;
            }
            String setId = rawSetId.toLowerCase(Locale.ROOT);
            String displayName = setSection.getString("display-name", "<white>" + rawSetId);
            double resistance = setSection.getDouble("armor-resistance", 1.0D);
            if (!Double.isFinite(resistance)) {
                plugin.getLogger().warning("ego.yml: " + setId + " 的 armor-resistance 不是有限数字，已跳过");
                continue;
            }

            int developCost = Math.max(0, setSection.getInt("develop.cost", 64));
            String blueprintId = "blueprint-" + setId;
            sets.put(setId, new EgoSetDefinition(setId, displayName, resistance, developCost, blueprintId));
            loadArmor(setId, setSection.getConfigurationSection("armor"));
            loadItems(setId, setSection.getConfigurationSection("weapons"), EgoPiece.WEAPON);
            loadItems(setId, setSection.getConfigurationSection("accessories"), EgoPiece.ACCESSORY);
            registerBlueprint(setId, displayName, setSection);
        }

        plugin.getLogger().info("已注册 " + sets.size() + " 套 E.G.O.，共 " + itemCount + " 件物品");
    }

    /**
     * 为每套 E.G.O. 注册一张"研发图纸"。
     *
     * <p>图纸只负责**解锁**该套的研发能力（图纸本身会消耗掉），之后在开发台研发同一套不再需要图纸。
     * 物品 id 约定为 {@code blueprint-<套装id>}，右键使用即可解锁。
     */
    private void registerBlueprint(String setId, String setDisplayName, ConfigurationSection setSection) {
        ConfigurationSection blueprint = setSection.getConfigurationSection("develop.blueprint");
        org.bukkit.configuration.file.YamlConfiguration definition =
                new org.bukkit.configuration.file.YamlConfiguration();
        definition.set("material", blueprint == null ? "PAPER" : blueprint.getString("material", "PAPER"));
        definition.set("display-name", blueprint == null
                ? "<gradient:#facc15:#a855f7>E.G.O. 研发图纸</gradient> <gray>· " + setDisplayName
                : blueprint.getString("display-name"));
        definition.set("lore", blueprint == null
                ? java.util.List.of(
                        "<gray>解锁「" + setDisplayName + "<gray>」的研发能力",
                        "<dark_gray>专属图纸 · 不是消耗品",
                        "<dark_gray>右键解锁后仍留在手上")
                : blueprint.getStringList("lore"));
        definition.set("glint", blueprint == null || blueprint.getBoolean("glint", true));
        definition.set("stackable", true);
        definition.set("placeable", false);
        definition.set("craftable", false);

        plugin.items().register(new CustomItem(
                plugin, plugin.items().itemKey(), "blueprint-" + setId, definition));
        itemCount++;
    }

    private void loadArmor(String setId, ConfigurationSection armorSection) {
        if (armorSection == null) {
            return;
        }
        for (EgoPiece piece : new EgoPiece[]{
                EgoPiece.HELMET, EgoPiece.CHESTPLATE, EgoPiece.LEGGINGS, EgoPiece.BOOTS}) {
            ConfigurationSection section = armorSection.getConfigurationSection(piece.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                plugin.getLogger().warning("ego.yml: " + setId + " 缺少防具部位 " + piece.name());
                continue;
            }
            String itemId = setId + "-" + piece.name().toLowerCase(Locale.ROOT);
            register(itemId, section, setId, piece, null);
        }
    }

    private void loadItems(String setId, ConfigurationSection section, EgoPiece piece) {
        if (section == null) {
            return;
        }
        for (String rawName : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(rawName);
            if (itemSection == null) {
                continue;
            }
            DamageChannel channel = piece == EgoPiece.WEAPON
                    ? DamageChannel.parse(itemSection.getString("attack-channel", ""))
                    : null;
            if (piece == EgoPiece.WEAPON && channel == null) {
                plugin.getLogger().warning("ego.yml: " + setId + "/" + rawName
                        + " 的 attack-channel 无效，只允许 RED 或 BLUE");
                continue;
            }
            String itemId = setId + "-" + rawName.toLowerCase(Locale.ROOT);
            register(itemId, itemSection, setId, piece, channel);
        }
    }

    private void register(
            String itemId,
            ConfigurationSection section,
            String setId,
            EgoPiece piece,
            DamageChannel channel) {
        CustomItem item = new CustomItem(
                plugin,
                plugin.items().itemKey(),
                itemId,
                section,
                setId,
                piece,
                channel);
        plugin.items().register(item);
        itemCount++;
    }

    /** 统计玩家当前穿戴的 E.G.O. 防具，并计算统一抗性倍率。 */
    public EgoEquipped resolve(Player player) {
        String setId = null;
        int pieces = 0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            CustomItem item = plugin.items().identify(stack);
            EgoPiece expected = EgoPiece.forSlot(slot);
            if (item == null || item.egoPiece() != expected || item.egoSetId() == null) {
                return EgoEquipped.invalid(setId, pieces);
            }
            if (setId == null) {
                setId = item.egoSetId();
            } else if (!setId.equals(item.egoSetId())) {
                return EgoEquipped.invalid(setId, pieces + 1);
            }
            pieces++;
        }

        if (pieces == 0) {
            return EgoEquipped.none();
        }
        EgoSetDefinition definition = sets.get(setId);
        if (definition == null) {
            return EgoEquipped.invalid(setId, pieces);
        }
        double factor = EgoMath.resistanceFactor(definition.resistance(), pieces);
        return new EgoEquipped(
                true,
                false,
                definition.id(),
                definition.displayName(),
                pieces,
                definition.resistance(),
                factor);
    }

    /** 手持武器如果是 E.G.O. 武器，返回它的定义；否则返回 null。 */
    public CustomItem weapon(ItemStack stack) {
        CustomItem item = plugin.items().identify(stack);
        if (item == null || item.egoPiece() != EgoPiece.WEAPON) {
            return null;
        }
        return item;
    }

    /** 蓝伤只允许由持有 BLUE E.G.O. 武器的玩家作为来源。 */
    public boolean canDealBlue(Player player) {
        CustomItem weapon = weapon(player.getInventory().getItemInMainHand());
        return weapon != null && weapon.damageChannel() == DamageChannel.BLUE;
    }

    public Collection<EgoSetDefinition> sets() {
        return sets.values();
    }

    /** 某套 E.G.O. 当前注册的全部物品（防具、武器、饰品）。 */
    public Collection<CustomItem> itemsForSet(String id) {
        if (id == null) {
            return java.util.List.of();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return plugin.items().all().stream()
                .filter(item -> normalized.equals(item.egoSetId()))
                .toList();
    }

    public EgoSetDefinition set(String id) {
        return id == null ? null : sets.get(id.toLowerCase(Locale.ROOT));
    }
}
