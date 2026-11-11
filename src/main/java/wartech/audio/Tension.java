package wartech.audio;

/**
 * Rolling "combat intensity" in 0..1 that drives the music layers. Combat
 * events add energy and it decays over time, and the exposed level is smoothed
 * so layers fade rather than jump.
 */
public final class Tension {
    public static final double FULL_ENERGY = 1.0;
    public static final double DECAY_PER_SECOND = 0.08;
    public static final double RISE_RATE = 0.6;
    public static final double FALL_RATE = 0.25;

    private double energy = 0.0;
    private double level = 0.0;

    public void add(double amount) {
        if (!(amount > 0.0) || Double.isInfinite(amount))
            return;
        energy = Math.min(FULL_ENERGY * 1.5, energy + amount);
    }

    public void update(double dt) {
        if (!(dt > 0.0) || Double.isInfinite(dt))
            return;
        energy = Math.max(0.0, energy - DECAY_PER_SECOND * dt);
        double target = Math.min(1.0, energy / FULL_ENERGY);
        double rate = target > level ? RISE_RATE : FALL_RATE;
        double step = rate * dt;
        if (Math.abs(target - level) <= step) {
            level = target;
        } else {
            level += Math.signum(target - level) * step;
        }
    }

    public double level() {
        return level;
    }

    public void reset() {
        energy = 0.0;
        level = 0.0;
    }
}
