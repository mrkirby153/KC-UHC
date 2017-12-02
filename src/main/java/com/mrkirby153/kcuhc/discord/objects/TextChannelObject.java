package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.function.Consumer;

public class TextChannelObject extends DiscordObject<TextChannel> {

    private String name;

    public TextChannelObject(DiscordModule module, String name) {
        super(module);
        this.name = name.replace(' ', '-');
    }

    @Override
    public void create(Consumer<TextChannel> callback) {
        this.bot.getGuild().getController().createTextChannel(this.name).queue(chan -> {
            set((TextChannel) chan);
            ObjectRegistry.INSTANCE.registerForDelete(this);
            if (callback != null) {
                callback.accept((TextChannel) chan);
            }
        });
    }

    @Override
    public void delete() {
        get().ifPresent(chan -> chan.delete().queue());
        set(null);
        ObjectRegistry.INSTANCE.unregister(this);
    }
}
