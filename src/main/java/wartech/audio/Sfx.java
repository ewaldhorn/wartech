package wartech.audio;

public final class Sfx {
    public static final int FIRE = 0;
    public static final int HIT = 1;
    public static final int EXPLOSION_SMALL = 2;
    public static final int EXPLOSION_BIG = 3;
    public static final int UNIT_READY = 4;
    public static final int ORDER_MOVE = 5;
    public static final int ORDER_ATTACK = 6;
    public static final int COUNT = 7;

    private static final double[] COOLDOWN = { 0.05, 0.06, 0.08, 0.15, 0.25, 0.08, 0.08 };
    private static final double[] TENSION = { 0.012, 0.010, 0.06, 0.15, 0.0, 0.0, 0.0 };
    private static final boolean[] POSITIONAL = { true, true, true, true, true, false, false };

    private Sfx() {
    }

    public static double cooldown(int type) {
        return COOLDOWN[type];
    }

    public static double tensionWeight(int type) {
        return TENSION[type];
    }

    public static boolean positional(int type) {
        return POSITIONAL[type];
    }

    public static void play(AudioBackend b, int type, double t, double gain, double pan) {
        int bus = AudioBus.SFX;
        switch (type) {
            case FIRE -> {
                b.noise(bus, AudioBackend.FILTER_BANDPASS, 2200, 0, 0.7, t, 0.09, 0.10 * gain, 0.002, 0.07, pan);
                b.osc(bus, AudioBackend.WAVE_SQUARE, 180, 90, t, 0.05, 0.035 * gain, 0.002, 0.04, 1200, pan);
            }
            case HIT ->
                b.noise(bus, AudioBackend.FILTER_BANDPASS, 1200, 0, 0.8, t, 0.10, 0.08 * gain, 0.002, 0.08, pan);
            case EXPLOSION_SMALL -> {
                b.noise(bus, AudioBackend.FILTER_LOWPASS, 1800, 200, 0.7, t, 0.5, 0.22 * gain, 0.005, 0.40, pan);
                b.osc(bus, AudioBackend.WAVE_SINE, 90, 40, t, 0.45, 0.25 * gain, 0.005, 0.35, 0, pan);
            }
            case EXPLOSION_BIG -> {
                b.noise(bus, AudioBackend.FILTER_LOWPASS, 1200, 120, 0.7, t, 1.1, 0.35 * gain, 0.01, 0.9, pan);
                b.osc(bus, AudioBackend.WAVE_SINE, 70, 28, t, 1.0, 0.40 * gain, 0.01, 0.8, 0, pan);
            }
            case UNIT_READY -> {
                b.osc(bus, AudioBackend.WAVE_TRIANGLE, 520, 0, t, 0.18, 0.08 * gain, 0.005, 0.12, 2400, pan);
                b.osc(bus, AudioBackend.WAVE_TRIANGLE, 700, 0, t + 0.12, 0.22, 0.08 * gain, 0.005, 0.16, 2400, pan);
            }
            case ORDER_MOVE -> b.osc(bus, AudioBackend.WAVE_SINE, 600, 800, t, 0.06, 0.05 * gain, 0.003, 0.04, 0, pan);
            case ORDER_ATTACK ->
                b.osc(bus, AudioBackend.WAVE_SQUARE, 300, 200, t, 0.08, 0.04 * gain, 0.003, 0.06, 1500, pan);
            default -> {
            }
        }
    }
}
