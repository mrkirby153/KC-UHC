package com.mrkirby153.kcuhc.module;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
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

    @Inject
    public CommandModule(UHC uhc) {
        this.uhc = uhc;
    }

    @Subcommand("load")
    @CommandCompletion("@unloadedModules")
    @CommandPermission("kcuhc.module.load")
    public void loadModule(CommandSender sender, UHCModule module) {
        ModuleRegistry.INSTANCE.load(module);
        sender.sendMessage(
            Chat.message("Module", "Module {module} loaded!", "{module}", module.getName())
                .toLegacyText());
    }

    @Default
    @CommandPermission("kcuhc.module")
    public void showGui(Player player) {
//        gui.open(player);
        ModuleRegistry.INSTANCE.openGui(player);
    }

    @Subcommand("unload")
    @CommandCompletion("@loadedModules")
    @CommandPermission("kcuhc.module.unload")
    public void unloadModule(CommandSender sender, UHCModule module) {
        ModuleRegistry.INSTANCE.unload(module);
        sender.sendMessage(Chat
            .message("Module", "Module {module} unloaded!", "{module}", module.getName())
            .toLegacyText());
    }

    @Subcommand("options|settings")
    @CommandCompletion("@loadedModules @moduleSettings")
    @CommandPermission("kcuhc.module.options")
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
            moduleSetting.reset();
            module.onSettingChange(moduleSetting);
            sender.sendMessage(Chat.message("Module", "Reset {key}", "{key}", setting).toLegacyText());
            return;
        }
        try {
            moduleSetting.set(value);
            module.onSettingChange(moduleSetting);
        } catch (SettingParseException e) {
            sender.sendMessage(Chat.error(e.getMessage()).toLegacyText());
            return;
        }
        sender.sendMessage(
            Chat.message("Module", "{key} set to {value}", "{key}", setting, "{value}",
                moduleSetting.toString()).toLegacyText());
    }
}
