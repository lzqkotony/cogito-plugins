// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EgoDevelopTierTest {

    @Test
    void usesTheFourConfiguredCostMultipliers() {
        assertEquals(300, EgoDevelopTier.NORMAL.costFor(400));
        assertEquals(400, EgoDevelopTier.ADVANCED.costFor(400));
        assertEquals(600, EgoDevelopTier.HIGH.costFor(400));
        assertEquals(800, EgoDevelopTier.FULL.costFor(400));
    }

    @Test
    void roundsFractionalCostsUp() {
        assertEquals(338, EgoDevelopTier.NORMAL.costFor(450));
        assertEquals(675, EgoDevelopTier.HIGH.costFor(450));
    }
}
