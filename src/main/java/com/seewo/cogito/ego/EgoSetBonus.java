// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** 按防具件数 y 生效的套装属性与技能。 */
public record EgoSetBonus(
        double maxHealthPerPiece,
        NavigableMap<Integer, Double> attackSpeedByPieces,
        NavigableMap<Integer, Double> attackDamageByPieces,
        NavigableMap<Integer, Integer> regenerationLevelByPieces,
        NavigableMap<Integer, Integer> resistanceLevelByPieces,
        NavigableMap<Integer, Integer> strengthLevelByPieces,
        NavigableMap<Integer, Integer> fireResistanceLevelByPieces,
        NavigableMap<Integer, Integer> waterBreathingLevelByPieces,
        String skill,
        int skillCooldownSeconds,
        double skillChargePerActivation,
        double skillMaxCharge,
        double damageImmunityThreshold) {

    public static EgoSetBonus none() {
        return new EgoSetBonus(
                0.0D,
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                null,
                0,
                0.0D,
                0.0D,
                0.0D);
    }

    public EgoSetBonus {
        attackSpeedByPieces = orderedCopy(attackSpeedByPieces);
        attackDamageByPieces = orderedCopy(attackDamageByPieces);
        regenerationLevelByPieces = orderedLevelCopy(regenerationLevelByPieces);
        resistanceLevelByPieces = orderedLevelCopy(resistanceLevelByPieces);
        strengthLevelByPieces = orderedLevelCopy(strengthLevelByPieces);
        fireResistanceLevelByPieces = orderedLevelCopy(fireResistanceLevelByPieces);
        waterBreathingLevelByPieces = orderedLevelCopy(waterBreathingLevelByPieces);
        skill = skill == null || skill.isBlank() ? null : skill.toLowerCase(java.util.Locale.ROOT);
        skillCooldownSeconds = Math.max(0, skillCooldownSeconds);
        skillChargePerActivation = Math.max(0.0D, skillChargePerActivation);
        skillMaxCharge = Math.max(0.0D, skillMaxCharge);
        damageImmunityThreshold = Math.max(0.0D, damageImmunityThreshold);
    }

    public double maxHealth(int pieces) {
        return Math.max(0, pieces) * maxHealthPerPiece;
    }

    public double attackSpeed(int pieces) {
        return sumAtMost(attackSpeedByPieces, pieces);
    }

    public double attackDamage(int pieces) {
        return sumAtMost(attackDamageByPieces, pieces);
    }

    public int regenerationLevel(int pieces) {
        return levelAtMost(regenerationLevelByPieces, pieces);
    }

    public int resistanceLevel(int pieces) {
        return levelAtMost(resistanceLevelByPieces, pieces);
    }

    public int strengthLevel(int pieces) {
        return levelAtMost(strengthLevelByPieces, pieces);
    }

    public int fireResistanceLevel(int pieces) {
        return levelAtMost(fireResistanceLevelByPieces, pieces);
    }

    public int waterBreathingLevel(int pieces) {
        return levelAtMost(waterBreathingLevelByPieces, pieces);
    }

    public boolean blocksDamage(double damage, int pieces) {
        return pieces >= 4 && damageImmunityThreshold > 0.0D && damage < damageImmunityThreshold;
    }

    public boolean hasSkill(String id, int pieces) {
        return pieces >= 4 && skill != null && skill.equalsIgnoreCase(id);
    }

    private static double sumAtMost(NavigableMap<Integer, Double> values, int pieces) {
        double total = 0.0D;
        for (Map.Entry<Integer, Double> entry : values.entrySet()) {
            if (entry.getKey() > pieces) {
                break;
            }
            total += entry.getValue();
        }
        return total;
    }

    private static int levelAtMost(NavigableMap<Integer, Integer> values, int pieces) {
        int level = 0;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            if (entry.getKey() > pieces) {
                break;
            }
            level = Math.max(level, entry.getValue());
        }
        return level;
    }

    private static NavigableMap<Integer, Double> orderedCopy(Map<Integer, Double> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        TreeMap<Integer, Double> copy = new TreeMap<>();
        source.forEach((pieces, value) -> {
            if (pieces != null && pieces > 0 && value != null && Double.isFinite(value)) {
                copy.put(pieces, value);
            }
        });
        return Collections.unmodifiableNavigableMap(copy);
    }

    private static NavigableMap<Integer, Integer> orderedLevelCopy(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        TreeMap<Integer, Integer> copy = new TreeMap<>();
        source.forEach((pieces, value) -> {
            if (pieces != null && pieces > 0 && value != null && value > 0) {
                copy.put(pieces, value);
            }
        });
        return Collections.unmodifiableNavigableMap(copy);
    }
}
