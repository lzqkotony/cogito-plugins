// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
        String blueprintId) {

    public EgoSetDefinition {
        developCosts = developCosts == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(developCosts));
        bonus = bonus == null ? EgoSetBonus.none() : bonus;
        id = id.toLowerCase(java.util.Locale.ROOT);
    }

    public int developCost(EgoDevelopCategory category) {
        return developCosts.getOrDefault(category, -1);
    }

    public boolean canDevelop(EgoDevelopCategory category) {
        return developable && developCost(category) >= 0;
    }
}
