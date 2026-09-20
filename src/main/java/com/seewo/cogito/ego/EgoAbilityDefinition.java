// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

/** E.G.O. 武器的特殊能力参数。 */
public record EgoAbilityDefinition(
        String type,
        double minDamage,
        double maxDamage,
        int minHits,
        int maxHits,
        double specialChance,
        int specialHeavyHits,
        double specialHeavyMinDamage,
        double specialHeavyMaxDamage,
        int specialLightHits,
        double specialLightMinDamage,
        double specialLightMaxDamage,
        double range,
        int forwardBlocks,
        int sideBlocks,
        int heightBlocks,
        boolean requireFullCharge,
        boolean percentageDamage,
        String message) {

    public static EgoAbilityDefinition none() {
        return new EgoAbilityDefinition(null, 0.0D, 0.0D, 1, 1, 0.0D, 0, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D, 0, 0, 0, false, true, null);
    }

    public EgoAbilityDefinition {
        type = type == null || type.isBlank() ? null : type.toLowerCase(Locale.ROOT).replace('_', '-');
        minDamage = Math.max(0.0D, minDamage);
        maxDamage = Math.max(minDamage, maxDamage);
        minHits = Math.max(1, minHits);
        maxHits = Math.max(minHits, maxHits);
        specialChance = Math.max(0.0D, Math.min(1.0D, specialChance));
        specialHeavyHits = Math.max(0, specialHeavyHits);
        specialHeavyMinDamage = Math.max(0.0D, specialHeavyMinDamage);
        specialHeavyMaxDamage = Math.max(specialHeavyMinDamage, specialHeavyMaxDamage);
        specialLightHits = Math.max(0, specialLightHits);
        specialLightMinDamage = Math.max(0.0D, specialLightMinDamage);
        specialLightMaxDamage = Math.max(specialLightMinDamage, specialLightMaxDamage);
        range = Math.max(0.0D, range);
        forwardBlocks = Math.max(0, forwardBlocks);
        sideBlocks = Math.max(0, sideBlocks);
        heightBlocks = Math.max(0, heightBlocks);
    }

    public static EgoAbilityDefinition parse(ConfigurationSection section) {
        if (section == null) {
            return none();
        }
        String type = section.getString("type");
        if (type == null || type.isBlank()) {
            return none();
        }
        return new EgoAbilityDefinition(
                type,
                section.getDouble("damage-min", 0.0D),
                section.getDouble("damage-max", section.getDouble("damage-min", 0.0D)),
                section.getInt("hits-min", 1),
                section.getInt("hits-max", section.getInt("hits-min", 1)),
                section.getDouble("special-chance", 0.0D),
                section.getInt("special-heavy-hits", 0),
                section.getDouble("special-heavy-min", 0.0D),
                section.getDouble("special-heavy-max", 0.0D),
                section.getInt("special-light-hits", 0),
                section.getDouble("special-light-min", 0.0D),
                section.getDouble("special-light-max", 0.0D),
                section.getDouble("range", 0.0D),
                section.getInt("forward", 0),
                section.getInt("side", 0),
                section.getInt("height", 0),
                section.getBoolean("require-full-charge", false),
                section.getBoolean("percentage-damage", true),
                section.getString("message", ""));
    }

    public boolean is(String id) {
        return type != null && type.equalsIgnoreCase(id);
    }
}
