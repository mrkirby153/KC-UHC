package com.mrkirby153.kcuhc.scoreboard;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.player.PvPGraceModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Objective;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The scoreboard updater
 */
public class ScoreboardUpdater implements Listener {

    private final UHCGame game;
    private final UHC uhc;

    private final Map<UUID, UHCScoreboard> scoreboardMap = new HashMap<>();


    @Inject
    public ScoreboardUpdater(UHC plugin, UHCGame game) {
        this.game = game;
        this.uhc = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(this::drawScoreboard);
    }

    private void drawScoreboard(Player player) {
        UHCScoreboard scoreboard = this.scoreboardMap.get(player.getUniqueId());
        if (scoreboard == null) {
            this.scoreboardMap
                .put(player.getUniqueId(), scoreboard = new UHCScoreboard(this.uhc));
        }
        scoreboard.reset();
        UHCScoreboard finalScoreboard = scoreboard;
        switch (this.game.getCurrentState()) {
            case COUNTDOWN:
            case WAITING:
                scoreboard.add(" ");
                if (game.getCurrentState() == GameState.COUNTDOWN) {
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + " Starting...");
                } else {
                    scoreboard
                        .add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                }
                scoreboard.add(" ");
                scoreboard
                    .add("Players Online: " + ChatColor.GOLD + Bukkit.getOnlinePlayers().size());
                ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(mod -> {
                    if (mod.getMapper() != null) {
                        finalScoreboard.add(" ");
                        User u = mod.getMapper().getUser(player.getUniqueId());
                        if (u != null) {
                            finalScoreboard
                                .add(new ElementHeadedText(ChatColor.GREEN + "Linked To",
                                    ChatColor.GOLD + u.getName() + "#" + u.getDiscriminator()));
                        } else {
                            finalScoreboard.add(new ElementHeadedText(ChatColor.RED + "Unlinked",
                                "Discord not linked!"));
                        }
                    }
                });
                scoreboard.add(" ");
                break;
            case ALIVE:
                ModuleRegistry.INSTANCE.getLoadedModule(PvPGraceModule.class).ifPresent(mod -> {
                    if (mod.getGraceTimeRemaining() > 0) {
                        finalScoreboard.add(" ");
                        finalScoreboard.add(new ElementHeadedText(
                            ChatColor.AQUA + "" + ChatColor.BOLD + "PvP Enabled in", Time.INSTANCE
                            .format(1, mod.getGraceTimeRemaining(),
                                TimeUnit.FIT, TimeUnit.SECONDS)));
                    }
                });
                scoreboard.add(" ");
                // Teams alive
                ScoreboardTeam team = game.getTeam(player);
                if (team != null && !(team instanceof SpectatorTeam)) {
                    // Display the members
                    if (team.getPlayers().size() > 5) {
                        scoreboard.add(
                            ChatColor.GREEN + "Teammates Alive: " + ChatColor.WHITE + team
                                .getPlayers().size());
                    } else {
                        team.getPlayers().stream().map(Bukkit::getOfflinePlayer)
                            .sorted((o1, o2) -> {
                                if (o1 instanceof Player && !(o2 instanceof Player)) {
                                    return 1;
                                } else if (!(o1 instanceof Player) && o2 instanceof Player) {
                                    return -1;
                                } else {
                                    return o1.getName().compareTo(o2.getName());
                                }
                            }).forEach(p -> {
                            if (p instanceof Player) {
                                Player pl = (Player) p;
                                double healthPercent =
                                    pl.getHealth() / pl.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                                        .getValue();
                                ChatColor nameColor;
                                if (healthPercent < .25) {
                                    nameColor = ChatColor.RED;
                                } else if (healthPercent < 0.5) {
                                    nameColor = ChatColor.GOLD;
                                } else if (healthPercent < 0.75) {
                                    nameColor = ChatColor.YELLOW;
                                } else {
                                    nameColor = ChatColor.GREEN;
                                }
                                float abs = this.getAbsorption(pl);
                                finalScoreboard.add(
                                    nameColor + p.getName() + " " + ChatColor.RED + (int) (pl
                                        .getHealth() + abs));
                            } else {
                                finalScoreboard.add(ChatColor.GRAY + p.getName());
                            }
                        });
                    }
                    scoreboard.add(" ");
                }
                long aliveTeamCount = this.game.getTeams().values().stream()
                    .filter(t -> t.getPlayers().size() > 0).count();

                scoreboard
                    .add(
                        ChatColor.DARK_PURPLE + "Teams Alive: " + ChatColor.WHITE + aliveTeamCount);
                scoreboard.add(" ");
                ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class)
                    .ifPresent(worldBorderModule -> {
                        finalScoreboard.add(new ElementHeadedText(
                            ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border",
                            String.format("from -%.1f to +%.1f",
                                worldBorderModule.worldborderLoc()[0],
                                worldBorderModule.worldborderLoc()[0])));
                    });
                scoreboard.add(
                    new ElementHeadedText(ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed",
                        Time.INSTANCE.format(1, System.currentTimeMillis() - game.getStartTime(),
                            Time.TimeUnit.FIT)));
                break;

        }
        // Update the health objectives
        Objective tabList = scoreboard.getTablistHealth();
        Objective belowName = scoreboard.getBelowNameHealth();
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (tabList.getScore(p.getName()).getScore() == 0 && !p.isDead()) {
                tabList.getScore(p.getName()).setScore((int) p.getHealth());
            }
                belowName.getScore(p.getName()).setScore((int) (p.getHealth()+getAbsorption(p)));
        });
        scoreboard.draw();
        if (player.getScoreboard() != scoreboard.getBoard()) {
            player.setScoreboard(scoreboard.getBoard());
        }
    }

    private float getAbsorption(Player player) {
        if (getVersion().equals("unknown")) {
            return 0; // We couldn't determine the version
        }
        try {
            Class<?> craftPlayerClass = Class
                .forName("org.bukkit.craftbukkit." + getVersion() + ".entity.CraftPlayer");
            Method handleMethod = craftPlayerClass.getMethod("getHandle");
            handleMethod.setAccessible(true);

            Object entityPlayer = handleMethod.invoke(craftPlayerClass.cast(player));

            Class<?> entityPlayerClass = entityPlayer.getClass();
            Method absorptionMethod = entityPlayerClass.getMethod("getAbsorptionHearts");
            absorptionMethod.setAccessible(true);
            return (float) absorptionMethod.invoke(entityPlayer);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Fail silently
        }
        return 0;
    }

    private String getVersion() {
        try {
            return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                .split(",")[3];
        } catch (IndexOutOfBoundsException e) {
            return "unknown";
        }
    }
}
