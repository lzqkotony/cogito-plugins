// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Locale;

/** 可独立研发的 E.G.O. 分类。 */
public enum EgoDevelopCategory {
    ARMOR("护甲"),
    WEAPON("武器");

    private final String displayName;

    EgoDevelopCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean contains(EgoPiece piece) {
        if (piece == null) {
            return false;
        }
        return this == ARMOR ? piece.armor() || piece == EgoPiece.ACCESSORY : piece == EgoPiece.WEAPON;
    }

    public static EgoDevelopCategory parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (EgoDevelopCategory category : values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }
        return null;
    }
}
