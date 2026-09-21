// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/** 把 ego.yml 的套装属性按当前防具件数 y 动态同步到玩家。 */
public final class EgoSetBonusService {

    private static final String HOLY = "holy";
    private static final int EFFECT_REFRESH_TICKS = 60;

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
    private final Map<UUID, HolyShieldState> holyShields = new HashMap<>();
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
            clearHolyShield(player.getUniqueId());
        }
        holyShields.clear();
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
            clearHolyShield(player.getUniqueId());
            return;
        }

        EgoSetBonus bonus = definition.bonus();
        apply(player, Attribute.MAX_HEALTH, maxHealthKey, bonus.maxHealth(equipped.pieces()));
        apply(player, Attribute.ATTACK_SPEED, attackSpeedKey, bonus.attackSpeed(equipped.pieces()));
        apply(player, Attribute.ATTACK_DAMAGE, attackDamageKey, bonus.attackDamage(equipped.pieces()));
        applyPotion(player, PotionEffectType.REGENERATION, bonus.regenerationLevel(equipped.pieces()));
        applyPotion(player, PotionEffectType.RESISTANCE, bonus.resistanceLevel(equipped.pieces()));
        applyPotion(player, PotionEffectType.STRENGTH, bonus.strengthLevel(equipped.pieces()));
        applyPotion(player, PotionEffectType.FIRE_RESISTANCE, bonus.fireResistanceLevel(equipped.pieces()));
        applyPotion(player, PotionEffectType.WATER_BREATHING, bonus.waterBreathingLevel(equipped.pieces()));
        reconcileHolyShield(player, definition.id(), bonus, equipped.pieces());
    }

    public void removeModifiers(Player player) {
        remove(player, Attribute.MAX_HEALTH, maxHealthKey);
        remove(player, Attribute.ATTACK_SPEED, attackSpeedKey);
        remove(player, Attribute.ATTACK_DAMAGE, attackDamageKey);
    }

    /** 结算一次神圣黄盾。伤害超过剩余盾量时盾会破碎，但本次伤害不会穿透。 */
    public HolyShieldResult absorbWithHolyShield(Player player, double damage) {
        if (player == null || !Double.isFinite(damage) || damage <= 0.0D) {
            return HolyShieldResult.NONE;
        }
        HolyShieldState state = holyShields.get(player.getUniqueId());
        if (state == null || state.amount <= 0.0D) {
            return HolyShieldResult.NONE;
        }
        EgoEquipped equipped = plugin.ego().resolve(player);
        EgoSetDefinition definition = equipped.active() ? plugin.ego().set(equipped.setId()) : null;
        if (definition == null || !definition.bonus().hasSkill(HOLY, equipped.pieces())) {
            clearHolyShield(player.getUniqueId());
            return HolyShieldResult.NONE;
        }
        HolyShieldResult result;
        if (damage <= state.amount) {
            state.amount = Math.max(0.0D, state.amount - damage);
            result = HolyShieldResult.ABSORBED;
        } else {
            state.amount = 0.0D;
            result = HolyShieldResult.BROKEN;
        }
        double maxCharge = definition.bonus().skillMaxCharge();
        renderShield(player, state, maxCharge);
        return result;
    }

    public double holyShield(Player player) {
        HolyShieldState state = player == null ? null : holyShields.get(player.getUniqueId());
        return state == null ? 0.0D : state.amount;
    }

    private void reconcileHolyShield(
            Player player,
            String setId,
            EgoSetBonus bonus,
            int pieces) {
        UUID playerId = player.getUniqueId();
        if (!bonus.hasSkill(HOLY, pieces) || bonus.skillMaxCharge() <= 0.0D) {
            clearHolyShield(playerId);
            return;
        }

        long now = System.currentTimeMillis();
        HolyShieldState state = holyShields.get(playerId);
        if (state == null || !setId.equals(state.setId)) {
            double initialAmount = Math.min(
                    bonus.skillMaxCharge(),
                    bonus.skillChargePerActivation());
            state = new HolyShieldState(setId, initialAmount, now + shieldIntervalMillis(bonus), now);
            holyShields.put(playerId, state);
        } else if (now - state.lastSeenAt > 3000L) {
            state.nextChargeAt = now + shieldIntervalMillis(bonus);
        }
        state.lastSeenAt = now;

        long interval = shieldIntervalMillis(bonus);
        if (interval > 0L) {
            int guard = 0;
            while (now >= state.nextChargeAt
                    && state.amount < bonus.skillMaxCharge()
                    && guard++ < 128) {
                state.amount = Math.min(
                        bonus.skillMaxCharge(),
                        state.amount + bonus.skillChargePerActivation());
                state.nextChargeAt += interval;
            }
            if (state.amount >= bonus.skillMaxCharge() && now >= state.nextChargeAt) {
                state.nextChargeAt = now + interval;
            }
        }
        renderShield(player, state, bonus.skillMaxCharge());
    }

    private long shieldIntervalMillis(EgoSetBonus bonus) {
        return Math.max(1, bonus.skillCooldownSeconds()) * 1000L;
    }

    private void renderShield(Player player, HolyShieldState state, double maxCharge) {
        double amount = Math.max(0.0D, state.amount);
        // Damageable#setAbsorptionAmount 可以保留半颗心的精度，比 ABSORPTION 药水效果更准确。
        player.setAbsorptionAmount(amount);

        long now = System.currentTimeMillis();
        boolean changed = Math.abs(state.lastDisplayedAmount - amount) > 1.0E-6D;
        boolean refreshDue = state.lastDisplayedAt == 0L || now - state.lastDisplayedAt >= 5000L;
        if (amount <= 0.0D) {
            if (changed) {
                state.lastDisplayedAmount = 0.0D;
                state.lastDisplayedAt = now;
                Messages.actionBar(player, "<gold>神圣黄盾 <red>已破碎");
            }
            return;
        }
        if (!changed && !refreshDue) {
            return;
        }
        state.lastDisplayedAmount = amount;
        state.lastDisplayedAt = now;
        Messages.actionBar(player, "<gold>神圣黄盾 <yellow>" + formatAmount(amount)
                + "</yellow><gray>/" + formatAmount(maxCharge));
    }

    private String formatAmount(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1.0E-6D) {
            return Long.toString(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    private void clearHolyShield(UUID playerId) {
        if (holyShields.remove(playerId) == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.setAbsorptionAmount(0.0D);
        }
    }

    private void applyPotion(Player player, PotionEffectType type, int level) {
        if (level <= 0) {
            return;
        }
        int amplifier = level - 1;
        PotionEffect current = player.getPotionEffect(type);
        if (current != null
                && current.getAmplifier() >= amplifier
                && current.getDuration() > EFFECT_REFRESH_TICKS / 2) {
            return;
        }
        player.addPotionEffect(new PotionEffect(
                type,
                EFFECT_REFRESH_TICKS,
                amplifier,
                false,
                false,
                false));
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

    public enum HolyShieldResult {
        NONE,
        ABSORBED,
        BROKEN
    }

    private static final class HolyShieldState {
        private final String setId;
        private double amount;
        private long nextChargeAt;
        private long lastSeenAt;
        private double lastDisplayedAmount;
        private long lastDisplayedAt;

        private HolyShieldState(String setId, double amount, long nextChargeAt, long lastSeenAt) {
            this.setId = setId;
            this.amount = amount;
            this.nextChargeAt = nextChargeAt;
            this.lastSeenAt = lastSeenAt;
        }
    }
}
