package com.mrkirby153.kcuhc.module.respawner;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import com.mrkirby153.kcuhc.module.respawner.TeamRespawnStructure.Phase;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("respawner")
public class RespawnerCommand extends BaseCommand {

    private TeamRespawnModule module;

    public RespawnerCommand(TeamRespawnModule module) {
        this.module = module;
    }


    @Subcommand("init")
    public void initializeStructure(Player sender) {
        Location l = sender.getLocation().subtract(0, 1, 0);
        this.module.structure = new TeamRespawnStructure(l);
        this.module.structure.buildStructure();
        sender.sendMessage(Chat.message("Respawner", "Structure initialized").toLegacyText());
    }

    @Subcommand("activate-beacon")
    public void activateBeacon(CommandSender sender) {
        this.module.structure.activateBeacon();
        sender.sendMessage(Chat.message("Respawner", "Beacon activated").toLegacyText());
    }

    @Subcommand("deactivate-beacon")
    public void deactivateBeacon(CommandSender sender) {
        this.module.structure.deactivateBeacon();
        sender.sendMessage(Chat.message("Respawner", "Beacon deactivated").toLegacyText());
    }

    @Subcommand("respawn-test")
    public void respawnTest(CommandSender sender) {
        TeamRespawnStructure trs = this.module.structure;
        trs.startRespawn();
        sender.sendMessage(Chat.message("Respawner", "Respawning team").toLegacyText());
    }

    @Subcommand("phase")
    public void respawnerPhase(CommandSender sender, Phase phase) {
        this.module.structure.setPhase(phase);
        sender.sendMessage(Chat.message("Respawner", "Set state to {state}", "{state}", phase).toLegacyText());
    }
}
