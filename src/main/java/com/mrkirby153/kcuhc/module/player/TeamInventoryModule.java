package com.mrkirby153.kcuhc.module.player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class TeamInventoryModule extends UHCModule {

    private HashMap<UHCTeam, Inventory> teamInventories = new HashMap<>();

    private UHCGame game;

    @Inject
    public TeamInventoryModule(UHCGame game) {
        super("Team Inventory", "A shared inventory between team members", Material.ENDER_CHEST);
        this.game = game;
    }

    /**
     * Drops the inventory at the given location
     *
     * @param team     The team to drop
     * @param location The location
     */
    public void dropInventory(UHCTeam team, Location location) {
        Inventory inv = getInventory(team);
        if (inv == null) {
            return;
        }
        Arrays.stream(inv.getContents()).filter(Objects::nonNull)
            .forEach(i -> location.getWorld().dropItemNaturally(location, i));
    }

    @EventHandler
    public void gameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            this.game.getTeams().values().forEach(
                t -> teamInventories.put(t,
                    Bukkit.createInventory(null, 9 * 2, "Team Inventory: " + t.getTeamName())));
        }
        if (event.getTo() == GameState.ENDING) {
            this.teamInventories.forEach((team, inventory) -> inventory.clear());
            this.teamInventories.clear();
        }
    }

    /**
     * Returns the player's team inventory
     *
     * @param player The player
     *
     * @return The inventory
     */
    public Inventory getInventory(Player player) {
        if (game.getTeam(player) != null && !(game.getTeam(player) instanceof SpectatorTeam)) {
            return getInventory((UHCTeam) game.getTeam(player));
        }
        if (game.getTeam(player) instanceof SpectatorTeam) {
            player.spigot()
                .sendMessage(Chat.INSTANCE.error("Spectators do not have a team inventory!"));
            return null;
        }
        if (game.getTeam(player) == null) {
            player.spigot().sendMessage(Chat.INSTANCE.error("You are not on a team!"));
            return null;
        }
        return null;
    }

    /**
     * Gets a team inventory for the team
     *
     * @param team The team
     *
     * @return The inventory
     */
    public Inventory getInventory(UHCTeam team) {
        return teamInventories.get(team);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        ScoreboardTeam team = this.game.getTeam(event.getEntity());
        if (team == null) {
            return;
        }
        if (team instanceof SpectatorTeam) {
            return;
        }
        if (team.getPlayers().size() <= 1) {
            dropInventory((UHCTeam) team, event.getEntity().getLocation());
        }
    }

    @CommandAlias("teaminv|ti")
    public static class TeamInventoryCommand extends BaseCommand {

        private UHC uhc;

        @Inject
        public TeamInventoryCommand(UHC uhc) {
            this.uhc = uhc;
        }

        @Default
        public void openTeamInventory(Player player) {
            // TODO: 7/27/2017 Add restriction to running game
            if (!ModuleRegistry.INSTANCE.loaded(TeamInventoryModule.class)) {
                player.sendMessage(
                    Chat.INSTANCE.error("Team inventories are not enabled").toLegacyText());
                return;
            }
            if (uhc.getGame().getCurrentState() != GameState.ALIVE) {
                player.sendMessage(
                    Chat.INSTANCE.error("You cannot open the team inventory before the game starts")
                        .toLegacyText());
                return;
            }
            Inventory inventory = ModuleRegistry.INSTANCE.getModule(TeamInventoryModule.class)
                .getInventory(player);
            if (inventory != null) {
                player.openInventory(inventory);
            }
        }
    }
}
