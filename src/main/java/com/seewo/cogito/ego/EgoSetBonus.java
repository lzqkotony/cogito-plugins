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
        String skill,
        int skillCooldownSeconds) {

    public static EgoSetBonus none() {
        return new EgoSetBonus(0.0D, new TreeMap<>(), new TreeMap<>(), null, 0);
    }

    public EgoSetBonus {
        attackSpeedByPieces = orderedCopy(attackSpeedByPieces);
        attackDamageByPieces = orderedCopy(attackDamageByPieces);
        skill = skill == null || skill.isBlank() ? null : skill.toLowerCase(java.util.Locale.ROOT);
        skillCooldownSeconds = Math.max(0, skillCooldownSeconds);
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
}
