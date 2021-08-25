package com.mrkirby153.kcuhc.tablist;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.mrkirby153.kcuhc.Strings;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TabListHandler implements Listener {

    private UHC uhc;

    private ProtocolManager pm;

    public TabListHandler(UHC uhc) {
        this.uhc = uhc;
        this.pm = ProtocolLibrary.getProtocolManager();
        uhc.getServer().getPluginManager().registerEvents(this, uhc);
    }


    public PacketContainer constructPacket(String header, String footer) {
        PacketContainer packet = this.pm.createPacket(Server.PLAYER_LIST_HEADER_FOOTER);
        WrappedChatComponent wcpHeader = WrappedChatComponent.fromText(header);
        WrappedChatComponent wcpFooter = WrappedChatComponent.fromText(footer);
        packet.getChatComponents().write(0, wcpHeader);
        packet.getChatComponents().write(1, wcpFooter);
        return packet;
    }

    public void sendHeaderFooter(Player player, String header, String footer) {
        this.uhc.protocolLibManager.sendPacket(player, constructPacket(header, footer));
    }

    public void sendHeaderFooter(Player player) {
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "  "+ Strings.LONG_NAME+"  \n";
        String footer =
            "\n" + ChatColor.GRAY + "Time: " + ChatColor.WHITE + Time.now()
                + ChatColor.GRAY + " \n";
        if (uhc.getGame().getCurrentState() == GameState.ALIVE) {
            long initialPlayers = this.uhc.getGame().getInitialPlayers();
            long alivePlayers = this.uhc.getGame().getTeams().values().stream()
                .mapToLong(t -> t.getPlayers().size()).sum();
            footer += "Players: " + ChatColor.WHITE + alivePlayers + "/" + initialPlayers + ChatColor.GRAY + " | Kills: " + ChatColor.WHITE + this.uhc.getGame().getKills(player);
        }
        int ping = player.getPing();
        footer +=
            ChatColor.DARK_GRAY + "\nPing: " + getPingBarColor(ping) + ping + "ms" + ChatColor.WHITE
                + " | " + ChatColor.DARK_GRAY + "TPS: " + ChatColor.WHITE + Time.trim(1, getTps()[0]);
        ScoreboardTeam team = this.uhc.getGame().getTeam(player);
        if (team != null) {
            if (!(team instanceof SpectatorTeam)) {
                footer += ChatColor.WHITE + "\n\nTeam: " + team.getColor() + team.getTeamName();
            }
        }
        sendHeaderFooter(player, header, footer);
    }

    private ChatColor getPingBarColor(int time) {
        if (time < 150) {
            return ChatColor.GREEN;
        }
        if (time < 300) {
            return ChatColor.YELLOW;
        }
        if (time < 600) {
            return ChatColor.GOLD;
        }
        if (time < 1000) {
            return ChatColor.RED;
        }
        return ChatColor.DARK_RED;
    }

    private double[] getTps() {
        try {
            Class mcServer = Class.forName("net.minecraft.server.MinecraftServer");
            Field f = mcServer.getField("recentTps");
            Method m = mcServer.getDeclaredMethod("getServer");
            Object server = m.invoke(null);
            return (double[]) f.get(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
        double def[] = {-1, -1, -1};
        return def;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SLOW) {
            Bukkit.getOnlinePlayers().forEach(this::sendHeaderFooter);
        }
    }
}
