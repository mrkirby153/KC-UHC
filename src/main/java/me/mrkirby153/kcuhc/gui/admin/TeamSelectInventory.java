package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.shop.Shop;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class TeamSelectInventory extends Shop<UHC> {

    private Mode mode = Mode.SELECT_TEAM;
    private UHCTeam team;

    public TeamSelectInventory(UHC module, Player player) {
        super(module, player, 6, "Team Selection");
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
        addButton(45, new ShopItem(Material.ARROW, "Back"), (player, clickType) -> {
            switch (mode) {
                case SELECT_TEAM:
                    player.closeInventory();
                    new GameSettingsInventory(module, player);
                    break;
                case SELECT_PLAYERS:
                    mode = Mode.SELECT_TEAM;
                    team = null;
                    build();
            }
        });
        addButton(3, new ShopItem(Material.DIAMOND_SWORD, "Two Teams"), (player, clickType) -> {
            // Create two random teams
            ArrayList<UHCTeam> te = new ArrayList<>(TeamHandler.teams());
            te.forEach(TeamHandler::unregisterTeam);
            TeamHandler.registerTeam("Red", ChatColor.RED);
           TeamHandler.registerTeam("Blue", ChatColor.BLUE);

            int playersOnline = Bukkit.getOnlinePlayers().size();
            double playersPerTeam = Math.floor(playersOnline / 2D);
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]);
            ArrayList<UUID> assignedAlready = new ArrayList<>();
            Random random = new Random();
            TeamHandler.teams().forEach(t -> {
                for (int i = 0; i < playersPerTeam; i++) {
                    Player p;
                    do {
                        p = players[random.nextInt(players.length)];
                    } while (assignedAlready.contains(p.getUniqueId()));
                    TeamHandler.joinTeam(t, p);
                    assignedAlready.add(p.getUniqueId());
                }
            });
            build();
        });
        addButton(5, new ShopItem(Material.GOLD_SWORD, "Single Person Teams"), (player, clickType) -> {
            // Create one team per player
            ArrayList<UHCTeam> te = new ArrayList<>(TeamHandler.teams());
            te.forEach(TeamHandler::unregisterTeam);

            String charCodes = "0123456789abcde";
            Random random = new Random();
            for (Player p : Bukkit.getOnlinePlayers()) {
                TeamHandler.registerTeam(p.getName().toLowerCase(), ChatColor.getByChar(charCodes.charAt(random.nextInt(charCodes.length()))));
                TeamHandler.joinTeam(TeamHandler.getTeamByName(p.getName().toLowerCase()), p);
            }
            build();
        });
    }

    private void buildPlayerSelect() {
        int row = 2;
        int col = 2;
        addButton(4, new ShopItem(Material.WOOL, getDye(team).getData(), 1, team.getColor() + WordUtils.capitalizeFully(team.getFriendlyName()), new String[0]), null);
        for (Player p : Bukkit.getOnlinePlayers()) {
            int slot = (9 * (row - 1)) + col - 1;
            ShopItem item = playerItem(p);
            if (TeamHandler.getTeamForPlayer(p) != null && team.getName().equalsIgnoreCase(TeamHandler.getTeamForPlayer(p).getName())) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "[MEMBER] " + item.getName());
                item.setItemMeta(meta);
            }
            addButton(slot, item, (player, clickType) -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
                TeamHandler.leaveTeam(p);
                TeamHandler.joinTeam(team, p);
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

        for (UHCTeam team : TeamHandler.teams()) {
            int slot = (9 * (row - 1)) + col - 1;
            addButton(slot, new ShopItem(Material.WOOL, getDye(team).getData(), 1, team.getColor() + WordUtils.capitalizeFully(team.getFriendlyName()), new String[0]),
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

    private ShopItem playerItem(Player player) {
        ShopItem item = new ShopItem(Material.SKULL_ITEM, (byte) 3, 1, player.getName(), new String[0]);
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

    private void setTeamWool(int row, UHCTeam team) {
        getInventory().setItem(row * 9, new ShopItem(Material.WOOL, getDye(team).getData(), 1, team.getColor() + WordUtils.capitalizeFully(team.getFriendlyName()), new String[0]));
    }

    enum Mode {
        SELECT_TEAM,
        SELECT_PLAYERS
    }
}
