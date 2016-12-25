package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.player.LoneWolfModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoneWolfCommand extends BaseCommand<UHC> {

    private LoneWolfModule loneWolfModule;

    public LoneWolfCommand(LoneWolfModule handler, UHC uhc) {
        super(uhc, "lonewolf", new String[]{
                "lw"
        });
        loneWolfModule = handler;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!ModuleRegistry.isLoaded(LoneWolfModule.class)) {
            player.spigot().sendMessage(C.e("Lone wolves aren't enabled!"));
            return;
        }
        if (args.length == 0) {
            loneWolfModule.addLoneWolf(player);
            return;
        }
        if (args.length == 1) {
            Player p = Bukkit.getPlayer(args[0]);
            if (p == null)
                return;
            loneWolfModule.addLoneWolf(p);
            player.spigot().sendMessage(C.m(loneWolfModule.getName(), "Assigned " + p.getName() + " to a lone wolf"));
        }
    }
}
