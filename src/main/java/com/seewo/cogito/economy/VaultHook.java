package com.seewo.cogito.economy;

import com.seewo.cogito.CogitoPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 经济对接。
 *
 * <p>服务器上装 Vault + 任意经济插件（EssentialsX、CMI 等）后即可使用；
 * 插件本身不存钱，钱全部由经济插件负责。
 */
public final class VaultHook {

    private final CogitoPlugin plugin;
    private Economy economy;

    public VaultHook(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    /** 查找 Vault 注册的经济服务，返回是否成功。 */
    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return false;
        }
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
        return economy != null;
    }

    /** Vault 或经济插件可能是后加载的，这里顺手重试一次。 */
    public boolean isReady() {
        if (economy == null) {
            setup();
        }
        return economy != null;
    }

    public Economy economy() {
        return economy;
    }

    public double balance(OfflinePlayer player) {
        return isReady() ? economy.getBalance(player) : 0D;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return isReady() && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return isReady() && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return isReady() ? economy.format(amount) : String.format("%.2f", amount);
    }
}
