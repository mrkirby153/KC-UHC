package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.core.entities.Category;

import java.util.function.Consumer;

public class ChannelCategoryObject extends DiscordObject<Category> {

    private String name;

    public ChannelCategoryObject(DiscordModule bot, String name) {
        super(bot);
        this.name = name;
    }

    @Override
    public void delete() {
        get().ifPresent(cat -> cat.delete().queue());
        set(null);
        ObjectRegistry.INSTANCE.unregister(this);
    }

    @Override
    public void create(Consumer<Category> callback) {
        this.bot.getGuild().getController().createCategory(name).queue(cat -> {
            if (callback != null) {
                callback.accept((Category) cat);
            }
        });
    }
}
