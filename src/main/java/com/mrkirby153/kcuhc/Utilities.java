package com.mrkirby153.kcuhc;

import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.reflections.Reflections;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class Utilities {

    private static final String GOLDEN_HEAD_URL = "http://textures.minecraft.net/texture/3bb612eb495ede2c5ca5178d2d1ecf1ca5a255d25dfc3c254bc47f6848791d8";
    public static final UUID HEAD_UUID = UUID.fromString("1e1eb54b-65e9-49f3-91cf-c11be6fdc401");

    public static ItemStack getGoldenHead() {
        ItemStack item = new ItemFactory(Material.PLAYER_HEAD).name(ChatColor.GOLD + "" + ChatColor.BOLD + "Golden Head").construct();
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if(meta != null) {
            try {
                Object gameProfile = getGameProfile(HEAD_UUID);
                byte[] data = Base64.getEncoder().encode(
                    String.format("{textures:{SKIN:{url:\"%s\"}}}", GOLDEN_HEAD_URL).getBytes());

                Object profileProperties = Reflections.invoke(gameProfile, "getProperties");

                Object property = constructSignedProperty("textures", new String(data));
                Reflections
                    .invoke(profileProperties, "put", "textures", property);

                Reflections.set(meta, "profile", gameProfile);
            } catch (Exception e) {
                meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "$$ ERROR $$");
                ArrayList<String> lore = new ArrayList<>();
                lore.add("An error occurred when creating this head: ");
                lore.add(ChatColor.RED + e.getClass().getCanonicalName());
                lore.add(ChatColor.GOLD + e.getMessage());
                meta.setLore(lore);
                e.printStackTrace();
            }
            item.setItemMeta(meta);
        }
        return item;
    }


    private static Object getGameProfile(UUID uuid)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor constructor = gameProfileClass.getConstructor(UUID.class, String.class);

        return constructor.newInstance(uuid, null);
    }

    private static Object constructSignedProperty(String key, String value)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class propertyConstructor = Class.forName("com.mojang.authlib.properties.Property");
        Constructor constructor = propertyConstructor.getConstructor(String.class, String.class);

        return constructor.newInstance(key, value);
    }
}
