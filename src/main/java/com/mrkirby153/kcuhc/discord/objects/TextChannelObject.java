package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Optional;
import java.util.function.Consumer;

public class TextChannelObject implements DiscordObject<TextChannel> {

    private String name;
    private DiscordModule module;
    private Category parent;

    private TextChannel channel;

    public TextChannelObject(String name, DiscordModule module, Category parent) {
        this.name = name.replaceAll("\\s", "-");
        this.module = module;
        this.parent = parent;
    }

    @Override
    public void create(Consumer<TextChannel> consumer) {
        module.guild.createTextChannel(this.name).setParent(parent).queue(c -> {
            this.channel = c;
            ObjectRegistry.INSTANCE.register(this);
            if (consumer != null) {
                consumer.accept(c);
            }
        });
    }

    @Override
    public Optional<TextChannel> get() {
        return Optional.ofNullable(channel);
    }

    @Override
    public void delete() {
        get().ifPresent(c -> c.delete().queue());
        this.channel = null;
        ObjectRegistry.INSTANCE.unregister(this);
    }
}
