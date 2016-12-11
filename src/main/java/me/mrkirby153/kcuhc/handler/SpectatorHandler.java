package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.handler.listener.SpectateListener;
import me.mrkirby153.kcuhc.shop.Inventory;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.Module;
import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectatorHandler extends Module<UHC> implements Runnable, Listener {

    private HashMap<UUID, UUID> spectatorTargets = new HashMap<>();

    private TeamHandler teamHandler;
    private UHC plugin;
    private SpectateListener spectateListener;

    public SpectatorHandler(UHC plugin, TeamHandler teamHandler) {
        super("Spectator Handler", "1.0", plugin);
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<Player> players = teamHandler.spectatorsTeam().getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).collect(Collectors.toList());
        for (Player p : players) {
            if (!p.getInventory().contains(Material.COMPASS) && plugin.arena.currentState() == UHCArena.State.RUNNING) {
                p.sendMessage(UtilChat.message("Giving you the spectate inventory as you no longer have it"));
                Inventory.closeInventory(p);
                new SpecInventory(plugin, p, teamHandler);
            }
            UUID target = spectatorTargets.get(p.getUniqueId());
            Entity currentTarget = p.getSpectatorTarget();
            if (target == null) {
                sendActionBar(p, ChatColor.GOLD + "Right click a player to spectate!");
                continue;
            }
            if (currentTarget == null || !currentTarget.getUniqueId().equals(target)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(true);
                if (Bukkit.getPlayer(target) != null)
                    p.teleport(Bukkit.getPlayer(target));
                spectatorTargets.remove(p.getUniqueId());
            }
            if (Bukkit.getPlayer(target) == null) {
                spectatorTargets.remove(p.getUniqueId());
                continue;
            }
            UHCTeam team = teamHandler.getTeamForPlayer(Bukkit.getPlayer(target));
            if (team == null) {
                sendActionBar(p, "Spectating " + Bukkit.getPlayer(target).getName());
            } else {
                sendActionBar(p, "Spectating " + team.getColor() + Bukkit.getPlayer(target).getName());
            }
        }
    }

    @EventHandler
    public void entityInteract(PlayerInteractEntityEvent event) {
        if (!teamHandler.isSpectator(event.getPlayer())) {
            return;
        }
        Entity rightClicked = event.getRightClicked();
        if (!(rightClicked instanceof Player)) {
            return;
        }
        Player toSpectate = (Player) rightClicked;
        if (teamHandler.isSpectator(toSpectate))
            return;
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        event.getPlayer().setSpectatorTarget(toSpectate);
        event.getPlayer().sendMessage(UtilChat.message("Sneak to stop spectating"));
        spectatorTargets.put(event.getPlayer().getUniqueId(), toSpectate.getUniqueId());
    }

    private void sendActionBar(Player player, String message) {
        PacketPlayOutChat chat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}"), (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(chat);
    }

    @Override
    protected void init() {
        registerListener(this);
        scheduleRepeating(this, 0L, 10L);
        spectateListener = new SpectateListener(teamHandler, plugin);
        registerListener(spectateListener);
    }
}
