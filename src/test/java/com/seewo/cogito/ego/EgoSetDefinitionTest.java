// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EgoSetDefinitionTest {

    private static final UUID OWNER_ID = UUID.fromString("2b0642d2-17d8-3987-8d83-9a1ca9decd2f");

    @Test
    void acquisitionWhitelistAcceptsJavaAndBedrockNamesCaseInsensitively() {
        EgoSetDefinition definition = definition(Set.of("lzqkotony", "BE_lzqkotony"));

        assertTrue(definition.canBeAcquiredBy("lzqkotony", null));
        assertTrue(definition.canBeAcquiredBy("be_lzqkotony", null));
        assertFalse(definition.canBeAcquiredBy("SomeoneElse", null));
    }

    @Test
    void acquisitionWhitelistAcceptsUuidAndOpenWhitelistAllowsEveryone() {
        EgoSetDefinition restricted = definition(Set.of(OWNER_ID.toString()));
        EgoSetDefinition open = definition(Set.of());

        assertTrue(restricted.canBeAcquiredBy("renamed-player", OWNER_ID));
        assertFalse(restricted.canBeAcquiredBy("renamed-player", UUID.randomUUID()));
        assertTrue(open.canBeAcquiredBy("anyone", UUID.randomUUID()));
    }

    private EgoSetDefinition definition(Set<String> whitelist) {
        return new EgoSetDefinition(
                "server-owner",
                "服主",
                -10.0D,
                false,
                true,
                true,
                Map.of(),
                EgoSetBonus.none(),
                null,
                whitelist);
    }
}
