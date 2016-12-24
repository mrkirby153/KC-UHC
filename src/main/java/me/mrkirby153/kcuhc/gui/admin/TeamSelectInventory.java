package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class TeamSelectInventory extends Gui<UHC> {

    private Mode mode = Mode.SELECT_TEAM;
    private UHCTeam team;
    private TeamHandler teamHandler;

    public TeamSelectInventory(UHC module, TeamHandler teamHandler, Player player) {
        super(module, player, 6, "Team Selection");
        this.teamHandler = teamHandler;
        open();
    }

    @Override
    public void build() {
        getInventory().clear();
        actions.clear();
        switch (mode) {
            case SELECT_TEAM:
                buildTeamSelect();
                break;
            case SELECT_PLAYERS:
                buildPlayerSelect();
        }
        addButton(45, new ItemFactory(Material.ARROW).name("Back").construct(), (player, clickType) -> {
            switch (mode) {
                case SELECT_TEAM:
                    player.closeInventory();
                    new GameSettingsInventory(plugin, player);
                    break;
                case SELECT_PLAYERS:
                    mode = Mode.SELECT_TEAM;
                    team = null;
                    build();
            }
        });
        addButton(3, new ItemFactory(Material.DIAMOND_SWORD).name("Two Teams").lore("Create two teams and assign everyone randomly").construct(), (player, clickType) -> {
            // Create two random teams
            ArrayList<UHCTeam> te = new ArrayList<>(teamHandler.teams());
            te.stream().filter(e -> e instanceof UHCPlayerTeam).forEach(teamHandler::unregisterTeam);
            teamHandler.registerTeam(new UHCPlayerTeam("Red", ChatColor.RED));
            teamHandler.registerTeam(new UHCPlayerTeam("Blue", ChatColor.BLUE));


            int playersOnline = Bukkit.getOnlinePlayers().size();
            double playersPerTeam = Math.floor(playersOnline / 2D);
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]);
            ArrayList<UUID> assignedAlready = new ArrayList<>();
            Random random = new Random();
            teamHandler.teams().forEach(t -> {
                for (int i = 0; i < playersPerTeam; i++) {
                    Player p;
                    do {
                        p = players[random.nextInt(players.length)];
                    } while (assignedAlready.contains(p.getUniqueId()));
                    teamHandler.joinTeam(t, p);
                    assignedAlready.add(p.getUniqueId());
                }
            });
            // Assign everyone who's not on a team to lone wolves
            Bukkit.getOnlinePlayers().stream().filter(p -> teamHandler.getTeamForPlayer(p) == null).forEach(p -> {
                plugin.loneWolfHandler.addLoneWolf(p);
            });
            build();
        });
        addButton(5, new ItemFactory(Material.GOLD_SWORD).name("Single Person Teams").lore("Create a team for everyone online").construct(), (player, clickType) -> {
            // Create one team per player
            ArrayList<UHCTeam> te = new ArrayList<>(teamHandler.teams());
            te.stream().filter(t -> t instanceof UHCPlayerTeam).forEach(teamHandler::unregisterTeam);

            String charCodes = "0123456789abcde";
            Random random = new Random();
            for (Player p : Bukkit.getOnlinePlayers()) {
                teamHandler.registerTeam(new UHCPlayerTeam(p.getName().toLowerCase(), ChatColor.getByChar(charCodes.charAt(random.nextInt(charCodes.length())))));
                teamHandler.joinTeam(teamHandler.getTeamByName(p.getName().toLowerCase()), p);
            }
            build();
        });
        addButton(8, new ItemFactory(Material.APPLE).name("Lone Wolf Settings").construct(), (player, clickType)->{
            player.closeInventory();
            new LoneWolfSettingsInventory(plugin, player);
        });
    }

    private void buildPlayerSelect() {
        int row = 2;
        int col = 2;
        addButton(4, new ItemFactory(Material.WOOL).data(getDye(team).getWoolData()).name(team.getColor()+ WordUtils.capitalize(team.getFriendlyName())).construct(), null);
        for (Player p : Bukkit.getOnlinePlayers()) {
            int slot = (9 * (row - 1)) + col - 1;
            ItemStack item = playerItem(p);
            if (teamHandler.getTeamForPlayer(p) != null && team.getTeamName().equalsIgnoreCase(teamHandler.getTeamForPlayer(p).getTeamName())) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "[MEMBER] " + item.getItemMeta().getDisplayName());
                item.setItemMeta(meta);
            }
            addButton(slot, item, (player, clickType) -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
                teamHandler.leaveTeam(p);
                teamHandler.joinTeam(team, p);
                build();
            });
            col++;
            if (col > 8) {
                row++;
                col = 2;
            }
        }
    }

    private void buildTeamSelect() {
        int row = 2;
        int col = 2;

        for (UHCTeam team : teamHandler.teams()) {
            int slot = (9 * (row - 1)) + col - 1;
            addButton(slot, new ItemFactory(Material.WOOL).data(getDye(team).getWoolData()).name(team.getColor()+ WordUtils.capitalize(team.getFriendlyName())).construct(),
                    (player, clickType) -> setTeam(team));
            col++;
            if (col > 8) {
                row++;
                col = 2;
            }
        }
    }

    private DyeColor getDye(UHCTeam team) {
        switch (team.getColor()) {
            case BLACK:
                return DyeColor.BLACK;
            case DARK_BLUE:
                return DyeColor.BLUE;
            case BLUE:
                return DyeColor.CYAN;
            case DARK_GREEN:
                return DyeColor.GREEN;
            case GREEN:
                return DyeColor.LIME;
            case DARK_AQUA:
                return DyeColor.BLUE;
            case DARK_RED:
                return DyeColor.RED;
            case DARK_PURPLE:
                return DyeColor.MAGENTA;
            case GOLD:
                return DyeColor.YELLOW;
            case GRAY:
                return DyeColor.SILVER;
            case DARK_GRAY:
                return DyeColor.GRAY;
            case AQUA:
                return DyeColor.CYAN;
            case RED:
                return DyeColor.RED;
            case LIGHT_PURPLE:
                return DyeColor.MAGENTA;
            case YELLOW:
                return DyeColor.YELLOW;
            case WHITE:
                return DyeColor.WHITE;
            default:
                return DyeColor.WHITE;
        }
    }

    private ItemStack playerItem(Player player) {
        ItemStack item = new ItemFactory(Material.SKULL_ITEM).data(3).name(player.getName()).construct();
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player.getName());
        item.setItemMeta(meta);
        return item;
    }

    private void setTeam(UHCTeam team) {
        this.team = team;
        this.mode = Mode.SELECT_PLAYERS;
        build();
    }

    enum Mode {
        SELECT_TEAM,
        SELECT_PLAYERS
    }
}
