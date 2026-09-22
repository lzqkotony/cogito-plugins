// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private int itemCount;

    public EgoRegistry(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    /** 读取或释放 ego.yml，并把生成的 E.G.O. 物品注册进统一物品表。 */
    public void load() {
        sets.clear();
        aliases.clear();
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
            String setId = normalizeId(rawSetId);
            String displayName = setSection.getString("display-name", "<white>" + rawSetId);
            double resistance = setSection.getDouble("armor-resistance", 1.0D);
            if (!Double.isFinite(resistance)) {
                plugin.getLogger().warning("ego.yml: " + setId + " 的 armor-resistance 不是有限数字，已跳过");
                continue;
            }

            boolean developable = setSection.getBoolean("developable", true);
            boolean hidden = setSection.getBoolean("hidden", false);
            boolean opOnly = "op".equalsIgnoreCase(setSection.getString("wear-requirement", ""));
            Map<EgoDevelopCategory, Integer> costs = readDevelopCosts(setSection, developable);
            EgoSetBonus bonus = readSetBonus(setSection);
            String blueprintId = developable ? "blueprint-" + setId : null;

            EgoSetDefinition definition = new EgoSetDefinition(
                    setId,
                    displayName,
                    resistance,
                    developable,
                    hidden,
                    opOnly,
                    costs,
                    bonus,
                    blueprintId,
                    new LinkedHashSet<>(setSection.getStringList("acquisition-whitelist")));
            sets.put(setId, definition);
            registerAliases(setId, setSection.getStringList("aliases"));

            loadArmor(setId, setSection.getConfigurationSection("armor"));
            loadItems(setId, setSection.getConfigurationSection("weapons"), EgoPiece.WEAPON);
            loadItems(setId, setSection.getConfigurationSection("accessories"), EgoPiece.ACCESSORY);
            if (developable) {
                registerBlueprint(setId, displayName, setSection);
            }
        }

        plugin.getLogger().info("已注册 " + sets.size() + " 套 E.G.O.，共 " + itemCount + " 件物品");
    }

    private Map<EgoDevelopCategory, Integer> readDevelopCosts(
            ConfigurationSection setSection,
            boolean developable) {
        Map<EgoDevelopCategory, Integer> result = new EnumMap<>(EgoDevelopCategory.class);
        if (!developable) {
            return result;
        }
        ConfigurationSection develop = setSection.getConfigurationSection("develop");
        if (develop == null) {
            return result;
        }

        for (EgoDevelopCategory category : EgoDevelopCategory.values()) {
            String key = category.name().toLowerCase(Locale.ROOT);
            ConfigurationSection child = develop.getConfigurationSection(key);
            if (child != null && child.contains("cost")) {
                result.put(category, Math.max(0, child.getInt("cost")));
            } else if (develop.contains(key) && develop.isInt(key)) {
                result.put(category, Math.max(0, develop.getInt(key)));
            }
        }

        // 0.5.0 及以前的 develop.cost 表示整套一个价，向后兼容为护甲/武器同价。
        int legacyCost = develop.getInt("cost", -1);
        if (result.isEmpty() && legacyCost >= 0) {
            result.put(EgoDevelopCategory.ARMOR, legacyCost);
            result.put(EgoDevelopCategory.WEAPON, legacyCost);
        }
        return result;
    }

    private EgoSetBonus readSetBonus(ConfigurationSection setSection) {
        ConfigurationSection section = setSection.getConfigurationSection("set-bonuses");
        if (section == null) {
            return EgoSetBonus.none();
        }
        return new EgoSetBonus(
                section.getDouble("max-health-per-piece", 0.0D),
                readThresholds(section.getConfigurationSection("attack-speed")),
                readThresholds(section.getConfigurationSection("attack-damage")),
                readLevels(section.getConfigurationSection("regeneration-level")),
                readLevels(section.getConfigurationSection("resistance-level")),
                readLevels(section.getConfigurationSection("strength-level")),
                readLevels(section.getConfigurationSection("fire-resistance-level")),
                readLevels(section.getConfigurationSection("water-breathing-level")),
                section.getString("skill", ""),
                section.getInt("skill-cooldown-seconds", 0),
                section.getDouble("skill-charge-per-activation", 0.0D),
                section.getDouble("skill-max-charge", 0.0D),
                section.getDouble("damage-immunity-threshold", 0.0D));
    }

    private NavigableMap<Integer, Double> readThresholds(ConfigurationSection section) {
        NavigableMap<Integer, Double> result = new TreeMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            try {
                int pieces = Integer.parseInt(key);
                double value = section.getDouble(key);
                if (pieces > 0 && pieces <= 4 && Double.isFinite(value)) {
                    result.put(pieces, value);
                }
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("ego.yml: set-bonuses 的件数键无效：" + key);
            }
        }
        return result;
    }

    private NavigableMap<Integer, Integer> readLevels(ConfigurationSection section) {
        NavigableMap<Integer, Integer> result = new TreeMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            try {
                int pieces = Integer.parseInt(key);
                int value = section.getInt(key);
                if (pieces > 0 && pieces <= 4 && value > 0) {
                    result.put(pieces, value);
                }
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("ego.yml: set-bonuses 的等级键无效：" + key);
            }
        }
        return result;
    }

    private void registerAliases(String setId, List<String> rawAliases) {
        for (String raw : rawAliases) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            aliases.put(normalizeId(raw), setId);
        }
        // 兼容设计稿中 server_owner / server-owner 两种写法。
        if (setId.equals("server-owner")) {
            aliases.put("sever-owner", setId);
            aliases.put("owner", setId);
        }
    }

    /**
     * 为每套可研发 E.G.O. 注册一张"研发图纸"。
     *
     * <p>0.5.1 起图纸是一次性装载物：右键或 GUI 装载后消耗一张，把该套研发能力绑定到玩家。
     */
    private void registerBlueprint(String setId, String setDisplayName, ConfigurationSection setSection) {
        ConfigurationSection blueprint = setSection.getConfigurationSection("develop.blueprint");
        YamlConfiguration definition = new YamlConfiguration();
        definition.set("material", blueprint == null ? "PAPER" : blueprint.getString("material", "PAPER"));
        definition.set("display-name", blueprint == null
                ? "<gradient:#facc15:#a855f7>E.G.O. 研发图纸</gradient> <gray>· " + setDisplayName
                : blueprint.getString("display-name"));
        definition.set("lore", blueprint == null
                ? List.of(
                        "<gray>装载后解锁「" + setDisplayName + "<gray>」的研发能力",
                        "<dark_gray>一次性装载物 · 绑定到当前玩家",
                        "<dark_gray>右键装载，或在开发台选择「装载蓝图」")
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
            if (isFunctionalElytra(slot, stack.getType() == Material.ELYTRA, item)) {
                // 鞘翅是功能性装备：允许占用胸甲槽，但不计入 E.G.O. 件数 y。
                continue;
            }
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

    static boolean isFunctionalElytra(EquipmentSlot slot, boolean isElytra, CustomItem item) {
        return slot == EquipmentSlot.CHEST
                && isElytra
                && (item == null || item.egoSetId() == null);
    }

    /** 玩家是否穿着指定套装的任意一件防具；用于彩蛋保护、OP 穿戴限制等无视混搭的判定。 */
    public boolean wearsAnyArmorPiece(Player player, String setId) {
        if (player == null || setId == null) {
            return false;
        }
        String normalized = normalizeId(setId);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            CustomItem item = plugin.items().identify(player.getInventory().getItem(slot));
            if (item != null
                    && item.egoPiece() != null
                    && item.egoPiece().armor()
                    && normalized.equals(item.egoSetId())) {
                return true;
            }
        }
        return false;
    }

    /** 手持武器如果是 E.G.O. 武器，返回它的定义；否则返回 null。 */
    public CustomItem weapon(ItemStack stack) {
        CustomItem item = plugin.items().identify(stack);
        if (item == null || item.egoPiece() != EgoPiece.WEAPON) {
            return null;
        }
        return item;
    }

    /** 检查玩家是否允许获取某个 E.G.O. 物品；空白名单表示不限制。 */
    public boolean canAcquire(Player player, CustomItem item) {
        if (player == null || item == null || item.egoSetId() == null) {
            return true;
        }
        EgoSetDefinition definition = set(item.egoSetId());
        return definition == null || canAcquire(player, definition);
    }

    /** 检查玩家是否在白名单内；白名单支持玩家名、BE_ 名称或 UUID。 */
    public boolean canAcquire(Player player, EgoSetDefinition definition) {
        return player != null
                && definition != null
                && definition.canBeAcquiredBy(player.getName(), player.getUniqueId());
    }

    /** 清理不在白名单内的受限套装物品；用于登录和防止通过掉落/容器绕过。 */
    public int purgeRestrictedItems(Player player) {
        if (player == null) {
            return 0;
        }
        int removed = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            removed += purgeSlot(inventory, slot, player);
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.OFF_HAND}) {
            removed += purgeSlot(inventory, slot, player);
        }
        for (int slot = 0; slot < player.getEnderChest().getSize(); slot++) {
            removed += purgeSlot(player.getEnderChest(), slot, player);
        }
        return removed;
    }

    private int purgeSlot(org.bukkit.inventory.Inventory inventory, int slot, Player player) {
        ItemStack current = inventory.getItem(slot);
        CustomItem item = plugin.items().identify(current);
        if (item == null || canAcquire(player, item)) {
            return 0;
        }
        inventory.setItem(slot, null);
        return 1;
    }

    private int purgeSlot(PlayerInventory inventory, EquipmentSlot slot, Player player) {
        ItemStack current = inventory.getItem(slot);
        CustomItem item = plugin.items().identify(current);
        if (item == null || canAcquire(player, item)) {
            return 0;
        }
        inventory.setItem(slot, null);
        return 1;
    }

    /** 蓝伤只允许由持有 BLUE E.G.O. 武器的玩家作为来源。 */
    public boolean canDealBlue(Player player) {
        CustomItem weapon = weapon(player.getInventory().getItemInMainHand());
        return weapon != null && weapon.damageChannel() == DamageChannel.BLUE;
    }

    public Collection<EgoSetDefinition> sets() {
        return sets.values();
    }

    /** 开发台展示的套装，隐藏彩蛋不会出现在列表里。 */
    public Collection<EgoSetDefinition> developableSets() {
        return sets.values().stream()
                .filter(EgoSetDefinition::developable)
                .filter(set -> !set.hidden())
                .toList();
    }

    /** 某套 E.G.O. 当前注册的全部物品（防具、武器、饰品）。 */
    public Collection<CustomItem> itemsForSet(String id) {
        EgoSetDefinition definition = set(id);
        if (definition == null) {
            return List.of();
        }
        String normalized = definition.id();
        return plugin.items().all().stream()
                .filter(item -> normalized.equals(item.egoSetId()))
                .toList();
    }

    /** 某套 E.G.O. 指定分类的物品。 */
    public Collection<CustomItem> itemsForSet(String id, EgoDevelopCategory category) {
        if (category == null) {
            return List.of();
        }
        List<CustomItem> result = new ArrayList<>();
        for (CustomItem item : itemsForSet(id)) {
            if (category.contains(item.egoPiece())) {
                result.add(item);
            }
        }
        return result;
    }

    public EgoSetDefinition set(String id) {
        if (id == null) {
            return null;
        }
        String normalized = normalizeId(id);
        EgoSetDefinition direct = sets.get(normalized);
        if (direct != null) {
            return direct;
        }
        String canonical = aliases.get(normalized);
        return canonical == null ? null : sets.get(canonical);
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
