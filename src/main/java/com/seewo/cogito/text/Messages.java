package com.seewo.cogito.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/** 统一的文本出口：所有提示都走 MiniMessage，支持渐变、颜色、占位符。 */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component PREFIX = MINI.deserialize("<gradient:#22d3a8:#3b82f6>[脑啡肽]</gradient> ");

    private Messages() {
    }

    public static Component of(String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    /** 带统一前缀的提示。 */
    public static void send(CommandSender to, String miniMessage) {
        to.sendMessage(PREFIX.append(MINI.deserialize(miniMessage)));
    }

    /** 不带前缀的原文输出。 */
    public static void raw(CommandSender to, String miniMessage) {
        to.sendMessage(MINI.deserialize(miniMessage));
    }
}
