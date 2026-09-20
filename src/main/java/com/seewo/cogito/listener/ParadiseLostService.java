// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.DamageChannel;
import com.seewo.cogito.ego.EgoAbilityDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** 「失乐园」的武器、主动召唤与 12 位使徒。 */
public final class ParadiseLostService implements Listener {

    public static final String ABILITY_ID = "paradise-summon";

    private static final long ATTACK_INTERVAL_MILLIS = 2_000L;
    private static final long SPECIAL_ATTACK_INTERVAL_MILLIS = 20_000L;
    private static final long SUMMON_COOLDOWN_MILLIS = 120_000L;
    private static final double SUMMON_HEALTH_COST = 0.25D;
    private static final int SUMMON_COUNT = 12;
    private static final int SUMMON_INTERVAL_TICKS = 10;
    private static final int SUMMON_TOTAL_TICKS = 120;
    private static final int APOSTLE_HEALTH = 66;
    private static final double APOSTLE_DAMAGE = 6.0D;
    private static final long APOSTLE_LIFETIME_MILLIS = 26_000L;
    private static final double APOSTLE_DASH_DISTANCE = 10.0D;
    private static final long APOSTLE_DASH_COOLDOWN_MILLIS = 10_000L;
    private static final String SUMMON_LINE = "起来吧，我的仆从，欢唱着迎接我的到来。";

    private final CogitoPlugin plugin;
    private final NamespacedKey apostleKey;
    private final NamespacedKey apostleOwnerKey;
    private final Map<UUID, Long> lastAttackAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSpecialAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> summonReadyAt = new ConcurrentHashMap<>();
    private final Map<UUID, ApostleState> apostles = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> summonTasks = new ConcurrentHashMap<>();
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();
    private BukkitTask apostleTask;

    public ParadiseLostService(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.apostleKey = new NamespacedKey(plugin, "paradise_apostle");
        this.apostleOwnerKey = new NamespacedKey(plugin, "paradise_apostle_owner");
        startApostleTask();
    }

    public boolean isWeapon(CustomItem item) {
        return item != null && item.ability() != null && item.ability().is(ABILITY_ID);
    }

    /**
     * 执行一次挥击。普通攻击只命中 16 格内锁定的目标；特殊攻击冷却完成后会优先
     * 向前挥出黑色痕迹，即使没有直接锁定生物也能触发范围攻击。
     */
    public boolean tryAttack(Player player, Entity target) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (!isWeapon(weapon)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long previous = lastAttackAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < ATTACK_INTERVAL_MILLIS) {
            return false;
        }

        Long lastSpecial = lastSpecialAt.get(player.getUniqueId());
        if (lastSpecial == null) {
            lastSpecialAt.put(player.getUniqueId(), now);
        } else if (now - lastSpecial >= SPECIAL_ATTACK_INTERVAL_MILLIS) {
            lastAttackAt.put(player.getUniqueId(), now);
            lastSpecialAt.put(player.getUniqueId(), now);
            player.setCooldown(player.getInventory().getItemInMainHand(), 40);
            performSpecialAttack(player, weapon.ability());
            return true;
        }

        if (!(target instanceof LivingEntity living) || living == player || !living.isValid() || living.isDead()) {
            return false;
        }
        if (living instanceof Player victim
                && ServerOwnerGuard.isProtected(plugin, player, victim)) {
            Messages.send(player, "<red>目标受到服主 E.G.O. 保护");
            return false;
        }

        if (!strikeSingleTarget(player, living, weapon.ability())) {
            return false;
        }
        lastAttackAt.put(player.getUniqueId(), now);
        player.setCooldown(player.getInventory().getItemInMainHand(), 40);
        return true;
    }

    /** 右键主动技能：消耗 25% 最大生命，在 6 秒内召唤 12 位使徒。 */
    public boolean trySummon(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        CustomItem weapon = plugin.items().identify(player.getInventory().getItemInMainHand());
        if (!isWeapon(weapon)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long readyAt = summonReadyAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            Messages.send(player, "<white>失乐园的召唤尚需等待 <gray>" + seconds + " 秒");
            return false;
        }

        double cost = player.getMaxHealth() * SUMMON_HEALTH_COST;
        if (player.getHealth() <= cost) {
            Messages.send(player, "<red>生命值不足，失乐园无法回应召唤");
            return false;
        }

        player.setHealth(Math.max(1.0D, player.getHealth() - cost));
        summonReadyAt.put(player.getUniqueId(), now + SUMMON_COOLDOWN_MILLIS);
        player.setCooldown(player.getInventory().getItemInMainHand(), (int) (SUMMON_COOLDOWN_MILLIS / 50L));
        player.teleport(player.getLocation().clone().add(0.0D, 6.0D, 0.0D));
        player.setVelocity(new Vector());
        player.setFallDistance(0.0F);
        alertNearbyPlayers(player);
        startSummonSequence(player);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.55F, 1.55F);
        return true;
    }

    public void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        BukkitTask task = summonTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            tasks.remove(task);
        }
    }

    public void shutdown() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
        summonTasks.clear();
        if (apostleTask != null) {
            apostleTask.cancel();
            apostleTask = null;
        }
        for (ApostleState state : apostles.values()) {
            Entity entity = Bukkit.getEntity(state.entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        apostles.clear();
    }

    @EventHandler
    public void onApostleDeath(EntityDeathEvent event) {
        if (!apostles.containsKey(event.getEntity().getUniqueId())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        apostles.remove(event.getEntity().getUniqueId());
    }

    private boolean strikeSingleTarget(Player player, LivingEntity target, EgoAbilityDefinition ability) {
        double damage = random(ability.minDamage(), ability.maxDamage());
        boolean applied = plugin.egoDamage().dealDamage(player, target, damage, DamageChannel.BLUE);
        if (!applied) {
            return false;
        }
        onWeaponHit(player, target);
        spawnWhiteLances(target);
        return true;
    }

    private void performSpecialAttack(Player player, EgoAbilityDefinition ability) {
        Location origin = player.getEyeLocation().subtract(0.0D, 1.0D, 0.0D);
        Vector forward = horizontalDirection(player);
        Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX()).normalize();
        double forwardRange = Math.max(1.0D, ability.forwardBlocks());
        double halfWidth = Math.max(0.5D, ability.sideBlocks() / 2.0D);
        double halfHeight = Math.max(1.0D, ability.heightBlocks() / 2.0D);
        Location center = origin.clone().add(forward.clone().multiply(forwardRange / 2.0D));
        Set<UUID> hit = new HashSet<>();

        for (Entity entity : player.getWorld().getNearbyEntities(
                center,
                forwardRange,
                halfHeight,
                forwardRange)) {
            if (!(entity instanceof LivingEntity living)
                    || living == player
                    || living.isDead()
                    || !living.isValid()) {
                continue;
            }
            if (living instanceof Player victim
                    && ServerOwnerGuard.isProtected(plugin, player, victim)) {
                continue;
            }
            Vector relative = living.getLocation().toVector().subtract(origin.toVector());
            double forwardDistance = relative.dot(forward);
            if (forwardDistance < 0.0D || forwardDistance > forwardRange) {
                continue;
            }
            double lateral = relative.dot(right);
            if (Math.abs(lateral) > halfWidth || Math.abs(relative.getY()) > halfHeight) {
                continue;
            }
            double damage = random(50.0D, 60.0D);
            if (plugin.egoDamage().dealDamage(player, living, damage, DamageChannel.BLUE)) {
                onWeaponHit(player, living);
                spawnWhiteLances(living);
                hit.add(living.getUniqueId());
            }
        }

        spawnBlackTrail(origin, forward, forwardRange, halfHeight);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8F, 0.65F);
        if (hit.isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.45F, 0.75F);
        }
    }

    private void onWeaponHit(Player player, LivingEntity target) {
        double healed = random(2.0D, 4.0D);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healed));
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                10,
                3,
                false,
                false,
                false));
    }

    private void startSummonSequence(Player player) {
        cleanupPlayer(player);
        int[] elapsed = {0};
        int[] spawned = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                cleanupPlayer(player);
                return;
            }
            player.setFallDistance(0.0F);
            spawnWings(player);
            if (elapsed[0] % SUMMON_INTERVAL_TICKS == 0 && spawned[0] < SUMMON_COUNT) {
                spawnApostle(player, spawned[0]);
                spawned[0]++;
            }
            elapsed[0] += 2;
            if (elapsed[0] >= SUMMON_TOTAL_TICKS && spawned[0] >= SUMMON_COUNT) {
                cleanupPlayer(player);
            }
        }, 0L, 2L);
        summonTasks.put(player.getUniqueId(), task);
        tasks.add(task);
    }

    private void alertNearbyPlayers(Player summoner) {
        Component main = Messages.of("<white>" + SUMMON_LINE);
        Title title = Title.title(
                main,
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(300L),
                        Duration.ofSeconds(3L),
                        Duration.ofMillis(700L)));
        for (Player nearby : summoner.getWorld().getNearbyPlayers(summoner.getLocation(), 16.0D)) {
            if (nearby == summoner) {
                continue;
            }
            nearby.showTitle(title);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, true, true));
        }
    }

    private void spawnApostle(Player summoner, int index) {
        double angle = (Math.PI * 2.0D * index) / SUMMON_COUNT;
        double radius = index % 2 == 0 ? 2.0D : 3.1D;
        Location spawn = summoner.getLocation().clone().add(
                Math.cos(angle) * radius,
                -0.8D,
                Math.sin(angle) * radius);
        Skeleton skeleton = summoner.getWorld().spawn(spawn, Skeleton.class);
        skeleton.setShouldBurnInDay(false);
        skeleton.setRemoveWhenFarAway(false);
        skeleton.setCanPickupItems(false);
        skeleton.setCollidable(false);
        skeleton.setAware(true);
        skeleton.customName(Messages.of("<white>失乐园的使徒"));
        skeleton.setCustomNameVisible(false);
        skeleton.getPersistentDataContainer().set(apostleKey, PersistentDataType.BYTE, (byte) 1);
        skeleton.getPersistentDataContainer().set(
                apostleOwnerKey,
                PersistentDataType.STRING,
                summoner.getUniqueId().toString());

        setBaseAttribute(skeleton, Attribute.MAX_HEALTH, APOSTLE_HEALTH);
        setBaseAttribute(skeleton, Attribute.ATTACK_DAMAGE, APOSTLE_DAMAGE);
        skeleton.setHealth(APOSTLE_HEALTH);

        EntityEquipment equipment = skeleton.getEquipment();
        if (equipment != null) {
            equipment.setItem(EquipmentSlot.HAND, new ItemStack(org.bukkit.Material.IRON_SPEAR), true);
            equipment.setHelmet(new ItemStack(org.bukkit.Material.IRON_HELMET), true);
            equipment.setChestplate(new ItemStack(org.bukkit.Material.IRON_CHESTPLATE), true);
            equipment.setLeggings(new ItemStack(org.bukkit.Material.IRON_LEGGINGS), true);
            equipment.setBoots(new ItemStack(org.bukkit.Material.IRON_BOOTS), true);
            equipment.setDropChance(EquipmentSlot.HAND, 0.0F);
            equipment.setDropChance(EquipmentSlot.HEAD, 0.0F);
            equipment.setDropChance(EquipmentSlot.CHEST, 0.0F);
            equipment.setDropChance(EquipmentSlot.LEGS, 0.0F);
            equipment.setDropChance(EquipmentSlot.FEET, 0.0F);
        }

        long now = System.currentTimeMillis();
        apostles.put(skeleton.getUniqueId(), new ApostleState(
                skeleton.getUniqueId(),
                summoner.getUniqueId(),
                now,
                now,
                now,
                0L));
    }

    private void startApostleTask() {
        apostleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickApostles, 20L, 5L);
    }

    private void tickApostles() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, ApostleState>> iterator = apostles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ApostleState> entry = iterator.next();
            ApostleState state = entry.getValue();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Skeleton skeleton) || !skeleton.isValid() || skeleton.isDead()) {
                iterator.remove();
                continue;
            }
            if (state.expiresAt > 0L && now >= state.expiresAt) {
                skeleton.remove();
                iterator.remove();
                continue;
            }
            if (state.expiresAt == 0L && skeleton.isOnGround() && now - state.spawnedAt > 1500L) {
                state.expiresAt = now + APOSTLE_LIFETIME_MILLIS;
            }
            if (state.expiresAt == 0L && now - state.spawnedAt > 10_000L) {
                state.expiresAt = now + APOSTLE_LIFETIME_MILLIS;
            }

            LivingEntity target = findApostleTarget(skeleton, state);
            if (target == null) {
                skeleton.setTarget(null);
                continue;
            }
            skeleton.setTarget(target);
            double distance = skeleton.getLocation().distance(target.getLocation());
            if (distance > APOSTLE_DASH_DISTANCE && now >= state.nextDashAt) {
                dashToTarget(skeleton, target);
                state.nextDashAt = now + APOSTLE_DASH_COOLDOWN_MILLIS;
                state.nextMeleeAt = now + 1000L;
                continue;
            }
            if (distance > 2.6D) {
                skeleton.getPathfinder().moveTo(target, 1.2D);
            } else if (now >= state.nextMeleeAt) {
                target.setNoDamageTicks(0);
                target.damage(APOSTLE_DAMAGE, skeleton);
                spawnDashTrail(skeleton.getEyeLocation(), target.getLocation().add(0.0D, 1.0D, 0.0D));
                state.nextMeleeAt = now + 1000L;
            }
        }
    }

    private LivingEntity findApostleTarget(Skeleton skeleton, ApostleState state) {
        Player owner = Bukkit.getPlayer(state.ownerId);
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity nearby : skeleton.getNearbyEntities(24.0D, 16.0D, 24.0D)) {
            if (!(nearby instanceof LivingEntity living)
                    || living == skeleton
                    || living.isDead()
                    || !living.isValid()
                    || living.isInvulnerable()
                    || living instanceof ArmorStand
                    || apostles.containsKey(living.getUniqueId())) {
                continue;
            }
            if (living.getUniqueId().equals(state.ownerId)) {
                continue;
            }
            if (living instanceof Player victim && owner != null
                    && ServerOwnerGuard.isProtected(plugin, owner, victim)) {
                continue;
            }
            candidates.add(living);
        }
        return candidates.stream()
                .min(Comparator.comparingDouble(value -> value.getLocation().distanceSquared(skeleton.getLocation())))
                .orElse(null);
    }

    private void dashToTarget(Skeleton skeleton, LivingEntity target) {
        Location from = skeleton.getEyeLocation();
        Vector direction = target.getLocation().add(0.0D, 0.8D, 0.0D)
                .toVector()
                .subtract(from.toVector());
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = skeleton.getLocation().getDirection();
        } else {
            direction.normalize();
        }
        Location destination = target.getLocation().clone().subtract(direction.clone().multiply(1.35D));
        destination.setDirection(direction);
        skeleton.teleport(destination);
        spawnDashTrail(from, skeleton.getEyeLocation());
        target.setNoDamageTicks(0);
        target.damage(APOSTLE_DAMAGE, skeleton);
    }

    private void spawnWings(Player player) {
        Location base = player.getLocation().add(0.0D, 1.75D, 0.0D);
        Vector forward = horizontalDirection(player);
        Vector back = forward.clone().multiply(-1.0D);
        Vector right = new Vector(-back.getZ(), 0.0D, back.getX()).normalize();
        for (int side : new int[]{-1, 1}) {
            for (int layer = 0; layer < 2; layer++) {
                for (int step = 1; step <= 5; step++) {
                    Location point = base.clone()
                            .add(back.clone().multiply(0.15D + step * 0.28D))
                            .add(right.clone().multiply(side * (0.2D + step * 0.24D)))
                            .add(0.0D, layer * 0.36D + step * 0.08D, 0.0D);
                    player.getWorld().spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            0.0D,
                            0.0D,
                            0.0D,
                            0.0D,
                            new Particle.DustOptions(Color.WHITE, 0.85F));
                }
            }
        }
    }

    private void spawnWhiteLances(LivingEntity target) {
        Vector right = new Vector(1.0D, 0.0D, 0.0D);
        Location base = target.getLocation();
        for (int line = -1; line <= 1; line++) {
            for (int step = 0; step <= 10; step++) {
                Location point = base.clone()
                        .add(right.clone().multiply(line * 0.28D))
                        .add(0.0D, -0.35D + step * 0.20D, 0.0D);
                target.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnBlackTrail(Location origin, Vector forward, double range, double halfHeight) {
        for (int step = 0; step <= 40; step++) {
            double progress = step / 40.0D;
            Location point = origin.clone().add(forward.clone().multiply(range * progress));
            double ripple = Math.sin(progress * Math.PI) * halfHeight;
            point.add(0.0D, ripple - halfHeight * 0.35D, 0.0D);
            point.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    2,
                    0.08D,
                    0.10D,
                    0.08D,
                    0.0D,
                    new Particle.DustOptions(Color.BLACK, 1.45F));
            point.getWorld().spawnParticle(Particle.SQUID_INK, point, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    private void spawnDashTrail(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        for (int step = 0; step <= 10; step++) {
            Location point = from.clone().add(delta.clone().multiply(step / 10.0D));
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private Vector horizontalDirection(Player player) {
        Vector direction = player.getEyeLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = player.getLocation().getDirection().setY(0.0D);
        }
        if (direction.lengthSquared() < 1.0E-6D) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private void setBaseAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private double random(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static final class ApostleState {
        private final UUID entityId;
        private final UUID ownerId;
        private final long spawnedAt;
        private long nextDashAt;
        private long nextMeleeAt;
        private long expiresAt;

        private ApostleState(
                UUID entityId,
                UUID ownerId,
                long spawnedAt,
                long nextDashAt,
                long nextMeleeAt,
                long expiresAt) {
            this.entityId = entityId;
            this.ownerId = ownerId;
            this.spawnedAt = spawnedAt;
            this.nextDashAt = nextDashAt;
            this.nextMeleeAt = nextMeleeAt;
            this.expiresAt = expiresAt;
        }
    }
}
