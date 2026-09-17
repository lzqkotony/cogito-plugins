// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

/**
 * 一套 E.G.O. 的定义。
 *
 * @param resistance  统一抗性 x，作用于所有类型的传入伤害
 * @param developCost 研发一次消耗的脑啡肽数量
 * @param blueprintId 解锁该套研发能力的图纸物品 id（blueprint-&lt;套装id&gt;）
 */
public record EgoSetDefinition(String id, String displayName, double resistance, int developCost, String blueprintId) {
}
