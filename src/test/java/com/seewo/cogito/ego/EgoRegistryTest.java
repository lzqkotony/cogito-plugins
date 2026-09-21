// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

class EgoRegistryTest {

    @Test
    void elytraIsOnlyAllowedAsFunctionalEquipmentInTheChestSlot() {
        assertTrue(EgoRegistry.isFunctionalElytra(
                EquipmentSlot.CHEST,
                true,
                null));
        assertFalse(EgoRegistry.isFunctionalElytra(
                EquipmentSlot.HEAD,
                true,
                null));
        assertFalse(EgoRegistry.isFunctionalElytra(
                EquipmentSlot.CHEST,
                false,
                null));
    }
}
