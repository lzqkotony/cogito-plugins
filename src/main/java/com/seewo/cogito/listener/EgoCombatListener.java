// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.DamageChannel;
import com.seewo.cogito.ego.EgoAbilityDefinition;
import com.seewo.cogito.ego.EgoEquipped;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** E.G.O. 武器技能与「正义裁决者」整套被动；0.5.2 起蓝伤按最大生命值百分比结算。 */
public final class EgoCombatListener implements Listener {

    private static final String JUSTICE_AOE = "justice-aoe";
    private static final String JUSTICE_STRIKE = "justice-strike";
    private static final String JUSTICE_SOUL = "justice-soul";
    private static final String SERVER_OWNER = "server-owner";
    private static final String JUDGEMENT = "judgement";
    private static final double JUDGEMENT_DAMAGE = 15.0D;

    private final CogitoPlugin plugin;
    private final Map<UUID, Long> judgementReadyAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastJusticeSwingAt = new ConcurrentHashMap<>();
    private final ThreadLocal<Integer> abilityDepth = ThreadLocal.withInitial(() -> 0);

    public EgoCombatListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (abilityDepth.get() == 0 && event.getDamager() instanceof Player attacker) {
            CustomItem weapon = plugin.items().identify(attacker.getInventory().getItemInMainHand());
            if (weapon != null && isJusticeAbility(weapon.ability())) {
                event.setCancelled(true);
                tryJusticeSwing(attacker, event.getEntity());
                return;
            }
            if (weapon != null && weapon.ability().is(SERVER_OWNER)) {
                event.setCancelled(true);
                withSuppressedAbilities(() -> handleServerOwnerAttack(attacker, event.getEntity()));
                return;
            }
        }

        if (event.getEntity() instanceof Player defender
                && event.getDamager() instanceof LivingEntity attacker) {
            triggerJudgement(defender, attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (weapon == null || !weapon.ability().is(SERVER_OWNER)) {
            return;
        }

        Entity target = player.getTargetEntity((int) Math.ceil(Math.max(1.0D, weapon.ability().range())), false);
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (target instanceof Player victim) {
                if (ServerOwnerGuard.isProtected(plugin, player, victim)) {
                    Messages.send(player, "<red>目标受到服主 E.G.O. 保护");
                    return;
                }
                String message = weapon.ability().message();
                victim.kick(Messages.of(message == null || message.isBlank()
                        ? "<red>服主之意不可违逆"
                        : message), PlayerKickEvent.Cause.PLUGIN);
            }
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (target instanceof LivingEntity living) {
                killWithOwnerWeapon(player, living);
            }
        }
    }

    /**
     * 挥动手臂时触发正义裁决者。这个事件不要求光标命中实体，能覆盖空中挥剑和命中实体两种情况。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (weapon == null) {
            return;
        }
        if (isJusticeAbility(weapon.ability())) {
            Entity target = player.getTargetEntity(
                    (int) Math.ceil(Math.max(1.0D, weapon.ability().range())), false);
            tryJusticeSwing(player, target);
            return;
        }
        if (weapon.ability().is(SERVER_OWNER)) {
            Entity target = player.getTargetEntity(
                    (int) Math.ceil(Math.max(1.0D, weapon.ability().range())), false);
            if (target instanceof LivingEntity living) {
                killWithOwnerWeapon(player, living);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (weapon == null || !weapon.ability().is(SERVER_OWNER)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRightClicked() instanceof Player victim) {
            if (ServerOwnerGuard.isProtected(plugin, player, victim)) {
                Messages.send(player, "<red>目标受到服主 E.G.O. 保护");
                return;
            }
            String message = weapon.ability().message();
            victim.kick(Messages.of(message == null || message.isBlank()
                    ? "<red>服主之意不可违逆"
                    : message), PlayerKickEvent.Cause.PLUGIN);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        judgementReadyAt.remove(event.getPlayer().getUniqueId());
        lastJusticeSwingAt.remove(event.getPlayer().getUniqueId());
    }

    private boolean tryJusticeSwing(Player attacker, Entity primaryTarget) {
        CustomItem weapon = plugin.items().identify(attacker.getInventory().getItemInMainHand());
        if (weapon == null || !isJusticeAbility(weapon.ability())) {
            return false;
        }
        EgoAbilityDefinition ability = weapon.ability();
        if (ability.requireFullCharge() && attacker.getAttackCooldown() < 0.90F) {
            return false;
        }
        ItemStack held = attacker.getInventory().getItemInMainHand();
        if (ability.is(JUSTICE_SOUL)) {
            if (attacker.hasCooldown(held)) {
                return false;
            }
        }
        long now = System.currentTimeMillis();
        double attackSpeed = weapon.attributes().getOrDefault(org.bukkit.attribute.Attribute.ATTACK_SPEED, 4.0D);
        long interval = ability.is(JUSTICE_SOUL)
                ? 40L * 50L
                : Math.max(50L, (long) Math.ceil(1000.0D / Math.max(0.1D, attackSpeed)));
        long previous = lastJusticeSwingAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (now - previous < interval) {
            return false;
        }
        lastJusticeSwingAt.put(attacker.getUniqueId(), now);
        withSuppressedAbilities(() -> handleJusticeAttack(attacker, primaryTarget));
        if (ability.is(JUSTICE_SOUL)) {
            attacker.setCooldown(held, 40);
        } else {
            attacker.resetCooldown();
        }
        return true;
    }

    private void handleJusticeAttack(Player attacker, Entity primaryTarget) {
        EgoAbilityDefinition ability = plugin.items()
                .identify(attacker.getInventory().getItemInMainHand()).ability();
        if (ability.is(JUSTICE_SOUL)) {
            LivingEntity target = primaryTarget instanceof LivingEntity living && living != attacker
                    ? living
                    : targetInFront(attacker, ability.range());
            if (target != null) {
                dealJusticeSoulAttack(attacker, target, ability);
            }
            return;
        }

        Location eye = attacker.getEyeLocation();
        Vector forward = eye.getDirection().setY(0.0D);
        if (forward.lengthSquared() < 1.0E-6D) {
            forward = attacker.getLocation().getDirection().setY(0.0D);
        }
        if (forward.lengthSquared() < 1.0E-6D) {
            forward = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            forward.normalize();
        }

        double forwardRange = Math.max(1.0D, ability.forwardBlocks());
        double sideRange = Math.max(0.0D, ability.sideBlocks());
        double verticalRange = Math.max(1.0D, ability.heightBlocks());
        Location origin = attacker.getLocation();
        Collection<Entity> nearby = attacker.getWorld().getNearbyEntities(
                origin.clone().add(forward.clone().multiply(forwardRange / 2.0D)),
                forwardRange,
                verticalRange,
                forwardRange);

        if (ability.is(JUSTICE_STRIKE)) {
            if (primaryTarget instanceof LivingEntity living && living != attacker) {
                dealJusticeMultiHit(attacker, living, ability);
                spawnBlueTrail(living.getLocation().add(0.0D, 1.0D, 0.0D), ability);
            } else {
                Entity target = attacker.getTargetEntity((int) Math.ceil(Math.max(1.0D, ability.range())), false);
                if (target instanceof LivingEntity living && living != attacker) {
                    dealJusticeMultiHit(attacker, living, ability);
                    spawnBlueTrail(living.getLocation().add(0.0D, 1.0D, 0.0D), ability);
                }
            }
            return;
        }

        java.util.HashSet<UUID> hit = new java.util.HashSet<>();
        if (primaryTarget instanceof LivingEntity living && living != attacker) {
            dealJusticeMultiHit(attacker, living, ability);
            hit.add(living.getUniqueId());
        }
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living) || living == attacker || hit.contains(living.getUniqueId())) {
                continue;
            }
            Vector relative = living.getLocation().toVector().subtract(origin.toVector());
            double forwardDistance = relative.dot(forward);
            if (forwardDistance < 0.0D || forwardDistance > forwardRange) {
                continue;
            }
            Vector lateral = relative.clone().subtract(forward.clone().multiply(forwardDistance));
            if (Math.abs(lateral.getX()) > sideRange || Math.abs(lateral.getZ()) > sideRange) {
                continue;
            }
            if (Math.abs(relative.getY()) > verticalRange) {
                continue;
            }
            dealJusticeMultiHit(attacker, living, ability);
            hit.add(living.getUniqueId());
        }

        spawnBlueTrail(origin.clone().add(forward.clone().multiply(2.0D)), ability);
    }

    private LivingEntity targetInFront(Player attacker, double range) {
        Entity target = attacker.getTargetEntity((int) Math.ceil(Math.max(1.0D, range)), false);
        return target instanceof LivingEntity living && living != attacker ? living : null;
    }

    private void dealJusticeSoulAttack(Player attacker, LivingEntity target, EgoAbilityDefinition ability) {
        boolean special = ability.specialChance() > 0.0D
                && ThreadLocalRandom.current().nextDouble() < ability.specialChance();
        if (special) {
            for (int i = 0; i < ability.specialHeavyHits(); i++) {
                applyFlatHit(attacker, target, ability.specialHeavyMinDamage(), ability.specialHeavyMaxDamage());
            }
            for (int i = 0; i < ability.specialLightHits(); i++) {
                applyFlatHit(attacker, target, ability.specialLightMinDamage(), ability.specialLightMaxDamage());
            }
        } else {
            for (int i = 0; i < ability.minHits(); i++) {
                applyFlatHit(attacker, target, ability.minDamage(), ability.maxDamage());
            }
        }
        spawnBlueTrail(target.getLocation().add(0.0D, 1.0D, 0.0D), ability);
    }

    private void applyFlatHit(Player attacker, LivingEntity target, double min, double max) {
        if (!target.isValid() || target.isDead()) {
            return;
        }
        double damage = max <= min ? min : ThreadLocalRandom.current().nextDouble(min, max);
        target.setNoDamageTicks(0);
        plugin.egoDamage().dealFlatDamage(attacker, target, damage, DamageChannel.BLUE);
    }

    private boolean isJusticeAbility(EgoAbilityDefinition ability) {
        return ability != null && (ability.is(JUSTICE_AOE)
                || ability.is(JUSTICE_STRIKE)
                || ability.is(JUSTICE_SOUL));
    }

    private void dealJusticeMultiHit(Player attacker, LivingEntity target, EgoAbilityDefinition ability) {
        int minHits = ability.minHits();
        int maxHits = ability.maxHits();
        int hits = maxHits <= minHits
                ? minHits
                : ThreadLocalRandom.current().nextInt(minHits, maxHits + 1);
        for (int i = 0; i < hits && target.isValid() && !target.isDead(); i++) {
            double min = ability.minDamage();
            double max = ability.maxDamage();
            double damage = max <= min ? min : ThreadLocalRandom.current().nextDouble(min, max);
            target.setNoDamageTicks(0);
            boolean applied = ability.percentageDamage()
                    ? plugin.egoDamage().dealDamage(attacker, target, damage, DamageChannel.BLUE)
                    : plugin.egoDamage().dealFlatDamage(attacker, target, damage, DamageChannel.BLUE);
            if (!applied) {
                break;
            }
        }
    }

    private void spawnBlueTrail(Location center, EgoAbilityDefinition ability) {
        int count = Math.max(8, Math.min(10, ability.forwardBlocks() + ability.sideBlocks()));
        center.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                center,
                count,
                Math.max(0.5D, ability.sideBlocks()),
                Math.max(0.5D, ability.heightBlocks() / 2.0D),
                Math.max(0.5D, ability.forwardBlocks() / 2.0D),
                0.01D);
    }

    private void triggerJudgement(Player defender, LivingEntity attacker) {
        if (defender == attacker || !defender.isOnline() || defender.isDead()) {
            return;
        }
        EgoEquipped equipped = plugin.ego().resolve(defender);
        EgoSetDefinition set = equipped.active() ? plugin.ego().set(equipped.setId()) : null;
        if (set == null || !set.bonus().hasSkill(JUDGEMENT, equipped.pieces())) {
            return;
        }
        int cooldown = Math.max(0, set.bonus().skillCooldownSeconds());
        long now = System.currentTimeMillis();
        long readyAt = judgementReadyAt.getOrDefault(defender.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        judgementReadyAt.put(defender.getUniqueId(), now + cooldown * 1000L);
        boolean[] judgementHit = {false};
        withSuppressedAbilities(() -> judgementHit[0] =
                plugin.egoDamage().dealSkillDamage(defender, attacker, JUDGEMENT_DAMAGE, DamageChannel.BLUE));
        if (judgementHit[0]) {
            defender.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    defender.getLocation().add(0.0D, 1.0D, 0.0D),
                    18,
                    0.45D,
                    0.65D,
                    0.45D,
                    0.02D);
            Messages.send(defender, "<aqua>审判已发动：对攻击者造成 <white>"
                    + JUDGEMENT_DAMAGE + "</white> 点蓝伤");
        }
    }

    private void handleServerOwnerAttack(Player attacker, Entity target) {
        if (target instanceof LivingEntity living) {
            killWithOwnerWeapon(attacker, living);
        }
    }

    private void killWithOwnerWeapon(Player attacker, LivingEntity target) {
        if (target instanceof Player victim && ServerOwnerGuard.isProtected(plugin, attacker, victim)) {
            Messages.send(attacker, "<red>目标受到服主 E.G.O. 保护");
            return;
        }
        target.setHealth(0.0D);
    }

    private void withSuppressedAbilities(Runnable action) {
        int previous = abilityDepth.get();
        abilityDepth.set(previous + 1);
        try {
            action.run();
        } finally {
            abilityDepth.set(previous);
        }
    }
}
