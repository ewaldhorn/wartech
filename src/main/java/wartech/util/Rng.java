package wartech.util;

public final class Rng {
    private long state;

    public Rng(long seed) {
        this.state = seed ^ 0x9E3779B97F4A7C15L;
        if (this.state == 0L) this.state = 0x2545F4914F6CDD1DL;
    }

    public double nextDouble() {
        state = state * 6364136223846793005L + 1442695040888963407L;
        return (double) ((state >>> 11) & 0x1FFFFFFFFFFFFFL) * (1.0 / 9007199254740992.0);
    }

    public int nextInt(int bound) {
        if (bound <= 0) return 0;
        return (int) (nextDouble() * bound);
    }

    public int nextInt(int min, int max) {
        if (max <= min) return min;
        return min + nextInt(max - min + 1);
    }

    public boolean nextBoolean() {
        return nextDouble() < 0.5;
    }

    public double range(double min, double max) {
        return min + nextDouble() * (max - min);
    }

    public double jitter(double magnitude) {
        return range(-magnitude, magnitude);
    }

    public double jitter(double center, double magnitude) {
        return center + range(-magnitude, magnitude);
    }

    public boolean chance(double probability) {
        return nextDouble() < probability;
    }
}
