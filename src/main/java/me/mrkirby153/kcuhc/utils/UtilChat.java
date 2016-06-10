package me.mrkirby153.kcuhc.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class UtilChat {

    public static BaseComponent generateError(String message) {
        TextComponent err = new TextComponent("Error> ");
        err.setColor(ChatColor.BLUE);
        TextComponent component = new TextComponent(message);
        component.setColor(ChatColor.GRAY);
        err.addExtra(component);
        return err;
    }

    public static String generateLegacyError(String message) {
        return generateError(message).toLegacyText();
    }

    /**
     * Generates formatted chat
     *
     * @param message The message to format
     * @param color   The color of the message
     * @param flags   A decimal number determining the flags to set. <br>Binary flags:<br> 1000 (8) = Bold
     *                <br> 0100 (4) = Italic<br> 0010 (2) = Strike<br> 0001 (1) = Obfuscated
     * @return
     */
    public static BaseComponent generateFormattedChat(String message, ChatColor color, int flags) {
        TextComponent tc = new TextComponent(message);
        tc.setColor(color);
        if (flags > 15 || flags < 0)
            throw new IllegalArgumentException("Flags must be between 0 and 15! (" + flags + ")");
        if ((flags & 8) == 8)
            tc.setBold(true);
        if ((flags & 4) == 4)
            tc.setItalic(true);
        if ((flags & 2) == 2)
            tc.setStrikethrough(true);
        if ((flags & 1) == 1)
            tc.setObfuscated(true);
        return tc;
    }

    public static String message(String message) {
        return ChatColor.BLUE + "> " + ChatColor.GRAY +  message;
    }

    public static BaseComponent messageComponent(String message) {
        BaseComponent bc = new TextComponent("> ");
        bc.setColor(ChatColor.BLUE);
        TextComponent component = new TextComponent(message);
        component.setColor(ChatColor.GRAY);
        bc.addExtra(component);
        return bc;
    }

    public static BaseComponent generateFormattedChat(String message, ChatColor color) {
        return generateFormattedChat(message, color, 0);
    }

    public static BaseComponent generateItalicChat(String message, ChatColor color) {
        return generateFormattedChat(message, color, 4);
    }

    public static BaseComponent generateBoldChat(String message, ChatColor color) {
        return generateFormattedChat(message, color, 8);
    }

    public static BaseComponent generateHyperlink(BaseComponent toDisplay, String hyperlink, BaseComponent... hoverText) {
        TextComponent textComponent = new TextComponent(toDisplay);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, hyperlink);
        textComponent.setHoverEvent(hoverEvent);
        textComponent.setClickEvent(clickEvent);
        return textComponent;
    }

    public static BaseComponent generateHyperlink(String toDisplay, String hyperlink, BaseComponent... hoverText) {
        return generateHyperlink(new TextComponent(toDisplay), hyperlink, hoverText);
    }

    public static void sendMultiple(Player player, BaseComponent... components) {
        for (BaseComponent component : components) {
            player.spigot().sendMessage(component);
        }
    }
}
