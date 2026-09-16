// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.data;

import java.util.Optional;
import java.util.UUID;

/** 玩家数据存储后端。目前只有 SQLite 实现，留接口是为了以后换 MySQL 不用动上层。 */
public interface PlayerDataStore {

    /** 建表等初始化工作。 */
    void init() throws Exception;

    Optional<PlayerProfile> load(UUID uuid);

    /** 按玩家名查（用于离线玩家，名字不区分大小写）。 */
    Optional<PlayerProfile> findByName(String name);

    void save(PlayerProfile profile);

    /** 删除某个玩家的全部数据。 */
    void delete(UUID uuid);

    int count();

    void close();
}
