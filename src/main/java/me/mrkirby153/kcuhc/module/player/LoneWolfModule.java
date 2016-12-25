package me.mrkirby153.kcuhc.module.player;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.command.LoneWolfCommand;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTitle;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLoneWolf;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.*;

public class LoneWolfModule extends UHCModule {

    private static final List<UUID> queuedLoneWolves = new ArrayList<>();
    private static final int MIN_BLOCKS = 3;

    private HashSet<UUID> loneWolves = new HashSet<>();
    private HashSet<net.md_5.bungee.api.ChatColor> chatColors = new HashSet<>();
    private TeamHandler teamHandler;

    private LoneWolfTeam loneWolfTeam;

    public LoneWolfModule(TeamHandler handler) {
        super(Material.BONE, 0, "Lone Wolf", false, "Adds lone wolves - players who start alone");
        this.teamHandler = handler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if(event.getTo() == UHCArena.State.ENDGAME)
            reset();
    }

    public void addLoneWolf(Player player) {
        loneWolves.add(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "" + org.bukkit.ChatColor.STRIKETHROUGH + org.bukkit.ChatColor.BOLD + "=============================================");
        player.sendMessage("You are a lone wolf.");
        player.sendMessage(" * Lone wolves start the game alone, however, once they are within " + MIN_BLOCKS + " blocks of another team, they automatically join that team");
        if (getPlugin().arena.getProperties().LONE_WOLF_CREATES_TEAMS.get()) {
            player.sendMessage(" * Also, if you encounter another lone wolf, you will team with them as well");
        }
        player.sendMessage(" * Please do " + ChatColor.BOLD + "NOT" + ChatColor.WHITE + " communicate with the other teams until you find them");
        player.sendMessage(ChatColor.GREEN + "" + org.bukkit.ChatColor.STRIKETHROUGH + org.bukkit.ChatColor.BOLD + "=============================================");
        teamHandler.joinTeam(loneWolfTeam, player);
    }

    public UHCTeam getLoneWolfTeam() {
        return loneWolfTeam;
    }

    public HashSet<UUID> getLoneWolves() {
        return loneWolves;
    }

    @Override
    public void onDisable() {
        teamHandler.unregisterTeam(loneWolfTeam);
        for(Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 1F, 1F);
        }
        Bukkit.broadcastMessage(UtilChat.message("Lone Wolves disabled!"));
    }

    @Override
    public void onEnable() {
        loneWolfTeam = new LoneWolfTeam();
        teamHandler.registerTeam(loneWolfTeam);

        for(Player p : Bukkit.getOnlinePlayers()){
            p.playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 1F, 1F);
        }
        Bukkit.broadcastMessage(UtilChat.message("Lone Wolves enabled!"));

        CommandManager.instance().registerCommand(new LoneWolfCommand(this, getPlugin()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.TWO_SECOND) {
            // Only work when the game is running
            if (queuedLoneWolves.size() > 0)
                System.out.println("Removing " + queuedLoneWolves.toString());
            loneWolves.removeAll(queuedLoneWolves);
            queuedLoneWolves.clear();
            if (getPlugin().arena.currentState() != UHCArena.State.RUNNING)
                return;
            loneWolves.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(loneWolf -> {
                if (queuedLoneWolves.contains(loneWolf.getUniqueId()))
                    return;
                Player target = null;
                for (Player p : getPlugin().arena.players()) {
                    if (p.getUniqueId().equals(loneWolf.getUniqueId()))
                        continue;
                    if (p.getLocation().distanceSquared(loneWolf.getLocation()) <= Math.pow(10, 2)) {
                        target = p;
                        break;
                    }
                }
                if (target != null) {
                    UHCTeam team = teamHandler.getTeamForPlayer(target);
                    if (team == null)
                        return;
                    if (team instanceof LoneWolfTeam) {
                        if (getPlugin().arena.getProperties().LONE_WOLF_CREATES_TEAMS.get()) {
                            team = new UHCPlayerTeam(loneWolf.getName() + "-" + target.getName(), getChatColor());
                            team.setFriendlyName(target.getName() + " & " + loneWolf.getName());
                            teamHandler.registerTeam(team);
                            teamHandler.joinTeam(team, target);

                            if (UHC.uhcNetwork != null) {
                                HashMap<UUID, String> toassign = new HashMap<>();
                                toassign.put(target.getUniqueId(), team.getTeamName());
                                new BotCommandNewTeam(getPlugin().serverId(), team.getTeamName()).publishBlocking();
                                new BotCommandAssignTeams(getPlugin().serverId(), null, toassign).publish();
                            }
                        } else {
                            return;
                        }
                    }
                    if (!getPlugin().arena.getProperties().LONE_WOLF_CREATES_TEAMS.get()
                            || team.getPlayers().size() < getPlugin().arena.getProperties().LONE_WOLF_TEAM_SIZE.get()) {
                        assignLoneWolf(loneWolf, team);
                    }
                }
            });
        }
    }

    public void removeLoneWolf(Player player) {
        queuedLoneWolves.add(player.getUniqueId());
    }

    public void reset() {
        queuedLoneWolves.clear();
        loneWolves.clear();
        chatColors.clear();
    }

    private void assignLoneWolf(Player loneWolf, UHCTeam team) {
        queuedLoneWolves.add(loneWolf.getUniqueId());
        ModuleRegistry.getLoadedModule(TeamInventoryModule.class).ifPresent(inv -> inv.takeInventoryItem(loneWolf));
        teamHandler.leaveTeam(loneWolf);
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> teamHandler.joinTeam(team, loneWolf), 10);
        team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(teamMember -> {
            teamMember.playSound(teamMember.getLocation(), Sound.ENTITY_WOLF_HOWL, SoundCategory.MASTER, 1F, 1F);
            teamMember.spigot().sendMessage(C.m(getName(), loneWolf.getName() + " has joined your team!"));
        });
        if (UHC.uhcNetwork != null) {
            getPlugin().getLogger().info("[LW] Assigning discord team to " + team);
            new BotCommandLoneWolf(getPlugin().serverId(), loneWolf.getUniqueId(), BotCommandLoneWolf.Command.UNASSIGN).publishBlocking();
            HashMap<UUID, String> teams = new HashMap<>();
            teams.put(loneWolf.getUniqueId(), team.getTeamName());
            new BotCommandAssignTeams(getPlugin().serverId(), null, teams).publish();
        }
        getPlugin().getLogger().info("[LW] " + loneWolf.getName() + " has joined the team " + team.getTeamName());
        for (Player p : Bukkit.getOnlinePlayers()) {
            UtilTitle.title(p, ChatColor.GOLD + loneWolf.getName(), "has joined team " + team.getFriendlyName());
        }
    }

    private net.md_5.bungee.api.ChatColor getChatColor() {
        net.md_5.bungee.api.ChatColor color;
        do {
            color = net.md_5.bungee.api.ChatColor.values()[new Random().nextInt(16)];
        } while (chatColors.contains(color));
        chatColors.add(color);
        return color;
    }

}
