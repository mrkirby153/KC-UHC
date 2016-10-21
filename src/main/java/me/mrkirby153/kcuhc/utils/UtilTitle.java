package me.mrkirby153.kcuhc.utils;


import net.minecraft.server.v1_10_R1.IChatBaseComponent;
import net.minecraft.server.v1_10_R1.Packet;
import net.minecraft.server.v1_10_R1.PacketPlayOutTitle;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class UtilTitle {

    public static void title(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        setTimings(player, fadeInTicks, stayTicks, fadeOutTicks);
        sendPacket(player, constructTitle(title));
        sendPacket(player, constructSubtitle(subtitle));
    }

    public static void title(Player player, String title, String subtitle) {
        sendPacket(player, constructTitle(title));
        sendPacket(player, constructSubtitle(subtitle));
    }

    private static void setTimings(Player player, int fadeIn, int stay, int fadeOut) {
        PacketPlayOutTitle timingsPacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
        sendPacket(player, timingsPacket);
    }

    private static PacketPlayOutTitle constructTitle(String title) {
        return new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", (title != null) ? title : "")));
    }

    private static PacketPlayOutTitle constructSubtitle(String subtitle) {
        return new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", (subtitle != null) ? subtitle : "")));
    }

    private static void sendPacket(Player player, Packet packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}
