package com.mrkirby153.kcuhc.module.respawner;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;
import javax.annotation.Nullable;

public class SoulVialHandler {

    private static SoulVialHandler instance;

    private HashMap<ItemStack, UUID> soulVials = new HashMap<>();
    private HashMap<ItemStack, UHCTeam> soulVialTeams = new HashMap<>();
    private UHC plugin;

    // Private constructor
    @Inject
    private SoulVialHandler(UHC plugin) {
        this.plugin = plugin;
    }

    public static SoulVialHandler getInstance() {
        if (instance == null) {
            instance = UHC.injector.getInstance(SoulVialHandler.class);
        }
        return instance;
    }

    public ItemStack getSoulVial(Player player) {
        ItemStack stack = new ItemFactory(Material.EXPERIENCE_BOTTLE)
            .name(ChatColor.GREEN + "Soul Vial: " + ChatColor.YELLOW + "(" + player.getName() + ")")
            .lore("Place this in a teammate respawner to revive this teammate").construct();
        this.soulVials.put(stack, player.getUniqueId());
        ScoreboardTeam team = this.plugin.getGame().getTeam(player);
        if (team instanceof UHCTeam) {
            this.soulVialTeams.put(stack, (UHCTeam) team);
        }
        return stack;
    }

    public boolean isSoulVial(ItemStack itemStack) {
        return this.soulVials.keySet().contains(itemStack);
    }

    public void clearSoulVials() {
        this.soulVials.clear();
        this.soulVialTeams.clear();
    }

    public void useSoulVial(ItemStack stack) {
        this.soulVials.remove(stack);
        this.soulVialTeams.remove(stack);
    }

    public UHCTeam getTeam(ItemStack stack) {
        return this.soulVialTeams.get(stack);
    }

    @Nullable
    public Player getSoulVialContents(ItemStack itemStack) {
        UUID u = this.soulVials.get(itemStack);
        if (u == null) {
            return null;
        }
        return Bukkit.getPlayer(u);
    }
}
