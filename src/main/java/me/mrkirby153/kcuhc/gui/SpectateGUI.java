package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import me.mrkirby153.kcuhc.item.ExecutableItem;
import me.mrkirby153.kcuhc.item.Gui;
import me.mrkirby153.kcuhc.item.InventoryHandler;
import me.mrkirby153.kcuhc.item.UndropableItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpectateGUI extends Gui {

    public SpectateGUI() {
        super(null, -1);
    }

    @Override
    public String getName() {
        return "Choose a player to spectate:";
    }

    @Override
    public List<ExecutableItem> getItems() {
        int row = 1;
        int col = 1;
        List<ExecutableItem> items = new ArrayList<>();
        for (UHCTeam team : TeamHandler.teams()) {
            if (team == TeamHandler.spectatorsTeam())
                continue;
            if (team.getPlayers().size() != 0)
                for (UUID u : team.getPlayers()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) {
                        PlayerItem item = new PlayerItem(p, getDye(team));
                        item.setSlot(rowCol(row, col++));
                        items.add(item);
                    }
                }
            else {
                for (int i = 1; i <= 9; i++) {
                    EliminatedTeamItem teamItem = new EliminatedTeamItem(getDye(team));
                    teamItem.setSlot(rowCol(row, col++));
                    items.add(teamItem);
                }
            }
            row++;
            col = 1;
        }
        SpawnItem spawnItem = new SpawnItem();
        spawnItem.setSlot(rowCol(row, 5));
        items.add(spawnItem);
        return items;
    }

    private int rowCol(int row, int col) {
        if (row > 0)
            row--;
        if (col > 0)
            col--;
        int slot = row * 9;
        slot += col;
        return slot;
    }

    @Override
    public int rows() {
        return (TeamHandler.teams().size() - 1)+1;
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

    class PlayerItem extends ExecutableItem {

        private final Player who;

        public PlayerItem(Player player, DyeColor teamColor) {
            super(Material.WOOL, teamColor.getWoolData(), 1, TeamHandler.getTeamForPlayer(player).getColor() + player.getName(), Arrays.asList(ChatColor.RED + "\u2764: " + ChatColor.WHITE + (int) player.getHealth(),
                    String.format(ChatColor.GOLD + "Food: " + ChatColor.WHITE + "%.0f", (player.getFoodLevel() / 20D) * 100) + "%"), Action.INV_CLICK);
            who = player;
        }

        @Override
        public void execute(Player player, ExecutableItem.Action action) {
            player.teleport(who);
            InventoryHandler.instance().closeInventory(player);
        }
    }

    class EliminatedTeamItem extends ExecutableItem {

        public EliminatedTeamItem(DyeColor color) {
            super(Material.STAINED_GLASS_PANE, color.getWoolData(), 1, ChatColor.RED + "" + ChatColor.BOLD + "TEAM ELIMINATED!", null, Action.NO_ACTION);
        }

        @Override
        public void execute(Player player, Action action) {

        }
    }

    class SpawnItem extends ExecutableItem implements UndropableItem {

        public SpawnItem(){
            super(Material.NETHER_STAR, (short) 0, 1, ChatColor.GOLD+"Teleport to spawn", null, Action.INV_CLICK);
        }

        @Override
        public void execute(Player player, Action action) {
            player.teleport(UHC.arena.getCenter());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 0.25f);
        }
    }
}
