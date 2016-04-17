package me.mrkirby153.kcuhc.noteBlock;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a {@link NoteBlockSong} ends
 * @author mrkirby153
 */
public class SongEndedEvent extends Event{
    private static final HandlerList handlers = new HandlerList();
    private NoteBlockSong noteBlockFileName;

    public SongEndedEvent(NoteBlockSong noteBlockFileName) {
        this.noteBlockFileName = noteBlockFileName;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public NoteBlockSong getName(){
        return noteBlockFileName;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
