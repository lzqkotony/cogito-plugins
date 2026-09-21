// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.DamageChannel;
import com.seewo.cogito.ego.EgoEquipped;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** 「拟态」三阶段、模仿层数和右键技能。 */
public final class MimicService implements Listener {

    public static final String SET_ID = "mimic";
    public static final String ABILITY_ID = "mimic";

    private static final int PHASE_ONE_DAMAGE = 7;
    private static final int PHASE_THREE_DAMAGE = 11;
    private static final int PHASE_TWO_TICKS = 20 * 15;
    private static final long PHASE_THREE_MILLIS = 15L * 60L * 1000L;
    private static final int MAX_STACKS = 20;
    private static final int SINGLE_SKILL_COST = 5;
    private static final double SINGLE_SKILL_DAMAGE = 35.0D;
    private static final double AREA_SKILL_BASE_DAMAGE = 65.0D;
    private static final double AREA_SKILL_PER_STACK = 2.0D;
    private static final double AREA_SKILL_RADIUS = 4.0D;

    private final CogitoPlugin plugin;
    private final NamespacedKey attackDamageKey;
    private final Map<UUID, MimicState> states = new ConcurrentHashMap<>();
    private final ThreadLocal<Integer> abilityDepth = ThreadLocal.withInitial(() -> 0);
    private BukkitTask task;

    public MimicService(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.attackDamageKey = new NamespacedKey(plugin, "mimic_phase_attack_damage");
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetState(player, false);
        }
        states.clear();
    }

    public boolean isWeapon(CustomItem item) {
        return item != null && item.ability() != null && item.ability().is(ABILITY_ID);
    }

    /** 拟态阶段覆盖统一抗性：II=0.05，III=0.2，其他阶段使用套装配置值。 */
    public double effectiveResistance(Player player, double configuredResistance) {
        if (player == null || !isFullSet(player)) {
            return configuredResistance;
        }
        MimicState state = states.get(player.getUniqueId());
        if (state == null) {
            return configuredResistance;
        }
        return switch (state.phase) {
            case TWO -> 0.05D;
            case THREE -> 0.2D;
            case ONE -> configuredResistance;
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isFullSet(player)) {
            return;
        }
        MimicState state = state(player);
        if (state.phase == MimicPhase.TWO) {
            return;
        }

        double remainingHealth = player.getHealth() + player.getAbsorptionAmount();
        if (event.getFinalDamage() >= remainingHealth) {
            enterPhaseTwo(player, state);
            event.setCancelled(true);
            return;
        }

        if (state.phase == MimicPhase.THREE && event.getFinalDamage() > 0.0D) {
            chanceAddStack(player, state);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (abilityDepth.get() > 0 || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        CustomItem weapon = plugin.items().identify(attacker.getInventory().getItemInMainHand());
        if (!isWeapon(weapon)) {
            return;
        }
        MimicState state = state(attacker);
        if (state.phase == MimicPhase.TWO || !isFullSet(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (event.getFinalDamage() <= 0.0D || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        double healing = state.phase == MimicPhase.THREE ? 3.0D : 2.0D;
        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + healing));
        if (state.phase == MimicPhase.THREE) {
            chanceAddStack(attacker, state);
        }
        spawnHitParticles(target);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (!isWeapon(weapon)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        MimicState state = state(player);
        if (state.phase != MimicPhase.THREE || !isFullSet(player)) {
            Messages.actionBar(player, "<gray>模仿能力仅在 <yellow>拟态 III</yellow> 阶段可用");
            return;
        }
        if (state.stacks >= 10) {
            useAreaSkill(player, state);
        } else if (state.stacks >= 5) {
            useSingleSkill(player, state);
        } else {
            Messages.actionBar(player, "<gray>模仿层数不足：需要至少 <yellow>5</yellow> 层");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        MimicState state = states.get(event.getPlayer().getUniqueId());
        if (state == null || state.phase != MimicPhase.TWO) {
            return;
        }
        event.setTo(event.getFrom());
        event.getPlayer().setVelocity(new Vector());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resetState(event.getPlayer(), false);
        states.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isFullSet(player)) {
                resetState(player, false);
                states.remove(player.getUniqueId());
                continue;
            }
            MimicState state = state(player);
            if (state.phase == MimicPhase.TWO && now >= state.phaseUntil) {
                enterPhaseThree(player, state);
            } else if (state.phase == MimicPhase.THREE && now >= state.phaseUntil) {
                resetState(player, true);
                states.remove(player.getUniqueId());
            }
            applyAttackModifier(player);
        }
    }

    private void enterPhaseTwo(Player player, MimicState state) {
        state.phase = MimicPhase.TWO;
        state.phaseUntil = System.currentTimeMillis() + PHASE_TWO_TICKS * 50L;
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PHASE_TWO_TICKS, 9, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PHASE_TWO_TICKS, 4, false, true, true));
        player.setVelocity(new Vector());
        applyAttackModifier(player);
        showState(player, state, "进入拟态 II · 15 秒后转为拟态 III");
    }

    private void enterPhaseThree(Player player, MimicState state) {
        clearPhaseTwoEffects(player);
        state.phase = MimicPhase.THREE;
        state.phaseUntil = System.currentTimeMillis() + PHASE_THREE_MILLIS;
        applyAttackModifier(player);
        showState(player, state, "进入拟态 III · 模仿层数保留");
    }

    private void resetState(Player player, boolean announce) {
        MimicState state = states.get(player.getUniqueId());
        clearPhaseTwoEffects(player);
        removeAttackModifier(player);
        if (state != null) {
            state.stacks = 0;
            state.phase = MimicPhase.ONE;
            state.phaseUntil = 0L;
            if (announce) {
                Messages.actionBar(player, "<gray>拟态回到 <yellow>拟态 I</yellow>，模仿层数已清空");
            }
        }
    }

    private void clearPhaseTwoEffects(Player player) {
        PotionEffect regeneration = player.getPotionEffect(PotionEffectType.REGENERATION);
        if (regeneration != null && regeneration.getAmplifier() >= 9) {
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }
        PotionEffect resistance = player.getPotionEffect(PotionEffectType.RESISTANCE);
        if (resistance != null && resistance.getAmplifier() >= 4) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
        }
    }

    private void useSingleSkill(Player player, MimicState state) {
        LivingEntity target = nearestTarget(player, 20.0D);
        if (target == null) {
            Messages.actionBar(player, "<gray>附近没有可攻击目标");
            return;
        }
        state.stacks -= SINGLE_SKILL_COST;
        withSuppressedAbility(() -> plugin.egoDamage().dealFlatDamage(
                player, target, SINGLE_SKILL_DAMAGE, DamageChannel.RED));
        spawnRedBurst(target.getLocation().add(0.0D, 1.0D, 0.0D), 0.8D, 26);
        showState(player, state, "模仿穿刺命中");
    }

    private void useAreaSkill(Player player, MimicState state) {
        int stacks = state.stacks;
        double damage = AREA_SKILL_BASE_DAMAGE + stacks * AREA_SKILL_PER_STACK;
        int hits = 0;
        Location center = player.getLocation().add(0.0D, 1.0D, 0.0D);
        for (Entity entity : player.getWorld().getNearbyEntities(
                center, AREA_SKILL_RADIUS, AREA_SKILL_RADIUS, AREA_SKILL_RADIUS)) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living instanceof ArmorStand
                    || !living.isValid()
                    || living.isDead()) {
                continue;
            }
            if (withSuppressedAbility(() -> plugin.egoDamage().dealFlatDamage(
                    player, living, damage, DamageChannel.RED))) {
                spawnRedBurst(living.getLocation().add(0.0D, 1.0D, 0.0D), 1.1D, 30);
                hits++;
            }
        }
        state.stacks = 0;
        spawnRedBurst(center, AREA_SKILL_RADIUS, 50);
        showState(player, state, hits == 0 ? "模仿爆发未命中" : "模仿爆发命中 " + hits + " 个目标");
    }

    private LivingEntity nearestTarget(Player player, double range) {
        return player.getWorld().getNearbyEntities(player.getLocation(), range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != player && !(entity instanceof ArmorStand))
                .filter(Entity::isValid)
                .filter(entity -> !entity.isDead())
                .min(Comparator
                        .comparing((LivingEntity entity) -> entity instanceof Player ? 0 : 1)
                        .thenComparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private void chanceAddStack(Player player, MimicState state) {
        if (ThreadLocalRandom.current().nextDouble() < 0.20D && state.stacks < MAX_STACKS) {
            state.stacks++;
            showState(player, state, "模仿层数 +1");
        }
    }

    private void applyAttackModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (instance == null) {
            return;
        }
        instance.removeModifier(attackDamageKey);
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        MimicState state = states.get(player.getUniqueId());
        if (!isWeapon(weapon) || state == null || state.phase == MimicPhase.TWO || !isFullSet(player)) {
            return;
        }
        double amount = state.phase == MimicPhase.THREE ? PHASE_THREE_DAMAGE : PHASE_ONE_DAMAGE;
        instance.addModifier(new AttributeModifier(
                attackDamageKey,
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND));
    }

    private void removeAttackModifier(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (instance != null) {
            instance.removeModifier(attackDamageKey);
        }
    }

    private boolean isFullSet(Player player) {
        EgoEquipped equipped = plugin.ego().resolve(player);
        return equipped.active() && SET_ID.equals(equipped.setId()) && equipped.pieces() >= 4;
    }

    private MimicState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), ignored -> new MimicState());
    }

    private void showState(Player player, MimicState state, String note) {
        String phase = switch (state.phase) {
            case ONE -> "<yellow>拟态 I";
            case TWO -> "<red>拟态 II";
            case THREE -> "<dark_red>拟态 III";
        };
        Messages.actionBar(player, phase + " <gray>| <gold>模仿 " + state.stacks + "/" + MAX_STACKS
                + (note == null || note.isBlank() ? "" : " <dark_gray>· " + note));
    }

    private void spawnHitParticles(LivingEntity target) {
        target.getWorld().spawnParticle(
                Particle.CRIT,
                target.getLocation().add(0.0D, 1.0D, 0.0D),
                10,
                0.35D,
                0.5D,
                0.35D,
                0.02D);
    }

    private void spawnRedBurst(Location center, double radius, int count) {
        center.getWorld().spawnParticle(
                Particle.DUST,
                center,
                count,
                radius,
                radius,
                radius,
                0.02D,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.0F));
    }

    private boolean withSuppressedAbility(java.util.function.Supplier<Boolean> action) {
        int previous = abilityDepth.get();
        abilityDepth.set(previous + 1);
        try {
            return Boolean.TRUE.equals(action.get());
        } finally {
            abilityDepth.set(previous);
        }
    }

    private enum MimicPhase {
        ONE,
        TWO,
        THREE
    }

    private static final class MimicState {
        private MimicPhase phase = MimicPhase.ONE;
        private long phaseUntil;
        private int stacks;
    }
}
