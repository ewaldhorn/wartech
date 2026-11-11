package wartech.audio;

public final class AudioSettings {
    public static final double DEFAULT_MUSIC = 0.35;
    public static final double DEFAULT_SFX = 0.60;
    public static final double DEFAULT_AMBIENCE = 0.50;

    public double music = DEFAULT_MUSIC;
    public double sfx = DEFAULT_SFX;
    public double ambience = DEFAULT_AMBIENCE;
    public boolean muted = false;

    public static double clamp01(double v) {
        if (Double.isNaN(v))
            return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    public static double curve(double slider) {
        double v = clamp01(slider);
        return v * v;
    }

    public double busGain(int bus) {
        return switch (bus) {
            case AudioBus.MUSIC -> curve(music);
            case AudioBus.SFX -> curve(sfx);
            case AudioBus.AMBIENCE -> curve(ambience);
            default -> 0.0;
        };
    }

    public static double parse(String s, double fallback) {
        if (s == null)
            return fallback;
        try {
            double v = Double.parseDouble(s.trim());
            if (Double.isNaN(v) || Double.isInfinite(v))
                return fallback;
            return clamp01(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double fromPercent(String s) {
        if (s == null)
            return 0.0;
        try {
            double v = Double.parseDouble(s.trim());
            if (Double.isNaN(v) || Double.isInfinite(v))
                return 0.0;
            return clamp01(v / 100.0);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
