// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一套 E.G.O. 的定义。
 *
 * @param resistance   统一抗性 x，作用于所有类型的传入伤害
 * @param developable  是否可通过开发台研发
 * @param hidden       是否从公开研发列表隐藏（仍可通过管理命令发放）
 * @param opOnly       防具是否只允许 OP 穿戴
 * @param developCosts 护甲/武器各自的基准研发费用
 * @param bonus        按防具件数 y 生效的套装能力
 * @param blueprintId  解锁研发能力的图纸 id；不可研发时为 null
 * @param acquisitionWhitelist 允许获取该套装的玩家名 / UUID；为空表示不限制
 */
public record EgoSetDefinition(
        String id,
        String displayName,
        double resistance,
        boolean developable,
        boolean hidden,
        boolean opOnly,
        Map<EgoDevelopCategory, Integer> developCosts,
        EgoSetBonus bonus,
        String blueprintId,
        Set<String> acquisitionWhitelist) {

    public EgoSetDefinition {
        developCosts = developCosts == null || developCosts.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(developCosts));
        bonus = bonus == null ? EgoSetBonus.none() : bonus;
        id = id.toLowerCase(Locale.ROOT);
        acquisitionWhitelist = normalizeWhitelist(acquisitionWhitelist);
    }

    public int developCost(EgoDevelopCategory category) {
        return developCosts.getOrDefault(category, -1);
    }

    public boolean canDevelop(EgoDevelopCategory category) {
        return developable && developCost(category) >= 0;
    }

    public boolean acquisitionRestricted() {
        return !acquisitionWhitelist.isEmpty();
    }

    public boolean canBeAcquiredBy(String playerName, UUID playerId) {
        if (acquisitionWhitelist.isEmpty()) {
            return true;
        }
        if (playerName != null && acquisitionWhitelist.contains(playerName.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return playerId != null && acquisitionWhitelist.contains(playerId.toString().toLowerCase(Locale.ROOT));
    }

    private static Set<String> normalizeWhitelist(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : source) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
