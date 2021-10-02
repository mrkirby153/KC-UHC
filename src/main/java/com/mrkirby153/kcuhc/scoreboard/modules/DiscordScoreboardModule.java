package com.mrkirby153.kcuhc.scoreboard.modules;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DiscordScoreboardModule implements ScoreboardModule {

    private final DiscordModule module;
    private final UHCGame game;

    public DiscordScoreboardModule(UHCGame game, DiscordModule module) {
        this.module = module;
        this.game = game;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        User u = module.playerMapper.getUser(player.getUniqueId());
        if (u != null) {
            scoreboard.add(new ElementHeadedText(ChatColor.GREEN + "Linked to",
                ChatColor.GOLD + u.getName() + "#" + u.getDiscriminator()));
        } else {
            module.playerMapper.drawUnlinkedScoreboard(player, scoreboard);
        }
    }

    @Override
    public boolean shouldDisplay() {
        return game.getCurrentState() == GameState.WAITING;
    }
}
