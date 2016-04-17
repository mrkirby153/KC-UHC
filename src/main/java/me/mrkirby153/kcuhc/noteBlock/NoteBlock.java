package me.mrkirby153.kcuhc.noteBlock;

/**
 * Class to handle deconstructing of noteblocks from binary into a playable form
 * @author mrkirby153
 */
public class NoteBlock {

    private Instrument instrument;
    private Pitch pitch;
    private int tick;

    public NoteBlock(int tick, byte inst, int key) {
        this.tick = tick;
        this.instrument = Instrument.getInstrument(inst);
        this.pitch = Pitch.findPitch((byte)key);
    }

    /**
     * Gets the {@link Instrument} that the {@link NoteBlock} represents
     * @return The {@link Instrument} that the {@link NoteBlock} represents
     */
    public Instrument getInstrument() {
        return instrument;
    }

    /**
     * Gets the pitch that the {@link NoteBlock} represents
     * @return the {@link Pitch}
     */
    public Pitch getPitch() {
        return pitch;
    }

    /**
     * Gets the tick that the {@link NoteBlock} should be played on
     * @return The tick that the noteblock should be played on
     */
    public int getTick() {
        return tick;
    }

    /**
     * Custom toString() override containing the instrument, pitch, and the tick the
     * @return A custom string in the following format: {@link Instrument} string, {@link Pitch} string and the tick
     */
    @Override
    public String toString() {
        return instrument.toString() + ", " + pitch.toString() + ", " + tick;
    }

    /**
     * Enum for turning binary data (0-4) into a more user-friendly representation
     */
    enum Instrument {
        INVALID(-1),
        PIANO(0),
        DOUBLE_BASS(1),
        BASS_DRUM(2),
        SNARE_DRUM(3),
        CLICK(4);

        private final int inst;

        Instrument(int inst) {
            this.inst = inst;
        }

        /**
         * Returns the raw byte that the instrument represents
         * @return The instrument's byte (0-4)
         */
        public int getInst() {
            return inst;
        }

        /**
         * Converts a byte of an instrument into its more friendly notation
         * @param inst The instrument (0-4)
         * @return The {@link Instrument} corresponding to the byte
         */
        public static Instrument getInstrument(byte inst) {
            switch (inst) {
                case 0:
                    return PIANO;
                case 1:
                    return DOUBLE_BASS;
                case 2:
                    return BASS_DRUM;
                case 3:
                    return SNARE_DRUM;
                case 4:
                    return CLICK;
            }
            return INVALID;
        }
    }

    /**
     * Enum handling a note's binary representation as well as its playSound() pitch
     */
    public enum Pitch {
        ERR(-1, -1),
        F_SHARP_3(33, 0.5F),
        G_3(34, .53F),
        G_SHARP_3(35, 0.56F),
        A_3(36, 0.600),
        A_SHARP_3(37, 0.630),
        B_3(38, 0.670),
        C_4(39, 0.700),
        C_SHARP_4(40, 0.760),
        D_4(41, 0.800),
        D_SHARP_4(42, 0.84),
        E_4(43, 0.900),
        F_4(44, 0.940),
        F_SHARP_4(45, 1.000),
        G_4(46, 1.060),
        G_SHARP_4(47, 1.120),
        A_4(48, 1.180),
        A_SHARP_4(49, 1.260),
        B_4(50, 1.340),
        C_5(51, 1.42),
        C_SHARP_5(52, 1.500),
        D_5(53, 1.600),
        D_SHARP_5(54, 1.680),
        E_5(55, 1.780),
        F_5(56, 1.88),
        F_SHARP_5(57, 2.000);
        private final int pitch;
        private final double noteblockPitch;

        Pitch(int pitch, double noteblockPitch) {
            this.pitch = pitch;
            this.noteblockPitch = noteblockPitch;
        }

        /**
         * Gets the raw pitch (33-57) that the {@link Pitch} represents
         * @return Raw pitch data
         */
        public int getPitch() {
            return pitch;
        }

        /**
         * Gets the noteblock pitch for using in playSound()
         * @return The current noteblock pitch
         */
        public double getNoteblockPitch(){
            return this.noteblockPitch;
        }

        /**
         * Modified toString() to include the noteblock's pitch
         * @return A modified version of the {@link Enum Enum's} toString(), appending the noteblock pitch
         */
        @Override
        public String toString() {
            String s = super.toString();
            return s + ":" + noteblockPitch;
        }

        /**
         * Converts the raw pitch byte into a {@link Pitch}
         * @param pitch The pitch byte (33-57)
         * @return A {@link Pitch}
         */
        public static Pitch findPitch(byte pitch) {
            int p = (int) pitch;
            Pitch[] pitches = Pitch.class.getEnumConstants();
            for (Pitch pi : pitches) {
                if (pi.getPitch() == p) {
                    return pi;
                }
            }
            return ERR;
        }

        /**
         * Converts the noteblock pitch into a {@link Pitch}
         * @param pitch The noteblock pitch
         * @return A {@link Pitch}
         */
        public static Pitch findPitch(double pitch){
            Pitch[] pitches = Pitch.class.getEnumConstants();
            for(Pitch pi : pitches){
                if(pi.getNoteblockPitch() == pitch)
                    return pi;
            }
            return ERR;
        }

        /**
         * @deprecated Redundant and will be removed
         */
        @Deprecated
        public static float getPitch(int note){
            for(Pitch pitch : values()){
                if(pitch.pitch == note)
                    return (float) pitch.noteblockPitch;
            }
            return 0.0F;
        }
    }
}
