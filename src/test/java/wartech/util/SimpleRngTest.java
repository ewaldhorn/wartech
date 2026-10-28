package wartech.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** SimpleRng drives map generation; its output must stay stable so seeds keep producing the same maps. */
class SimpleRngTest {
    @Test
    void sameSeedProducesSameSequence() {
        var a = new SimpleRng(42L);
        var b = new SimpleRng(42L);
        for (int i = 0; i < 100; i++) assertEquals(a.nextInt(1000), b.nextInt(1000));
    }

    @Test
    void sequenceIsStable() {
        // Reference values computed by hand from the LCG; guards against accidental algorithm changes.
        var r = new SimpleRng(1L);
        long state = 1L;
        for (int i = 0; i < 20; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int expected = (int) (((state >>> 32) & 0x7FFFFFFFL) % 100);
            assertEquals(expected, r.nextInt(100));
        }
    }

    @Test
    void zeroSeedUsesFallback() {
        var a = new SimpleRng(0L);
        var b = new SimpleRng(123456789L);
        for (int i = 0; i < 20; i++) assertEquals(b.nextInt(50), a.nextInt(50));
    }

    @Test
    void nextIntRespectsBound() {
        var r = new SimpleRng(5L);
        for (int i = 0; i < 10_000; i++) {
            int v = r.nextInt(7);
            assertTrue(v >= 0 && v < 7);
        }
        assertEquals(0, r.nextInt(0));
    }
}
