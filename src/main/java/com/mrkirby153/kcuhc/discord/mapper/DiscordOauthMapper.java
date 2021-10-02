package com.mrkirby153.kcuhc.discord.mapper;

import com.mrkirby153.kcuhc.discord.oauth.DiscordOAuthModule;
import com.mrkirby153.kcuhc.discord.oauth.dto.SavedOAuthUser;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordOauthMapper implements PlayerMapper {

    private final DiscordOAuthModule module;
    private final JDA jda;

    public DiscordOauthMapper(DiscordOAuthModule module, JDA jda) {
        this.module = module;
        this.jda = jda;
    }

    @Override
    public User getUser(UUID uuid) {
        SavedOAuthUser discordUser = module.getDiscordUser(uuid);
        if (discordUser == null || jda == null) {
            return null;
        }
        User userById = jda.getUserById(discordUser.getId());
        return userById;
    }

    @Override
    public void createLink(Player player) {
        BaseComponent component = Chat.message("Discord", "Click ");

        BaseComponent suggest = Chat.formattedChat("[HERE]", ChatColor.GOLD);
        suggest.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text("Open the link website")));
        String linkSite = String.format("%s/%s", module.getOauthDashboardUrl(),
            player.getUniqueId());
        suggest.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, linkSite));
        component.addExtra(suggest);
        BaseComponent finalComponent = Chat.formattedChat(
            " to link your Discord and Minecraft Accounts", Chat.INSTANCE.getTEXT_COLOR());
        component.addExtra(finalComponent);
        player.spigot().sendMessage(component);
    }

    @Override
    public void forceLink(Player player, String id) {
        // No op
    }

    @Override
    public String getCode(UUID uuid) {
        return "<NO OP>";
    }

    @Override
    public void drawUnlinkedScoreboard(Player player, UHCScoreboard scoreboard) {
        scoreboard.add(new ElementHeadedText(ChatColor.RED + "Link your discord account with",
            "/discord link"));
    }
}
