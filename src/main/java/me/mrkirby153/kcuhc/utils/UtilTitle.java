package me.mrkirby153.kcuhc.utils;


import me.mrkirby153.kcutils.nms.NMS;
import org.bukkit.entity.Player;

public class UtilTitle {

    private static NMS nms;

    public static void setNms(NMS nms){
        UtilTitle.nms = nms;
    }

    public static void title(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if(title == null)
            title = "";
        if(subtitle == null)
            subtitle = "";
        nms.title(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
    }

    public static void title(Player player, String title, String subtitle) {
        if(title == null)
            title = "";
        if(subtitle == null)
            subtitle = "";
        nms.title(player, title, subtitle);
    }

}
