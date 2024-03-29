package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.Optional;
import java.util.function.Consumer;

public class VoiceChannelObject implements DiscordObject<VoiceChannel> {

    private String name;
    private VoiceChannel channel;
    private DiscordModule discordModule;
    private Category parent;

    public VoiceChannelObject(String name, DiscordModule discordModule, Category parent) {
        this.name = name;
        this.discordModule = discordModule;
        this.parent = parent;
    }

    @Override
    public void create(Consumer<VoiceChannel> consumer) {
        this.discordModule.guild.createVoiceChannel(this.name).setParent(this.parent)
            .queue(chan -> {
                this.channel = chan;
                ObjectRegistry.INSTANCE.register(this);
                if (consumer != null) {
                    consumer.accept(chan);
                }
            });
    }

    @Override
    public Optional<VoiceChannel> get() {
        return Optional.ofNullable(this.channel);
    }

    @Override
    public void delete() {
        get().ifPresent(c -> c.delete().queue());
        this.channel = null;
        ObjectRegistry.INSTANCE.unregister(this);
    }
}
