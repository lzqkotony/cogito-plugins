// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class EgoSetBonusTest {

    private final EgoSetBonus paradise = new EgoSetBonus(
            10.0D,
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>(Map.of(1, 1, 4, 2)),
            new TreeMap<>(Map.of(2, 1, 4, 2)),
            new TreeMap<>(Map.of(2, 2, 4, 2)),
            new TreeMap<>(Map.of(3, 1)),
            new TreeMap<>(Map.of(4, 1)),
            "holy",
            10,
            1.0D,
            20.0D);

    @Test
    void appliesPierceThresholdsWithoutRegressingAtHigherPieceCounts() {
        assertEquals(1, paradise.regenerationLevel(1));
        assertEquals(1, paradise.regenerationLevel(2));
        assertEquals(2, paradise.regenerationLevel(4));
        assertEquals(0, paradise.resistanceLevel(1));
        assertEquals(1, paradise.resistanceLevel(2));
        assertEquals(2, paradise.resistanceLevel(4));
        assertEquals(0, paradise.strengthLevel(1));
        assertEquals(2, paradise.strengthLevel(2));
        assertEquals(2, paradise.strengthLevel(4));
        assertEquals(0, paradise.fireResistanceLevel(2));
        assertEquals(1, paradise.fireResistanceLevel(3));
        assertEquals(1, paradise.waterBreathingLevel(4));
    }

    @Test
    void maximumHealthScalesWithEveryArmorPiece() {
        assertEquals(10.0D, paradise.maxHealth(1), 1.0E-9D);
        assertEquals(40.0D, paradise.maxHealth(4), 1.0E-9D);
    }

    @Test
    void holyOnlyUnlocksAtFourPieces() {
        assertFalse(paradise.hasSkill("holy", 3));
        assertTrue(paradise.hasSkill("holy", 4));
    }
}
