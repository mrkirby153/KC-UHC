package com.mrkirby153.kcuhc.module.head;

import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

public class HeadAppleModule extends UHCModule {

    private static Recipe HEAD_APPLE_RECIPE = new ShapedRecipe(
        new ItemFactory(Material.GOLDEN_APPLE)
            .enchantment(Enchantment.ARROW_DAMAGE, 1)
            .name(ChatColor.AQUA + "Head Apple").construct())
        .shape("GGG", "GHG", "GGG")
        .setIngredient('G', Material.GOLD_INGOT)
        .setIngredient('H',
            new MaterialData(Material.SKULL_ITEM, (byte) SkullType.PLAYER.ordinal()));

    private static Predicate<Recipe> IS_HEAD_APPLE = recipe ->
        recipe.getResult().getType() == Material.GOLDEN_APPLE
            && recipe.getResult().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE);

    private UHC uhc;

    @Inject
    public HeadAppleModule(UHC uhc) {
        super("Head Apple", "Player heads can be crafted into head apples", Material.APPLE);
        this.uhc = uhc;
    }

    @EventHandler
    public void consumeHeadApple(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getItemMeta().getDisplayName() == null) {
            return;
        }
        if (!item.getItemMeta().getDisplayName().contains("Head")) {
            return;
        }
        if (!item.getEnchantments().containsKey(Enchantment.ARROW_DAMAGE)) {
            return;
        }
        if (item.getType() != Material.GOLDEN_APPLE) {
            return;
        }

        event.getPlayer().spigot().sendMessage(Chat.INSTANCE.message("You ate a head apple"));
        ScoreboardTeam team = this.uhc.getGame().getTeam(event.getPlayer());
        if (team != null) {
            if (team instanceof UHCTeam) {
                team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
                    .forEach(p -> {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));

                        if (p.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                            p.spigot().sendMessage(Chat.INSTANCE.message(
                                "You have given your team Regeneration II and Absorption"));
                        } else {
                            p.spigot().sendMessage(Chat.INSTANCE.message("",
                                "{player} has given you Regeneration II and Absorption",
                                "{player}", event.getPlayer().getName()));
                        }
                    });
            }
        } else {
            Player p = event.getPlayer();
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
        }
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
                if (playerHead.getType() == Material.SKULL_ITEM) {
                    playerName = ((SkullMeta) playerHead.getItemMeta()).getOwner();
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
