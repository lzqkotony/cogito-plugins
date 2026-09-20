// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EgoMathTest {

    @Test
    void fullSetUsesConfiguredResistance() {
        assertEquals(0.1D, EgoMath.resistanceFactor(0.1D, 4), 1.0E-9D);
    }

    @Test
    void missingPiecesInterpolateTowardNormalDamage() {
        assertEquals(0.325D, EgoMath.resistanceFactor(0.1D, 3), 1.0E-9D);
        assertEquals(0.55D, EgoMath.resistanceFactor(0.1D, 2), 1.0E-9D);
        assertEquals(0.775D, EgoMath.resistanceFactor(0.1D, 1), 1.0E-9D);
    }

    @Test
    void noArmorMeansNormalDamage() {
        assertEquals(1.0D, EgoMath.resistanceFactor(0.1D, 0), 1.0E-9D);
    }

    @Test
    void resistanceAboveOneCanRepresentWeakness() {
        assertEquals(1.5D, EgoMath.resistanceFactor(1.5D, 4), 1.0E-9D);
        assertEquals(1.25D, EgoMath.resistanceFactor(1.5D, 2), 1.0E-9D);
    }

    @Test
    void negativeResistanceProducesNegativeHealingMultiplier() {
        assertEquals(-10.0D, EgoMath.resistanceFactor(-10.0D, 4), 1.0E-9D);
        assertEquals(-4.5D, EgoMath.resistanceFactor(-10.0D, 2), 1.0E-9D);
    }

    @Test
    void nonFiniteResistanceFallsBackToNormalDamage() {
        assertEquals(1.0D, EgoMath.resistanceFactor(Double.NaN, 4), 1.0E-9D);
    }

    @Test
    void blueDamageUsesPercentageOfMaxHealth() {
        assertEquals(1.0D, EgoMath.blueDamage(20.0D, 5.0D), 1.0E-9D);
        assertEquals(15.0D, EgoMath.blueDamage(100.0D, 15.0D), 1.0E-9D);
        assertEquals(0.0D, EgoMath.blueDamage(20.0D, 0.0D), 1.0E-9D);
    }
}
