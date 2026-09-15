package com.seewo.cogito.item;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 脑啡肽物品工厂。
 *
 * <p>物品 = 配置里的基础材质（默认附魔绿宝石块）+ 自定义名称/Lore + <b>PersistentDataContainer 标签</b>。
 * 标签是唯一的身份凭据：改名、重新附魔都不影响识别，玩家也无法伪造。
 */
public final class EnkephalinItem {

    private final NamespacedKey tagKey;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Enchantment enchantment;
    private final int enchantmentLevel;
    private final boolean hideEnchants;
    private final boolean glint;
    private final int customModelData;

    public EnkephalinItem(CogitoPlugin plugin) {
        var config = plugin.getConfig();

        this.tagKey = new NamespacedKey(plugin, config.getString("enkephalin.tag", "enkephalin"));

        Material parsed = Material.matchMaterial(config.getString("enkephalin.material", "EMERALD_BLOCK"));
        if (parsed == null) {
            plugin.getLogger().warning("config.yml 里的 enkephalin.material 无效，回退到 EMERALD_BLOCK");
            parsed = Material.EMERALD_BLOCK;
        }
        this.material = parsed;

        this.displayName = config.getString("enkephalin.display-name", "<aqua>脑啡肽");
        this.lore = config.getStringList("enkephalin.lore");
        this.enchantment = resolveEnchantment(plugin, config.getString("enkephalin.enchantment", ""));
        this.enchantmentLevel = Math.max(1, config.getInt("enkephalin.enchantment-level", 1));
        this.hideEnchants = config.getBoolean("enkephalin.hide-enchants", true);
        this.glint = config.getBoolean("enkephalin.glint", true);
        this.customModelData = config.getInt("enkephalin.custom-model-data", 0);
    }

    private static Enchantment resolveEnchantment(CogitoPlugin plugin, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).trim().replace(' ', '_'));
        Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            plugin.getLogger().warning("未知附魔 " + name + "，脑啡肽将不带附魔光效");
        }
        return enchantment;
    }

    /** 造一个脑啡肽物品（数量会被裁剪到单堆上限）。 */
    public ItemStack create(int amount) {
        int size = Math.max(1, Math.min(material.getMaxStackSize(), amount));
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
        meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);

        stack.setItemMeta(meta);
        return stack;
    }

    /** 判断一个物品是不是脑啡肽。 */
    public boolean isEnkephalin(ItemStack stack) {
        if (stack == null || stack.getType() != material) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte tag = meta.getPersistentDataContainer().get(tagKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }

    /** 统计玩家身上（含护甲/副手槽）的脑啡肽总数。 */
    public int count(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isEnkephalin(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** 从玩家身上扣除脑啡肽，返回实际扣除数量（不够就扣多少算多少）。 */
    public int remove(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;
        int removed = 0;

        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isEnkephalin(item)) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            int left = item.getAmount() - take;
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(left);
                inventory.setItem(slot, item);
            }
            remaining -= take;
            removed += take;
        }
        return removed;
    }

    /** 给玩家脑啡肽；背包放不下就掉在脚下。 */
    public void give(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        int perStack = Math.max(1, material.getMaxStackSize());
        while (remaining > 0) {
            int size = Math.min(perStack, remaining);
            stacks.add(create(size));
            remaining -= size;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) {
            Location location = player.getLocation();
            for (ItemStack stack : leftover.values()) {
                player.getWorld().dropItemNaturally(location, stack);
            }
            Messages.send(player, "<yellow>背包放不下，多出来的脑啡肽掉在你脚下了");
        }
    }

    public NamespacedKey tagKey() {
        return tagKey;
    }

    public Material material() {
        return material;
    }

    public Enchantment enchantment() {
        return enchantment;
    }

    public int customModelData() {
        return customModelData;
    }
}
