package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordRobot;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.function.Consumer;

public class VoiceChannelObject extends DiscordObject<VoiceChannel> {

    private String name;

    public VoiceChannelObject(DiscordRobot robot, String name) {
        super(robot);
        this.name = name;
    }

    @Override
    public void create(Consumer<VoiceChannel> callback) {
        robot.getGuild().getController().createVoiceChannel(name).queue(channel -> {
            this.object = (VoiceChannel) channel;
            if (callback != null)
                callback.accept((VoiceChannel) channel);
        });
    }

    @Override
    public void delete() {
        if (this.object != null)
            this.object.delete().queue();
    }
}
