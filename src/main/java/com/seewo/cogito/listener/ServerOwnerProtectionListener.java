// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/** 阻止其他 OP 与命令方块对穿戴「服主」防具的玩家执行控制类命令。 */
public final class ServerOwnerProtectionListener implements Listener {

    private static final Set<String> RESTRICTED = Set.of(
            "kill",
            "kick",
            "ban",
            "tempban",
            "ban-ip",
            "banip",
            "effect");

    private final CogitoPlugin plugin;

    public ServerOwnerProtectionListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (shouldCancel(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            Messages.send(event.getPlayer(), "<red>目标受到服主 E.G.O. 保护；只有控制台可以绕过");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!(event.getSender() instanceof BlockCommandSender) && !(event.getSender() instanceof Player)) {
            return;
        }
        if (shouldCancel(event.getSender(), "/" + event.getCommand())) {
            event.setCancelled(true);
            Messages.send(event.getSender(), "<red>目标受到服主 E.G.O. 保护；只有控制台可以绕过");
        }
    }

    private boolean shouldCancel(CommandSender source, String rawCommand) {
        List<String> tokens = tokenize(rawCommand);
        if (tokens.isEmpty()) {
            return false;
        }
        tokens = unwrapExecute(tokens);
        if (tokens.isEmpty()) {
            return false;
        }

        String command = stripNamespace(tokens.get(0).toLowerCase(Locale.ROOT));
        if (!RESTRICTED.contains(command)) {
            return false;
        }
        List<String> targets = targetNames(command, tokens);
        if (targets.isEmpty()) {
            return false;
        }
        for (String targetName : targets) {
            if ("@a".equalsIgnoreCase(targetName) || "@p".equalsIgnoreCase(targetName)) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (ServerOwnerGuard.isProtected(plugin, source, online)) {
                        return true;
                    }
                }
                continue;
            }
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null && ServerOwnerGuard.isProtected(plugin, source, target)) {
                return true;
            }
        }
        return false;
    }

    private List<String> targetNames(String command, List<String> tokens) {
        int targetIndex = switch (command) {
            case "effect" -> 2;
            default -> 1;
        };
        if (tokens.size() <= targetIndex) {
            return List.of();
        }
        return List.of(tokens.get(targetIndex));
    }

    private List<String> unwrapExecute(List<String> tokens) {
        if (!"execute".equals(stripNamespace(tokens.get(0).toLowerCase(Locale.ROOT)))) {
            return tokens;
        }
        for (int index = 1; index < tokens.size(); index++) {
            if ("run".equalsIgnoreCase(tokens.get(index)) && index + 1 < tokens.size()) {
                return new ArrayList<>(tokens.subList(index + 1, tokens.size()));
            }
        }
        return List.of();
    }

    private List<String> tokenize(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String normalized = raw.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return Arrays.stream(normalized.split("\\s+"))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String stripNamespace(String command) {
        int separator = command.indexOf(':');
        return separator >= 0 ? command.substring(separator + 1) : command;
    }
}
