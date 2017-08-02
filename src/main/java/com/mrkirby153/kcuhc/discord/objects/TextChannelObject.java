package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordRobot;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.function.Consumer;

public class TextChannelObject extends DiscordObject<TextChannel> {

    private String name;

    public TextChannelObject(DiscordRobot robot, String name) {
        super(robot);
        this.name = name;
    }


    @Override
    public void create(Consumer<TextChannel> callback) {
        robot.getGuild().getController().createTextChannel(name.replace(' ', '-'))
                .queue(chan -> {
                    this.object = (TextChannel) chan;
                    if (callback != null)
                        callback.accept((TextChannel) chan);
                });
    }

    @Override
    public void delete() {
        if (this.object != null)
            this.object.delete().queue();
    }
}
