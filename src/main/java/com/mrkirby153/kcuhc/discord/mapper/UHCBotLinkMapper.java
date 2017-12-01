package com.mrkirby153.kcuhc.discord.mapper;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import me.mrkirby153.kcutils.Chat;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UHCBotLinkMapper extends ListenerAdapter implements PlayerMapper {

    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String CROSS_MARK = "\u274C";
    private static final String CHECK_MARK = "\u2705";

    private DiscordModule discordModule;

    /**
     * Links the player's UUID to to their Discord ID
     */
    private HashMap<UUID, String> uuidToDiscordMap = new HashMap<>();

    /**
     * Associates a link code with a UUID
     */
    private HashMap<String, UUID> linkCodeToUuidMap = new HashMap<>();

    private Random random = new Random();

    public UHCBotLinkMapper(DiscordModule discordModule) {
        this.discordModule = discordModule;
        this.discordModule.getJda().addEventListener(this);
    }


    @Override
    public User getUser(UUID uuid) {
        if (!this.uuidToDiscordMap.containsKey(uuid)) {
            return null;
        }
        return this.discordModule.getJda().getUserById(this.uuidToDiscordMap.get(uuid));
    }

    @Override
    public void createLink(Player player) {
        String linkCode = "";
        if (this.linkCodeToUuidMap.containsValue(player.getUniqueId())) {
            // Return their old link code
            for (Map.Entry<String, UUID> entry : this.linkCodeToUuidMap.entrySet()) {
                if (entry.getValue() == player.getUniqueId()) {
                    linkCode = entry.getKey();
                    break;
                }
            }
        } else {
            // Generate a new one
            do {
                linkCode = generateLinkCode();
            } while (this.linkCodeToUuidMap.containsKey(linkCode));
        }
        this.linkCodeToUuidMap.put(linkCode, player.getUniqueId());

        String command = String.format("!uhcbot link %s", linkCode);
        BaseComponent component = Chat.INSTANCE.message("Discord",
            "To link your minecraft account to discord, run this command on the discord server: {command} ",
            "{command}", command);

        BaseComponent suggest = Chat.INSTANCE.formattedChat("[COPY]", ChatColor.AQUA);
        suggest.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
            Chat.INSTANCE.formattedChat("Click to fill your chat message for easy copying",
                ChatColor.WHITE)}));
        suggest.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            command));
        component.addExtra(suggest);

        player.spigot().sendMessage(component);
    }

    private String generateLinkCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            code.append(ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length())));
        }
        return code.toString();
    }

    private void rejectMessage(Message message) {
        message.addReaction(CROSS_MARK).queue(m -> {
            message.delete().queueAfter(30, TimeUnit.SECONDS);
        });
    }

    private void acceptMessage(Message message) {
        message.addReaction(CHECK_MARK).queue(m -> {
            message.delete().queueAfter(30, TimeUnit.SECONDS);
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!this.discordModule.isReady()) {
            return;
        }
        if (event.getGuild().getIdLong() != this.discordModule.getGuild().getIdLong()) {
            return;
        }
        String message = event.getMessage().getContent();

        if (!message.toLowerCase().startsWith("!uhcbot")) {
            return;
        }

        String[] split = event.getMessage().getContent().split(" ");

        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("link")) {
                if (args.length < 2) {
                    this.rejectMessage(event.getMessage());
                    return;
                }
                String code = args[1];
                UUID u = this.linkCodeToUuidMap.remove(code);
                if (u == null) {
                    this.rejectMessage(event.getMessage());
                    return;
                }

                this.uuidToDiscordMap.put(u, event.getAuthor().getId());
                this.acceptMessage(event.getMessage());
            }
        }
    }
}
