// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

/** 一套 E.G.O. 防具的定义。x 统一作用于所有类型的传入伤害。 */
public record EgoSetDefinition(String id, String displayName, double resistance) {
}
