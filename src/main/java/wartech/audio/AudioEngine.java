package wartech.audio;

import wartech.util.Rng;

public final class AudioEngine {
    public static final int MAX_VOICES = 24;
    public static final int EXPLOSION_EXTRA_VOICES = 8;

    private static AudioEngine instance;

    private final AudioBackend backend;
    private final Rng rng;
    private final MusicDirector music;
    private final AmbienceDirector ambience;
    private final Tension tension = new Tension();
    private final AudioSettings settings = new AudioSettings();

    private final double[] voiceEnds = new double[MAX_VOICES + EXPLOSION_EXTRA_VOICES];
    private final double[] lastPlayed = new double[Sfx.COUNT];

    private boolean unlocked = false;
    private boolean suspended = false;
    private boolean failed = false;

    private double viewX, viewY, viewW = 1.0, viewH = 1.0;
    private boolean hasView = false;

    public AudioEngine(AudioBackend backend, long seed) {
        this.backend = backend;
        this.rng = new Rng(seed);
        this.music = new MusicDirector(backend, rng);
        this.ambience = new AmbienceDirector(backend, rng);
        java.util.Arrays.fill(lastPlayed, -1e9);
    }

    public static void install(AudioBackend backend, long seed) {
        instance = new AudioEngine(backend, seed);
    }

    public static AudioEngine get() {
        return instance;
    }

    public static void playSfx(int type, double x, double y) {
        if (instance != null)
            instance.sfx(type, x, y);
    }

    public static void viewChanged(double x, double y, double w, double h) {
        if (instance != null)
            instance.setView(x, y, w, h);
    }

    public static void tick(double dt) {
        if (instance != null)
            instance.update(dt);
    }

    public AudioSettings settings() {
        return settings;
    }

    public Tension tension() {
        return tension;
    }

    public boolean isUnlocked() {
        return unlocked && !failed;
    }

    public boolean hasFailed() {
        return failed;
    }

    private void fail() {
        failed = true;
    }

    public void unlock() {
        if (failed)
            return;
        try {
            if (unlocked) {
                if (suspended)
                    setSuspended(false);
                return;
            }
            if (!backend.init())
                return;
            unlocked = true;
            applyVolumes();
        } catch (RuntimeException | Error e) {
            fail();
        }
    }

    public void setMusicVolume(double v) {
        settings.music = AudioSettings.clamp01(v);
        applyVolumes();
    }

    public void setSfxVolume(double v) {
        settings.sfx = AudioSettings.clamp01(v);
        applyVolumes();
    }

    public void setAmbienceVolume(double v) {
        settings.ambience = AudioSettings.clamp01(v);
        applyVolumes();
    }

    public void setMuted(boolean muted) {
        settings.muted = muted;
        applyVolumes();
    }

    private void applyVolumes() {
        if (!unlocked || failed)
            return;
        try {
            backend.setBusGain(AudioBus.MUSIC, settings.busGain(AudioBus.MUSIC));
            backend.setBusGain(AudioBus.SFX, settings.busGain(AudioBus.SFX));
            backend.setBusGain(AudioBus.AMBIENCE, settings.busGain(AudioBus.AMBIENCE));
            backend.setMasterGain(settings.muted ? 0.0 : 1.0);
        } catch (RuntimeException | Error e) {
            fail();
        }
    }

    public void setSuspended(boolean value) {
        if (!unlocked || failed || suspended == value)
            return;
        suspended = value;
        try {
            if (value)
                backend.suspend();
            else
                backend.resume();
        } catch (RuntimeException | Error e) {
            fail();
        }
    }

    public void setView(double x, double y, double w, double h) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(w) || !Double.isFinite(h))
            return;
        viewX = x;
        viewY = y;
        viewW = Math.max(1.0, w);
        viewH = Math.max(1.0, h);
        hasView = true;
    }

    public void update(double dt) {
        tension.update(dt);
        if (!unlocked || suspended || failed)
            return;
        try {
            double now = backend.now();
            if (Double.isNaN(now) || Double.isInfinite(now))
                return;
            music.update(now, tension.level());
            ambience.update(now, tension.level());
        } catch (RuntimeException | Error e) {
            fail();
        }
    }

    public void sfx(int type, double x, double y) {
        if (type < 0 || type >= Sfx.COUNT)
            return;
        tension.add(Sfx.tensionWeight(type));
        if (!unlocked || suspended || failed)
            return;
        if (Double.isNaN(x) || Double.isNaN(y))
            return;
        try {
            playSfxUnchecked(type, x, y);
        } catch (RuntimeException | Error e) {
            fail();
        }
    }

    private void playSfxUnchecked(int type, double x, double y) {
        double now = backend.now();
        if (Double.isNaN(now) || Double.isInfinite(now))
            return;
        if (now - lastPlayed[type] < Sfx.cooldown(type))
            return;

        double gain = 1.0;
        double pan = 0.0;
        if (Sfx.positional(type) && hasView) {
            double dx = (x - (viewX + viewW / 2.0)) / (viewW / 2.0);
            double dy = (y - (viewY + viewH / 2.0)) / (viewH / 2.0);
            double over = Math.max(Math.abs(dx), Math.abs(dy)) - 1.0;
            if (over > 2.0)
                return;
            if (over > 0.0)
                gain = 1.0 / (1.0 + 1.5 * over);
            pan = Math.max(-1.0, Math.min(1.0, dx)) * 0.7;
        }

        boolean explosion = type == Sfx.EXPLOSION_SMALL || type == Sfx.EXPLOSION_BIG;
        int cap = explosion ? voiceEnds.length : MAX_VOICES;
        if (!claimVoice(now, cap, type))
            return;

        lastPlayed[type] = now;
        Sfx.play(backend, type, now, gain, pan);
    }

    private boolean claimVoice(double now, int cap, int type) {
        int active = 0;
        int free = -1;
        for (int i = 0; i < voiceEnds.length; i++) {
            if (voiceEnds[i] > now)
                active++;
            else if (free < 0)
                free = i;
        }
        if (active >= cap || free < 0)
            return false;
        voiceEnds[free] = now + voiceLength(type);
        return true;
    }

    private static double voiceLength(int type) {
        return switch (type) {
            case Sfx.EXPLOSION_BIG -> 1.2;
            case Sfx.EXPLOSION_SMALL -> 0.55;
            case Sfx.UNIT_READY -> 0.4;
            default -> 0.12;
        };
    }
}
