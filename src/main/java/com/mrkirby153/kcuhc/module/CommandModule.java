package com.mrkirby153.kcuhc.module;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.gui.ModuleGui;
import com.mrkirby153.kcuhc.module.settings.ModuleSetting;
import com.mrkirby153.kcuhc.module.settings.SettingParseException;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map.Entry;
import java.util.Optional;

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
            Chat.message("Module", "Module {module} loaded!", "{module}", module.getName())
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
        sender.sendMessage(Chat
            .message("Module", "Module {module} unloaded!", "{module}", module.getName())
            .toLegacyText());
    }

    @Subcommand("options|settings")
    @CommandCompletion("@loadedModules @moduleSettings")
    public void options(CommandSender sender, UHCModule module, @Default String setting,
        @Default String value) {
        if (setting.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "The following settings are available: ");
            module.getSettings().forEach((name, s) -> sender
                .sendMessage(ChatColor.GREEN + "   - " + name + " = " + s.toString()));
            return;
        }
        Optional<Entry<String, ModuleSetting>> optSetting = module.getSettings().entrySet().stream()
            .filter(s -> s.getKey().equalsIgnoreCase(setting)).findFirst();

        if (!optSetting.isPresent()) {
            sender.sendMessage(Chat.error("That setting does not exist").toLegacyText());
            return;
        }
        ModuleSetting moduleSetting = optSetting.get().getValue();

        if (value.isEmpty()) {
            sender.sendMessage(Chat
                .message("Module", "{key} = {value}", "{key}", setting, "{value}",
                    moduleSetting.toString()).toLegacyText());
            return;
        }
        try {
            moduleSetting.set(value);
        } catch (SettingParseException e) {
            sender.sendMessage(Chat.error(e.getMessage()).toLegacyText());
            return;
        }
        sender.sendMessage(
            Chat.message("Module", "{key} set to {value}", "{key}", setting, "{value}",
                moduleSetting.toString()).toLegacyText());
    }
}
