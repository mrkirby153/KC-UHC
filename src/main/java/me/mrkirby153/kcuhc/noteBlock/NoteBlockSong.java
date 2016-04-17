package me.mrkirby153.kcuhc.noteBlock;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class handling loading and playback of a NoteBlockStudioParser song
 *
 * @author mrkirby153
 */
public class NoteBlockSong {

    public static boolean debug = false;

    private HashMap<Integer, ArrayList<NoteBlock>> notes = new HashMap<>();
    private final File location;
    private double tempo;
    private int currentTick = 0;
    private Thread playerThread;
    private boolean isPlaying = true;
    private boolean isDestroyed = false;
    private ArrayList<String> playersListening = new ArrayList<>();
    private String songName;

    public NoteBlockSong(File songLocation) {
        this.location = songLocation;
    }

    /**
     * Starts playback of the song
     */
    public void play() {
        loadSong();
        createThread();
    }

    /**
     * "Tunes" the player to the song. Following notes will be played to the player
     *
     * @param player The player's name
     */
    public void addPlayer(String player) {
        if (playersListening.contains(player))
            return;
        playersListening.add(player);
    }

    /**
     * Untunes a player to the song
     *
     * @param player The player
     */
    public void removePlayer(Player player) {
        playersListening.remove(player.getName());
    }

    /**
     * "Tunes" the player to the song. Following notes will be played to the player
     *
     * @param player The player object
     */
    public void addPlayer(Player player) {
        addPlayer(player.getName());
    }

    /**
     * Temporarily pauses the playback of the song
     */
    public void pause() {
        isPlaying = false;
    }

    /**
     * Resumes playback of the song
     */
    public void resume() {
        isPlaying = true;
    }

    /**
     * Stops the playback of the song and destroys it
     */
    public void stop(boolean broadcastEvent) {
        isPlaying = false;
        currentTick = 0;
        if (broadcastEvent)
            Bukkit.getPluginManager().callEvent(new SongEndedEvent(NoteBlockSong.this));
        destroy();
    }

    /**
     * Creates the thread for playback
     */
    private void createThread() {
        this.playerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isDestroyed) {
                    long startTime = System.currentTimeMillis();
                    synchronized (NoteBlockSong.this) {
                        if (isPlaying) {
                            if (currentTick > notes.size()) {
                                isPlaying = false;
                                currentTick = 0;
                                Bukkit.getPluginManager().callEvent(new SongEndedEvent(NoteBlockSong.this));
                                destroy();
                                return;
                            }
                            int tick = currentTick++;
                            for (String s : playersListening) {
                                Player p = Bukkit.getPlayerExact(s);
                                playTick(p, tick);
                            }
                        }
                        long duration = System.currentTimeMillis() - startTime;
                        int delayMillis = getDelay() * 50;
                        if (duration < delayMillis) {
                            try {
                                Thread.sleep(delayMillis - duration);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        this.playerThread.setName("JukeboxThread");
        this.playerThread.setPriority(10);
        this.playerThread.start();
    }

    /**
     * Destroy the playback thread
     */
    private void destroy() {
        isDestroyed = true;
    }


    /**
     * Gets the delay for scheduling song playback
     *
     * @return The delay
     */
    private short getDelay() {
        return ((short) (20 / tempo));
    }

    /**
     * Parses the NoteBlockStudioParser file and loads them into {@link NoteBlockSong#notes} list
     */
    private void loadSong() {
        try {
            byte[] bytes = Files.readAllBytes(location.toPath());
            ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
            this.songName = input.readUTF();
            int totalChords = input.readInt();
            this.tempo = input.readDouble();
            for (int i = 0; i < totalChords; i++) {
                int tick = input.readInt();
                int notesInChord = input.readInt();
                ArrayList<NoteBlock> chord = new ArrayList<>();
                for (int j = 0; j < notesInChord; j++) {
                    byte instrument = input.readByte();
                    int pitch = input.readInt();
                    NoteBlock nb = new NoteBlock(tick, instrument, pitch);
                    chord.add(nb);
                }
                notes.put(tick, chord);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the song name (Loaded from the file)
     *
     * @return The song name
     */
    public String getSongName() {
        return this.songName;
    }

    /**
     * Plays the corresponding tick to the player
     *
     * @param player The player to play the chord for
     * @param tick   The tick in the song to play
     */
    private void playTick(Player player, int tick) {
        ArrayList<NoteBlock> chord = this.notes.get(tick);
        if (debug)
            System.out.println("TICK: " + tick);
        if (chord != null) {
            for (NoteBlock nb : chord) {
                Sound sound = null;
                switch (nb.getInstrument()) {
                    case PIANO:
                        sound = Sound.BLOCK_NOTE_HARP;
                        break;
                    case DOUBLE_BASS:
                        sound = Sound.BLOCK_NOTE_BASS;
                        break;
                    case BASS_DRUM:
                        sound = Sound.BLOCK_NOTE_BASEDRUM;
                        break;
                    case SNARE_DRUM:
                        sound = Sound.BLOCK_NOTE_SNARE;
                        break;
                    case CLICK:
                        sound = Sound.BLOCK_NOTE_HAT;
                        break;
                }
                if (player == null)
                    return;
                if (debug)
                    System.out.println("\tNOTE: " + nb.toString());
                player.playSound(player.getLocation(), sound, 0.75f, (float) nb.getPitch().getNoteblockPitch());
            }
        }
    }
}
