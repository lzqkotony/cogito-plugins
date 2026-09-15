// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito;

import com.seewo.cogito.command.EnkephalinCommand;
import com.seewo.cogito.command.ExchangeCommand;
import com.seewo.cogito.economy.VaultHook;
import com.seewo.cogito.item.EnkephalinItem;
import com.seewo.cogito.listener.PlayerJoinListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cogito —— 脑叶服务器核心玩法插件。
 *
 * <p>当前版本只做「脑啡肽」这一层：实体物品、Vault 经济兑换、管理命令。
 * 异想体镇压与 E.G.O 开发会在后续版本接上来（见设计稿）。
 */
public final class CogitoPlugin extends JavaPlugin {

    private static CogitoPlugin instance;

    private EnkephalinItem enkephalinItem;
    private VaultHook vaultHook;

    public static CogitoPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.enkephalinItem = new EnkephalinItem(this);
        this.vaultHook = new VaultHook(this);
        boolean vaultReady = vaultHook.setup();

        registerCommand("enkephalin", new EnkephalinCommand(this));
        registerCommand("exchange", new ExchangeCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("已启用：脑啡肽物品 = " + enkephalinItem.material()
                + "（标签 " + enkephalinItem.tagKey().getKey() + "）");
        if (vaultReady) {
            getLogger().info("Vault 经济已连接：" + vaultHook.economy().getName());
        } else {
            getLogger().warning("未找到 Vault 经济（需要 Vault + 一个经济插件），/exchange 暂时不可用");
        }
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("已禁用");
    }

    /** 重载 config.yml：物品定义会跟着刷新（/enkephalin reload）。 */
    public void reloadPluginConfig() {
        reloadConfig();
        this.enkephalinItem = new EnkephalinItem(this);
        this.vaultHook.setup();
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("plugin.yml 中没有声明命令 /" + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public EnkephalinItem enkephalinItem() {
        return enkephalinItem;
    }

    public VaultHook vault() {
        return vaultHook;
    }
}
