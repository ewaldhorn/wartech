package wartech.util;

public final class SimpleRng {
    private long state;

    public SimpleRng(long seed) {
        this.state = seed != 0 ? seed : 123456789L;
    }

    public int nextInt(int bound) {
        if (bound <= 0) return 0;
        state = (state * 6364136223846793005L + 1442695040888963407L);
        long val = (state >>> 32) & 0x7FFFFFFFL;
        return (int) (val % bound);
    }

    public boolean nextBoolean() {
        return nextInt(2) == 0;
    }
}
