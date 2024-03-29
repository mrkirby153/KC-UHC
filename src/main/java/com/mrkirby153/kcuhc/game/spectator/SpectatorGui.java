package com.mrkirby153.kcuhc.game.spectator;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.gui.Gui;
import me.mrkirby153.kcutils.paginate.Paginator;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpectatorGui extends Gui<UHC> {

    private static final int ROWS_PER_PAGE = 5;
    private int page = 1;

    public SpectatorGui(UHC plugin) {
        super(plugin, 6, "Spectate");
    }

    @Override
    public void build() {
        clear();
        List<UHCTeam> teams = new ArrayList<>(this.getPlugin().getGame().getTeams().values());
        teams.sort(((o1, o2) -> o2.getPlayers().size() - o1.getPlayers().size()));
        Paginator<UHCTeam> paginator = new Paginator<>(teams, ROWS_PER_PAGE);

        teams = paginator.getPage(this.page);

        int row = 0;
        for (UHCTeam team : teams) {
            getInventory().setItem(row * 9,
                new ItemFactory(getWool(team))
                    .name(team.getColor() + WordUtils.capitalizeFully(team.getTeamName()))
                    .construct());
            int column = 1;
            if (team.getPlayers().size() > 0) {
                for (UUID uuid : team.getPlayers()) {
                    if (column > 8) {
                        continue;
                    }
                    OfflinePlayer player = Bukkit.getPlayer(uuid);
                    boolean offline = false;
                    if (player == null) {
                        player = Bukkit.getOfflinePlayer(uuid);
                        offline = true;
                    }
                    ItemStack skullItem = offline ? createOfflinePlayer(player.getName())
                        : createPlayerHead((Player) player);
                    OfflinePlayer finalPlayer = player;
                    addButton((row * 9) + column, skullItem, (p, click) -> {
                        if (finalPlayer instanceof Player) {
                            p.teleport(((Player) finalPlayer).getLocation());
                            p.sendMessage(
                                Chat.message("Spectate", "Teleported to {player}",
                                    "{player}", finalPlayer.getName()).toLegacyText());
                        }
                    });
                    column++;
                }
            } else {
                for (int i = 1; i <= 8; i++) {
                    getInventory().setItem((row * 9) + i, new ItemFactory(Material.BARRIER)
                        .name(ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED!").construct());
                }
            }
            row++;
        }
        if (page < paginator.getMaxPages()) {
            addButton(53,
                new ItemFactory(Material.ARROW).name("Page " + (page + 1)).amount(page + 1)
                    .construct(), (p, click) -> {
                    this.page += 1;
                    build();
                });
        }
        if (page > 1) {
            addButton(45,
                new ItemFactory(Material.ARROW).name("Page " + (page - 1)).amount(page - 1)
                    .construct(), (p, click) -> {
                    this.page -= 1;
                    build();
                });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.TWO_SECOND) {
            build();
        }
    }

    private ItemStack createOfflinePlayer(String name) {
        return new ItemFactory(Material.SKELETON_SKULL).name(name)
            .lore("", ChatColor.RED + "" + ChatColor.BOLD + "OFFLINE").construct();
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack itemStack = new ItemFactory(Material.PLAYER_HEAD)
            .name(player.getName())
            .lore(ChatColor.GREEN + "Health: " + ChatColor.RESET + (int) player.getHealth() + "/"
                    + (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),
                ChatColor.GREEN + "Food: " + ChatColor.RESET + String
                    .format("%s", (int) (100 * player.getFoodLevel() / 20D)) + "%",
                ChatColor.GREEN + "World: " + ChatColor.RESET + player.getLocation().getWorld()
                    .getName()).construct();

        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private Material getWool(UHCTeam team) {
        switch (team.getColor()) {
            case BLACK:
                return Material.BLACK_WOOL;
            case DARK_BLUE:
            case BLUE:
            case DARK_AQUA:
                return Material.BLUE_WOOL;
            case DARK_GREEN:
                return Material.GREEN_WOOL;
            case GREEN:
                return Material.LIME_WOOL;
            case DARK_RED:
            case RED:
                return Material.RED_WOOL;
            case DARK_PURPLE:
            case LIGHT_PURPLE:
                return Material.MAGENTA_WOOL;
            case GOLD:
            case YELLOW:
                return Material.YELLOW_WOOL;
            case GRAY:
                return Material.LIGHT_GRAY_WOOL;
            case DARK_GRAY:
                return Material.GRAY_WOOL;
            case AQUA:
                return Material.CYAN_WOOL;
            default:
                return Material.WHITE_WOOL;
        }
    }
}
