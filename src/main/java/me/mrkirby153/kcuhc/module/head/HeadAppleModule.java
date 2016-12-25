package me.mrkirby153.kcuhc.module.head;

import com.google.common.collect.Iterators;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.function.Predicate;

public class HeadAppleModule extends UHCModule {

    private static Recipe HEAD_APPLE_RECIPE = new ShapedRecipe(new ItemFactory(Material.GOLDEN_APPLE).enchantment(Enchantment.ARROW_DAMAGE, 1).name(ChatColor.AQUA + "Head Apple").construct())
            .shape("GGG", "GHG", "GGG").setIngredient('G', Material.GOLD_INGOT).setIngredient('H', new MaterialData(Material.SKULL_ITEM, (byte) SkullType.PLAYER.ordinal()));

    private static Predicate<Recipe> IS_HEAD_APPLE = recipe -> recipe.getResult().getType() == Material.GOLDEN_APPLE && recipe.getResult().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE);

    public HeadAppleModule() {
        super(Material.APPLE, 0, "Head Apples", false, "Player heads can be crafted into head apples");
        addDepends(DropPlayerHeadModule.class);
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Enabling head apples!"));
        if (!Iterators.any(Bukkit.recipeIterator(), IS_HEAD_APPLE::test)) {
            Bukkit.addRecipe(HEAD_APPLE_RECIPE);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("Disabling head apples"));
        Iterators.removeIf(Bukkit.recipeIterator(), IS_HEAD_APPLE::test);
    }

    @EventHandler
    public void consumeHeadApple(PlayerItemConsumeEvent event) {
        if (event.getItem().getItemMeta().getDisplayName() == null)
            return;
        if (!event.getItem().getItemMeta().getDisplayName().contains("Head"))
            return;
        if (!event.getItem().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE))
            return;
        if (event.getItem().getType() != Material.GOLDEN_APPLE) {
            return;
        }

        event.getPlayer().sendMessage(UtilChat.message("You ate a head apple"));

        UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(event.getPlayer());
        if (team != null) {
            team.getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).forEach(p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));

                if (p.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                    p.sendMessage(UtilChat.message("You have given your team Regeneration II and Absorption!"));
                } else {
                    p.sendMessage(UtilChat.message(ChatColor.GOLD + event.getPlayer().getName() + ChatColor.GRAY + " ate a head apple, giving you Regeneration II and Absorption!"));
                }
            });
        } else {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
            event.getPlayer().sendMessage(UtilChat.message("You are not on a team so only you get the effects"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void itemPickup(PlayerPickupItemEvent event) {
        // Display player head info
        if (event.getItem().getItemStack().getType() == Material.SKULL_ITEM && event.getItem().getItemStack().getDurability() == 3) {
            Player player = event.getPlayer();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.5F);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "You've picked up a player head!");
            player.sendMessage(ChatColor.WHITE + "You can use this head to craft a Head Apple for healing");
            player.sendMessage(ChatColor.WHITE + "A golden head will give you 2x the effects of a golden apple!");
            player.sendRawMessage(ChatColor.GREEN + "To Craft: " + ChatColor.WHITE + "Use the recipe for a Golden Apple, but replace the apple with the head");
            player.sendMessage("");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
        }
    }

}
