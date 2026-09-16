// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Locale;
import org.bukkit.inventory.EquipmentSlot;

/** E.G.O. 物品部位。只有四个防具部位会计入抗性件数。 */
public enum EgoPiece {
    HELMET(true),
    CHESTPLATE(true),
    LEGGINGS(true),
    BOOTS(true),
    WEAPON(false),
    ACCESSORY(false);

    private final boolean armor;

    EgoPiece(boolean armor) {
        this.armor = armor;
    }

    public boolean armor() {
        return armor;
    }

    public static EgoPiece parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (EgoPiece piece : values()) {
            if (piece.name().equals(normalized)) {
                return piece;
            }
        }
        return null;
    }

    public static EgoPiece forSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> HELMET;
            case CHEST -> CHESTPLATE;
            case LEGS -> LEGGINGS;
            case FEET -> BOOTS;
            default -> null;
        };
    }
}
