package com.mrkirby153.kcuhc.module;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import me.mrkirby153.kcutils.C;
import org.bukkit.command.CommandSender;

@CommandAlias("module")
public class CommandModule extends BaseCommand {

    @Subcommand("load")
    @CommandCompletion("@unloadedModules")
    public void loadModule(CommandSender sender, UHCModule module){
        ModuleRegistry.INSTANCE.load(module);
        sender.sendMessage(C.m("Module", "Module {module} loaded!", "{module}", module.getName()).toLegacyText());
    }

    @Subcommand("unload")
    @CommandCompletion("@loadedModules")
    public void unloadModule(CommandSender sender, UHCModule module){
        ModuleRegistry.INSTANCE.unload(module);
        sender.sendMessage(C.m("Module", "Module {module} unloaded!", "{module}", module.getName()).toLegacyText());
    }
}
