package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.core.entities.Category;

import java.util.Optional;
import java.util.function.Consumer;

public class ChannelCategoryObject implements DiscordObject<Category> {

    private DiscordModule module;

    private String name;

    private Category category;

    public ChannelCategoryObject(DiscordModule module, String name) {
        this.module = module;
        this.name = name;
    }

    @Override
    public void create(Consumer<Category> consumer) {
        this.module.guild.getController().createCategory(name).queue(cat -> {
            ObjectRegistry.INSTANCE.register(this);
            this.category = (Category) cat;
            if (consumer != null) {
                consumer.accept((Category) cat);
            }
        });
    }

    @Override
    public Optional<Category> get() {
        return Optional.ofNullable(category);
    }

    @Override
    public void delete() {
        get().ifPresent(c -> c.delete().queue());
        this.category = null;
        ObjectRegistry.INSTANCE.unregister(this);
    }
}
