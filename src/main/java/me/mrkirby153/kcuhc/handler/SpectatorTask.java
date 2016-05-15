package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_9_R2.IChatBaseComponent;
import net.minecraft.server.v1_9_R2.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectatorTask implements Runnable, Listener {

    private HashMap<UUID, UUID> spectatorTargets = new HashMap<>();

    public SpectatorTask(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 1L);
    }

    @Override
    public void run() {
        List<Player> players = TeamHandler.spectatorsTeam().getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).collect(Collectors.toList());
        for (Player p : players) {
            UUID target = spectatorTargets.get(p.getUniqueId());
            Entity currentTarget = p.getSpectatorTarget();
            if (target == null) {
                sendActionBar(p, ChatColor.GOLD + "Right click a player to spectate!");
                continue;
            }
            if (currentTarget == null || !currentTarget.getUniqueId().equals(target)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(true);
                p.teleport(Bukkit.getPlayer(target));
                spectatorTargets.remove(p.getUniqueId());
            }
            if (Bukkit.getPlayer(target) == null) {
                spectatorTargets.remove(p.getUniqueId());
                continue;
            }
            UHCTeam team = TeamHandler.getTeamForPlayer(Bukkit.getPlayer(target));
            if (team == null) {
                sendActionBar(p, "Spectating " + Bukkit.getPlayer(target).getName());
            } else {
                sendActionBar(p, "Spectating " + team.getColor() + Bukkit.getPlayer(target).getName());
            }
        }
    }

    @EventHandler
    public void entityInteract(PlayerInteractEntityEvent event) {
        if (!TeamHandler.isSpectator(event.getPlayer())) {
            return;
        }
        Entity rightClicked = event.getRightClicked();
        if (!(rightClicked instanceof Player)) {
            return;
        }
        Player toSpectate = (Player) rightClicked;
        if(TeamHandler.isSpectator(toSpectate))
            return;
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        event.getPlayer().setSpectatorTarget(toSpectate);
        event.getPlayer().spigot().sendMessage(UtilChat.generateFormattedChat("Sneak to stop spectating", ChatColor.GRAY, 0));
        spectatorTargets.put(event.getPlayer().getUniqueId(), toSpectate.getUniqueId());
    }

    private void sendActionBar(Player player, String message) {
        PacketPlayOutChat chat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}"), (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(chat);
    }
}
