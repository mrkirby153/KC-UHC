package com.mrkirby153.kcuhc.discord.mapper;

import com.mrkirby153.botcore.command.Command;
import com.mrkirby153.botcore.command.Context;
import com.mrkirby153.botcore.command.args.CommandContext;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.DiscordModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.utils.IdGenerator;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class UHCBotLinkMapper implements PlayerMapper {

    private static final IdGenerator ID_GENERATOR = new IdGenerator(
        IdGenerator.Companion.getALPHA() + IdGenerator.Companion.getNUMBERS());

    private static final String CROSS_MARK = "\u274C";
    private static final String CHECK_MARK = "\u2705";
    private final UHC uhc;
    private final File dataFile;
    private DiscordModule discordModule;
    /**
     * Links a player's UUID to their Discord ID
     */
    private HashMap<UUID, String> uuidToDiscordMap = new HashMap<>();
    /**
     * Correlate a link code with a UUID
     */
    private HashMap<String, UUID> linkCodeMap = new HashMap<>();
    private FileConfiguration savedLinkages = new YamlConfiguration();

    public UHCBotLinkMapper(UHC uhc, DiscordModule discordModule) {
        this.discordModule = discordModule;
        this.uhc = uhc;
        dataFile = new File(uhc.getDataFolder(), "links.yml");
        loadLinks();
    }

    @Override
    public User getUser(UUID uuid) {
        String id = this.uuidToDiscordMap.get(uuid);
        if (id == null) {
            return null;
        }
        Member member = this.discordModule.guild.getMemberById(id);
        if (member == null) {
            return null;
        }
        return member.getUser();
    }

    @Override
    public void createLink(Player player) {
        String code = null;
        if (this.linkCodeMap.containsValue(player.getUniqueId())) {
            for (Map.Entry<String, UUID> entry : this.linkCodeMap.entrySet()) {
                if (entry.getValue().equals(player.getUniqueId())) {
                    code = entry.getKey();
                    break;
                }
            }
        } else {
            do {
                code = ID_GENERATOR.generate(5);
            } while (this.linkCodeMap.containsKey(code));
            this.linkCodeMap.put(code, player.getUniqueId());
        }

        String command = String.format("!uhcbot link %s", code);
        BaseComponent component = Chat.message("Discord",
            "To link your minecraft account to discord, run this command on the discord {discord}: {command} ",
            "{discord}", this.discordModule.guild.getName(), "{command}", command);

        BaseComponent suggest = Chat.formattedChat("[COPY]", ChatColor.AQUA);
        suggest.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new BaseComponent[]{
            Chat.formattedChat(
                "Click to copy the command to your chat box for easy copying", ChatColor.WHITE)
        }));
        suggest.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        component.addExtra(suggest);
        player.spigot().sendMessage(component);
        discordModule
            .log(":pencil2:", "Created link code `" + code + "` for `" + player.getName() + "`");
    }

    @Override
    public void forceLink(Player player, String id) {
        Member member = this.discordModule.guild.getMemberById(id);
        if (member == null) {
            return;
        }
        User user = member.getUser();
        commitLink(user, player.getUniqueId());
        player.spigot()
            .sendMessage(Chat.message("Discord", "Your account has been linked to {user}",
                "{user}", user.getName() + "#" + user.getDiscriminator()));
    }

    private void accept(Message message) {
        message.addReaction(CHECK_MARK).queue();
    }

    private void reject(Message message) {
        message.addReaction(CROSS_MARK).queue();
    }

    @Command(name = "link", arguments = {"<code:string>"}, parent = "uhcbot")
    public void link(Context context, CommandContext commandContext) {
        String code = commandContext.get("code");

        UUID uuid = this.linkCodeMap.get(code);
        if (uuid == null) {
            reject(context);
            return;
        }
        commitLink(context.getAuthor(), uuid);
        this.uuidToDiscordMap.put(uuid, context.getAuthor().getId());
        this.accept(context);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1F, 1F);
            p.spigot().sendMessage(Chat.message("Discord",
                "Your minecraft account was linked to the discord account {user}", "{user}",
                context.getAuthor().getName() + "#" + context.getAuthor().getDiscriminator()));
            discordModule
                .log(":chains:",
                    "Linking `" + context.getAuthor().getName() + "#" + context.getAuthor()
                        .getDiscriminator() + "` to minecraft account `" + p.getName() + "`");
        }
    }

    public void commitLink(User user, UUID uuid) {
        this.uuidToDiscordMap.put(uuid, user.getId());
        savedLinkages.set(uuid.toString(), user.getId());
        saveLinks();
    }

    @Override
    public String getCode(UUID uuid) {
        for (Map.Entry<String, UUID> e : linkCodeMap.entrySet()) {
            if (e.getValue().equals(uuid)) {
                return e.getKey();
            }
        }
        return null;
    }

    private void saveLinks() {
        try {
            savedLinkages.save(dataFile);
        } catch (IOException e) {
            uhc.getLogger().log(Level.SEVERE, "Could not load link file", e);
        }
    }

    public void loadLinks() {
        try {
            if(!dataFile.exists()) {
                dataFile.createNewFile();
            }
            savedLinkages.load(dataFile);
            uuidToDiscordMap.clear();
            savedLinkages.getKeys(false).forEach(key -> {
                UUID u = UUID.fromString(key);
                uuidToDiscordMap.put(u, savedLinkages.getString(key));
            });
        } catch (IOException | InvalidConfigurationException e) {
            uhc.getLogger().log(Level.SEVERE, "Could not load link file", e);
        }
    }
}
