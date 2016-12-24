package me.mrkirby153.kcuhc.arena.handler.lonewolf;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoneWolfCommand extends BaseCommand<UHC>{

    private LoneWolfHandler loneWolfHandler;

    public LoneWolfCommand(LoneWolfHandler handler, UHC uhc){
        super(uhc, "lonewolf", new String[] {
                "lw"
        });
        loneWolfHandler = handler;
    }
    @Override
    public void execute(Player player, String[] args) {
        if(args.length == 0){
            loneWolfHandler.addLoneWolf(player);
            return;
        }
        if(args.length == 1){
            Player p = Bukkit.getPlayer(args[0]);
            if(p == null)
                return;
            loneWolfHandler.addLoneWolf(p);
            player.spigot().sendMessage(C.m(loneWolfHandler.getName(), "Assigned "+p.getName()+" to a lone wolf"));
        }
    }
}
