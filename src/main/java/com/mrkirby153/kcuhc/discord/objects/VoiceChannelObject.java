package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.function.Consumer;

public class VoiceChannelObject extends DiscordObject<VoiceChannel> {

    private String name;

    public VoiceChannelObject(DiscordModule bot, String name) {
        super(bot);
        this.name = name;
    }

    @Override
    public void create(Consumer<VoiceChannel> callback) {
        this.bot.getGuild().getController().createVoiceChannel(this.name).queue(chan -> {
            set((VoiceChannel) chan);
            ObjectRegistry.INSTANCE.registerForDelete(this);
            if (callback != null) {
                callback.accept((VoiceChannel) chan);
            }
        });
    }

    @Override
    public void delete() {
        get().ifPresent(chan -> chan.delete().queue());
        set(null);
    }
}
