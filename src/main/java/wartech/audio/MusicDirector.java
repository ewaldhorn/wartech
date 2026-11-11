package wartech.audio;

import wartech.util.Rng;

public final class MusicDirector {
    public static final double BPM = 88.0;
    public static final double STEP_SECONDS = 60.0 / BPM / 4.0;
    public static final int STEPS_PER_BAR = 16;
    public static final int STEPS_PER_CHORD = 32;
    public static final double LOOKAHEAD = 0.35;

    private static final int ROOT_MIDI = 38;

    private static final int[][] PROGRESSIONS = {
            { 0, -4, 3, -2 },
            { 0, -2, -4, -2 },
            { 0, 3, -2, -4 },
            { 0, 5, -4, -2 },
    };
    private static final boolean[][] MINOR = {
            { true, false, false, false },
            { true, false, false, false },
            { true, false, false, false },
            { true, true, false, false },
    };

    private static final int[][][] MOTIFS = {
            { { 0, 0, 3 }, { 7, 4, 2 }, { 5, 6, 2 }, { 7, 8, 6 } },
            { { 7, 0, 2 }, { 7, 3, 1 }, { 12, 4, 4 }, { 10, 10, 4 } },
            { { 0, 0, 2 }, { 3, 2, 2 }, { 7, 4, 3 }, { 5, 8, 2 }, { 0, 10, 6 } },
    };

    private final AudioBackend backend;
    private final Rng rng;

    private boolean started = false;
    private double nextStepTime = 0.0;
    private long stepIndex = 0;
    private int progression = 0;
    private int lastChord = -1;

    public MusicDirector(AudioBackend backend, Rng rng) {
        this.backend = backend;
        this.rng = rng;
    }

    public static double midiToFreq(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    public static double drumLevel(double tension) {
        return 0.30 + 0.70 * AudioSettings.clamp01(tension);
    }

    public static double snareLevel(double tension) {
        return AudioSettings.clamp01((tension - 0.15) / 0.85);
    }

    public static double motifChance(double tension) {
        return 0.20 + 0.50 * AudioSettings.clamp01(tension);
    }

    public void reset() {
        started = false;
        stepIndex = 0;
        lastChord = -1;
    }

    public void update(double now, double tension) {
        if (!started) {
            started = true;
            nextStepTime = now + 0.1;
            stepIndex = 0;
        }
        // If the clock ran far ahead (tab was suspended), don't try to catch up.
        // This is where that annoying screech is dealth with.
        if (nextStepTime < now - 0.5)
            nextStepTime = now + 0.05;
        while (nextStepTime < now + LOOKAHEAD) {
            scheduleStep(stepIndex, nextStepTime, tension);
            stepIndex++;
            nextStepTime += STEP_SECONDS;
        }
    }

    private void scheduleStep(long step, double t, double tension) {
        int inChord = (int) (step % STEPS_PER_CHORD);
        int chordNumber = (int) (step / STEPS_PER_CHORD);
        int chordIdx = chordNumber % 4;

        if (inChord == 0) {
            if (chordIdx == 0 && chordNumber != lastChord) {
                progression = rng.nextInt(PROGRESSIONS.length);
            }
            lastChord = chordNumber;
            playChord(t, tension);
        }

        int bar = (int) ((step / STEPS_PER_BAR) % 4);
        int inBar = (int) (step % STEPS_PER_BAR);
        playDrums(inBar, bar, t, tension);

        if (inChord == 0 || inChord == 16) {
            if (rng.chance(motifChance(tension)))
                playMotif(t, tension);
        }
    }

    private int chordRoot() {
        int chordIdx = (int) (Math.max(lastChord, 0) % 4);
        return ROOT_MIDI + PROGRESSIONS[progression][chordIdx];
    }

    private boolean chordMinor() {
        int chordIdx = (int) (Math.max(lastChord, 0) % 4);
        return MINOR[progression][chordIdx];
    }

    private void playChord(double t, double tension) {
        int root = chordRoot();
        int third = root + (chordMinor() ? 3 : 4) + 12;
        int fifth = root + 7 + 12;
        double dur = STEP_SECONDS * STEPS_PER_CHORD + 1.4;
        int[] notes = { root + 12, third, fifth };
        for (int n : notes) {
            double f = midiToFreq(n);
            backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_SAW, f * 0.997, 0, t, dur, 0.030, 1.6, 1.8, 650, -0.2);
            backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_TRIANGLE, f * 1.003, 0, t, dur, 0.040, 1.6, 1.8, 900, 0.2);
        }
        backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_SINE, midiToFreq(root), 0, t, dur, 0.13, 1.2, 1.8, 0, 0);
    }

    private void playDrums(int inBar, int bar, double t, double tension) {
        double drum = drumLevel(tension);
        double snare = snareLevel(tension);

        if (inBar == 0) {
            backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_SINE, 92, 46, t, 0.38, 0.22 * drum, 0.005, 0.30, 0, 0);
        } else if (inBar == 8 && tension > 0.3) {
            backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_SINE, 80, 42, t, 0.30, 0.12 * drum, 0.005, 0.24, 0, 0);
        }

        if (snare > 0.0) {
            if (inBar == 4 || inBar == 12) {
                backend.noise(AudioBus.MUSIC, AudioBackend.FILTER_BANDPASS, 1800, 0, 0.8, t, 0.12,
                        0.07 * snare, 0.002, 0.10, 0.1);
            } else if (inBar == 14 && rng.chance(0.4)) {
                backend.noise(AudioBus.MUSIC, AudioBackend.FILTER_BANDPASS, 2000, 0, 0.8, t, 0.07,
                        0.03 * snare, 0.002, 0.05, 0.1);
            }

            if (bar == 3 && inBar >= 12) {
                double ramp = 0.02 + 0.015 * (inBar - 12);
                backend.noise(AudioBus.MUSIC, AudioBackend.FILTER_BANDPASS, 2100, 0, 0.8, t, 0.06,
                        ramp * snare, 0.002, 0.04, 0.1);
            }
        }
    }

    private void playMotif(double t, double tension) {
        int[][] motif = MOTIFS[rng.nextInt(MOTIFS.length)];
        int root = chordRoot() + 24;
        for (int[] note : motif) {
            double start = t + note[1] * STEP_SECONDS;
            double dur = note[2] * STEP_SECONDS;
            double f = midiToFreq(root + note[0]);
            backend.osc(AudioBus.MUSIC, AudioBackend.WAVE_SAW, f, 0, start, dur + 0.15,
                    0.045, 0.05, 0.20, 1100, 0);
        }
    }
}
