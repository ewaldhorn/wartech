package wartech.audio;

import wartech.util.Rng;

/** Desert wind with a random gust now and then. */
public final class AmbienceDirector {
    public static final double BASE_FREQ_MIN = 280.0;
    public static final double BASE_FREQ_MAX = 520.0;
    public static final double BASE_GAIN_MIN = 0.035;
    public static final double BASE_GAIN_MAX = 0.060;
    public static final double GUST_GAIN = 0.16;
    public static final double GUST_GAP_MIN = 8.0;
    public static final double GUST_GAP_MAX = 25.0;

    private final AudioBackend backend;
    private final Rng rng;

    private boolean started = false;
    private double nextDriftAt;
    private double nextGustAt;
    private double gustEndsAt = -1.0;
    private double gustDuration;
    private double baseFreq = 400.0;
    private double baseGain = 0.045;

    public AmbienceDirector(AudioBackend backend, Rng rng) {
        this.backend = backend;
        this.rng = rng;
    }

    public boolean gustActive() {
        return gustEndsAt >= 0.0;
    }

    public void reset() {
        started = false;
        gustEndsAt = -1.0;
    }

    /** Soften ambient noises during heavy battles */
    public static double softenAmbientNoise(double tension) {
        return 1.0 - 0.4 * AudioSettings.clamp01(tension);
    }

    public void update(double now, double tension) {
        double soften = softenAmbientNoise(tension);
        if (!started) {
            started = true;
            backend.startWind();
            nextDriftAt = now;
            nextGustAt = now + rng.range(GUST_GAP_MIN, GUST_GAP_MAX);
        }

        if (gustEndsAt >= 0.0) {
            if (now >= gustEndsAt) {
                gustEndsAt = -1.0;
                backend.setWind(baseFreq, baseGain * soften, 3.0);
                nextDriftAt = now + 3.0;
                nextGustAt = now + rng.range(GUST_GAP_MIN, GUST_GAP_MAX);
            }
            return;
        }

        if (now >= nextGustAt) {
            startGust(now, soften);
            return;
        }

        if (now >= nextDriftAt) {
            baseFreq = rng.range(BASE_FREQ_MIN, BASE_FREQ_MAX);
            baseGain = rng.range(BASE_GAIN_MIN, BASE_GAIN_MAX);
            backend.setWind(baseFreq, baseGain * soften, 4.0);
            nextDriftAt = now + rng.range(4.0, 9.0);
        }
    }

    private void startGust(double now, double duck) {
        gustDuration = rng.range(3.0, 6.0);
        gustEndsAt = now + gustDuration;
        backend.setWind(rng.range(700.0, 1100.0), GUST_GAIN * duck, 1.5);
        if (rng.chance(0.4)) {
            double f0 = rng.range(700.0, 1000.0);
            double f1 = rng.range(500.0, 1300.0);
            backend.osc(AudioBus.AMBIENCE, AudioBackend.WAVE_SINE, f0, f1, now + 0.3, gustDuration,
                    0.012 * duck, 1.5, 2.0, 0, rng.range(-0.5, 0.5));
        }
    }
}
