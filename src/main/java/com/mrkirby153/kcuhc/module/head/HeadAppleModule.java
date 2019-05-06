package com.mrkirby153.kcuhc.module.head;

import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.Utilities;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.reflections.Reflections;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HeadAppleModule extends UHCModule {

    private static Predicate<Recipe> IS_HEAD_APPLE = recipe ->
        recipe.getResult().getType() == Material.GOLDEN_APPLE
            && recipe.getResult().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE);
    private Recipe HEAD_APPLE_RECIPE;
    private UHC uhc;

    @Inject
    public HeadAppleModule(UHC uhc) {
        super("Head Apple", "Player heads can be crafted into head apples", Material.APPLE);
        this.uhc = uhc;
        HEAD_APPLE_RECIPE = new ShapedRecipe(new NamespacedKey(uhc, "golden-head"),
            Utilities.getGoldenHead())
            .shape("GGG", "GHG", "GGG")
            .setIngredient('G', Material.GOLD_INGOT)
            .setIngredient('H', Material.PLAYER_HEAD);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (!isHeadApple(item)) {
            return;
        }

        event.getPlayer().spigot().sendMessage(Chat.message("You ate a head apple"));
        event.getPlayer()
            .playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BURP, 1F, 1F);
        item.setAmount(item.getAmount() - 1);
        List<Player> affectedPlayers = new ArrayList<>();

        ScoreboardTeam team = this.uhc.getGame().getTeam(event.getPlayer());
        if (team != null) {
            affectedPlayers.addAll(team.getPlayers().stream().map(Bukkit::getPlayer).filter(
                Objects::nonNull).collect(Collectors.toList()));
        } else {
            affectedPlayers.add(event.getPlayer());
        }

        affectedPlayers.forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));

            if (p.getUniqueId() != event.getPlayer().getUniqueId()) {
                p.spigot().sendMessage(
                    Chat.message("", "{player} has given you Regeneration II and Absorption",
                        "{player}", event.getPlayer().getName()));
            }
        });
    }

    private boolean isHeadApple(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD) {
            return false;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        Object gameProfile = Reflections.get(meta, "profile");
        UUID uuid = Reflections.invoke(gameProfile, "getId");
        return uuid.equals(Utilities.HEAD_UUID);
    }

    @Override
    public void onLoad() {
        if (!Iterators.any(Bukkit.recipeIterator(), IS_HEAD_APPLE::test)) {
            Bukkit.addRecipe(HEAD_APPLE_RECIPE);
        }
    }

    @Override
    public void onUnload() {
        Iterators.removeIf(Bukkit.recipeIterator(), IS_HEAD_APPLE::test);
    }

    @EventHandler
    public void prepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null) {
            if (event.getRecipe().getResult().containsEnchantment(Enchantment.ARROW_DAMAGE)
                && event.getRecipe().getResult().getType() == Material.GOLDEN_APPLE) {
                // Get player head
                ItemStack playerHead = event.getInventory().getMatrix()[4];
                String playerName = null;
                if (playerHead.getType() == Material.PLAYER_HEAD) {
                    playerName = ((SkullMeta) playerHead.getItemMeta()).getOwningPlayer().getName();
                }
                ItemStack output = event.getInventory().getResult();
                ItemMeta meta = output.getItemMeta();
                meta.setLore(Collections.singletonList(
                    ChatColor.WHITE + "Crafted with the head of " + ChatColor.GOLD + playerName));
                output.setItemMeta(meta);
                event.getInventory().setResult(output);
            }
        }
    }
}
