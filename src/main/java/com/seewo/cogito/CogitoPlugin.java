// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito;

import com.seewo.cogito.command.CogitoCommand;
import com.seewo.cogito.command.EnkephalinCommand;
import com.seewo.cogito.command.ExchangeCommand;
import com.seewo.cogito.data.PlayerDataService;
import com.seewo.cogito.develop.DevelopTableManager;
import com.seewo.cogito.develop.EgoDevelopmentService;
import com.seewo.cogito.economy.VaultHook;
import com.seewo.cogito.ego.EgoDamageService;
import com.seewo.cogito.ego.EgoRegistry;
import com.seewo.cogito.ego.EgoSetBonusService;
import com.seewo.cogito.gui.EnkephalinMenu;
import com.seewo.cogito.gui.MenuListener;
import com.seewo.cogito.item.EnkephalinItem;
import com.seewo.cogito.item.ItemRegistry;
import com.seewo.cogito.listener.EgoCombatListener;
import com.seewo.cogito.listener.EgoDamageListener;
import com.seewo.cogito.listener.EgoEnchantListener;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.listener.DevelopListener;
import com.seewo.cogito.listener.ItemBehaviourListener;
import com.seewo.cogito.listener.PlayerDataListener;
import com.seewo.cogito.listener.PlayerJoinListener;
import com.seewo.cogito.listener.ServerOwnerProtectionListener;
import com.seewo.cogito.text.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cogito —— 脑叶服务器核心玩法插件。
 *
 * <p>当前版本做「物品与经济」这一层：物品注册表（items.yml）、脑啡肽、Vault 经济兑换、
 * 箱子 GUI、管理命令。异想体镇压与 E.G.O 开发会在后续版本接上来（见设计稿）。
 */
public final class CogitoPlugin extends JavaPlugin {

    private static CogitoPlugin instance;

    private EnkephalinItem enkephalinItem;
    private ItemRegistry itemRegistry;
    private EgoRegistry egoRegistry;
    private EgoDamageService egoDamageService;
    private EgoSetBonusService egoSetBonusService;
    private EgoDevelopmentService egoDevelopmentService;
    private DevelopTableManager developTableManager;
    private PlayerDataService dataService;
    private VaultHook vaultHook;

    public static CogitoPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.itemRegistry = new ItemRegistry(this);
        this.itemRegistry.load();
        this.egoRegistry = new EgoRegistry(this);
        this.egoRegistry.load();
        this.egoDamageService = new EgoDamageService(this);
        this.egoSetBonusService = new EgoSetBonusService(this);
        this.egoDevelopmentService = new EgoDevelopmentService(this);
        this.developTableManager = new DevelopTableManager(this);
        this.enkephalinItem = new EnkephalinItem(this);

        this.dataService = new PlayerDataService(this);
        if (!dataService.init()) {
            getLogger().severe("玩家数据层初始化失败，插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.vaultHook = new VaultHook(this);
        boolean vaultReady = vaultHook.setup();

        registerCommand("enkephalin", new EnkephalinCommand(this));
        registerCommand("exchange", new ExchangeCommand(this));
        registerCommand("cogito", new CogitoCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemBehaviourListener(this), this);
        getServer().getPluginManager().registerEvents(new EgoDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new EgoCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerOwnerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EgoEnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
        getServer().getPluginManager().registerEvents(new DevelopListener(this), this);
        registerDevelopTableRecipe();
        this.egoSetBonusService.start();

        getLogger().info("已启用：脑啡肽物品 = " + enkephalinItem.material()
                + "（标签 " + enkephalinItem.tagKey() + "）");
        if (vaultReady) {
            getLogger().info("Vault 经济已连接：" + vaultHook.economy().getName());
        } else {
            getLogger().warning("未找到 Vault 经济（需要 Vault + 一个经济插件），/exchange 暂时不可用");
        }
    }

    @Override
    public void onDisable() {
        if (egoSetBonusService != null) {
            egoSetBonusService.stop();
        }
        if (dataService != null) {
            dataService.shutdown();
        }
        instance = null;
        getLogger().info("已禁用");
    }

    /** 重载 config.yml 与 items.yml（/enkephalin reload、/cogito reload）。 */
    public void reloadPluginConfig() {
        reloadConfig();
        this.itemRegistry.load();
        this.egoRegistry.load();
        this.enkephalinItem = new EnkephalinItem(this);
        this.vaultHook.setup();
        if (egoSetBonusService != null) {
            Bukkit.getOnlinePlayers().forEach(egoSetBonusService::reconcile);
        }
        registerDevelopTableRecipe();
    }

    /**
     * 打开「脑啡肽箱子」箱子界面（/cogito gui）。
     *
     * <p>每次打开都新建一个菜单实例——菜单的 InventoryHolder 就是那个实例本身。
     */
    public void openBox(Player player) {
        if (!getConfig().getBoolean("gui.enabled", true)) {
            Messages.send(player, "<red>箱子界面已被配置关闭（gui.enabled: false）");
            return;
        }
        new EnkephalinMenu(this, player).open(player);
    }

    private void registerCommand(String name, TabExecutor executor) {
        // 命令注册见 plugin.yml
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

    public ItemRegistry items() {
        return itemRegistry;
    }

    public PlayerDataService data() {
        return dataService;
    }

    public EgoRegistry ego() {
        return egoRegistry;
    }

    public EgoDamageService egoDamage() {
        return egoDamageService;
    }

    public EgoSetBonusService egoBonuses() {
        return egoSetBonusService;
    }

    public EgoDevelopmentService egoDevelopment() {
        return egoDevelopmentService;
    }

    public DevelopTableManager developTables() {
        return developTableManager;
    }

    /**
     * 注册开发台的合成表：四角下界合金块、四边脑啡肽模块、中心工作台。
     *
     * <p>重载时会先移除旧配方再重新加，避免重复注册。
     */
    private void registerDevelopTableRecipe() {
        NamespacedKey key = new NamespacedKey(this, "develop_table");
        Bukkit.removeRecipe(key);
        if (!getConfig().getBoolean("develop.table-recipe.enabled", true)) {
            return;
        }
        CustomItem table = itemRegistry.find(DevelopListener.TABLE_ID);
        CustomItem peModule = itemRegistry.find("pe-module");
        if (table == null || peModule == null) {
            getLogger().warning("开发台配方未注册：缺少 develop-table 或 pe-module 的物品定义");
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, table.create(1));
        recipe.shape("ABA", "BCB", "ABA");
        recipe.setIngredient('A', new RecipeChoice.MaterialChoice(Material.NETHERITE_BLOCK));
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(peModule.create(1)));
        recipe.setIngredient('C', new RecipeChoice.MaterialChoice(Material.CRAFTING_TABLE));
        Bukkit.addRecipe(recipe);
        getLogger().info("已注册开发台合成表：四角下界合金块 / 四边脑啡肽模块 / 中心工作台");
    }

    public VaultHook vault() {
        return vaultHook;
    }
}
