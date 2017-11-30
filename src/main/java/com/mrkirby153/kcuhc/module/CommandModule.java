package com.mrkirby153.kcuhc.module;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.gui.ModuleGui;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("module")
public class CommandModule extends BaseCommand {

    private UHC uhc;
    private ModuleGui gui;

    @Inject
    public CommandModule(UHC uhc) {
        this.uhc = uhc;
        this.gui = new ModuleGui(uhc);
    }

    @Subcommand("load")
    @CommandCompletion("@unloadedModules")
    public void loadModule(CommandSender sender, UHCModule module) {
        ModuleRegistry.INSTANCE.load(module);
        sender.sendMessage(
            Chat.INSTANCE.message("Module", "Module {module} loaded!", "{module}", module.getName())
                .toLegacyText());
    }

    @Default
    public void showGui(Player player) {
        gui.open(player);
    }

    @Subcommand("unload")
    @CommandCompletion("@loadedModules")
    public void unloadModule(CommandSender sender, UHCModule module) {
        ModuleRegistry.INSTANCE.unload(module);
        sender.sendMessage(Chat.INSTANCE
            .message("Module", "Module {module} unloaded!", "{module}", module.getName())
            .toLegacyText());
    }
}
