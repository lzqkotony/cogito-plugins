// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** 把 ego.yml 的套装属性按当前防具件数 y 动态同步到玩家。 */
public final class EgoSetBonusService {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final CogitoPlugin plugin;
    private final NamespacedKey maxHealthKey;
    private final NamespacedKey attackSpeedKey;
    private final NamespacedKey attackDamageKey;
    private BukkitTask task;

    public EgoSetBonusService(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.maxHealthKey = new NamespacedKey(plugin, "ego_set_max_health");
        this.attackSpeedKey = new NamespacedKey(plugin, "ego_set_attack_speed");
        this.attackDamageKey = new NamespacedKey(plugin, "ego_set_attack_damage");
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                reconcile(player);
            }
        }, 1L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeModifiers(player);
        }
    }

    /** 立即刷新一个玩家，装备变化或上线时可直接调用。 */
    public void reconcile(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        enforceOpOnly(player);

        EgoEquipped equipped = plugin.ego().resolve(player);
        EgoSetDefinition definition = equipped.active() ? plugin.ego().set(equipped.setId()) : null;
        if (definition == null) {
            removeModifiers(player);
            return;
        }

        EgoSetBonus bonus = definition.bonus();
        apply(player, Attribute.MAX_HEALTH, maxHealthKey, bonus.maxHealth(equipped.pieces()));
        apply(player, Attribute.ATTACK_SPEED, attackSpeedKey, bonus.attackSpeed(equipped.pieces()));
        apply(player, Attribute.ATTACK_DAMAGE, attackDamageKey, bonus.attackDamage(equipped.pieces()));
    }

    public void removeModifiers(Player player) {
        remove(player, Attribute.MAX_HEALTH, maxHealthKey);
        remove(player, Attribute.ATTACK_SPEED, attackSpeedKey);
        remove(player, Attribute.ATTACK_DAMAGE, attackDamageKey);
    }

    private void enforceOpOnly(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getInventory().getItem(slot);
            CustomItem item = plugin.items().identify(stack);
            if (item == null || item.egoSetId() == null || item.egoPiece() == null || !item.egoPiece().armor()) {
                continue;
            }
            EgoSetDefinition definition = plugin.ego().set(item.egoSetId());
            if (definition == null || !definition.opOnly() || player.isOp()) {
                continue;
            }
            player.getInventory().setItem(slot, null);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
            Messages.send(player, "<red>只有 OP 才能穿戴 <white>" + definition.displayName() + "</white>");
        }
    }

    private void apply(Player player, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(key);
        if (Math.abs(amount) > 1.0E-9D) {
            instance.addModifier(new AttributeModifier(
                    key,
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY));
        }
    }

    private void remove(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(key);
        }
    }
}
