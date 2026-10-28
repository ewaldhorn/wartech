package wartech.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RngTest {
    @Test
    void sameSeedProducesSameSequence() {
        var a = new Rng(1234L);
        var b = new Rng(1234L);
        for (int i = 0; i < 100; i++) assertEquals(a.nextDouble(), b.nextDouble());
    }

    @Test
    void differentSeedsDiverge() {
        var a = new Rng(1L);
        var b = new Rng(2L);
        boolean differs = false;
        for (int i = 0; i < 10; i++) differs |= a.nextDouble() != b.nextDouble();
        assertTrue(differs);
    }

    @Test
    void zeroSeedIsUsable() {
        var r = new Rng(0x9E3779B97F4A7C15L); // XORs to a zero state
        double first = r.nextDouble();
        assertTrue(first >= 0.0 && first < 1.0);
        assertNotEquals(first, r.nextDouble());
    }

    @Test
    void nextDoubleInUnitInterval() {
        var r = new Rng(7L);
        for (int i = 0; i < 10_000; i++) {
            double d = r.nextDouble();
            assertTrue(d >= 0.0 && d < 1.0);
        }
    }

    @Test
    void nextIntRespectsBounds() {
        var r = new Rng(7L);
        for (int i = 0; i < 10_000; i++) {
            int v = r.nextInt(5);
            assertTrue(v >= 0 && v < 5);
        }
        assertEquals(0, r.nextInt(0));
        assertEquals(0, r.nextInt(-3));
    }

    @Test
    void nextIntRangeIsInclusive() {
        var r = new Rng(9L);
        boolean sawMin = false, sawMax = false;
        for (int i = 0; i < 10_000; i++) {
            int v = r.nextInt(3, 6);
            assertTrue(v >= 3 && v <= 6);
            sawMin |= v == 3;
            sawMax |= v == 6;
        }
        assertTrue(sawMin && sawMax);
        assertEquals(4, r.nextInt(4, 4));
        assertEquals(4, r.nextInt(4, 2));
    }

    @Test
    void rangeStaysWithinBounds() {
        var r = new Rng(11L);
        for (int i = 0; i < 10_000; i++) {
            double v = r.range(-2.5, 4.0);
            assertTrue(v >= -2.5 && v < 4.0);
        }
    }

    @Test
    void jitterStaysWithinMagnitude() {
        var r = new Rng(13L);
        boolean neg = false, pos = false;
        for (int i = 0; i < 10_000; i++) {
            double v = r.jitter(8.0);
            assertTrue(v >= -8.0 && v < 8.0);
            neg |= v < 0;
            pos |= v > 0;
        }
        assertTrue(neg && pos);
    }

    @Test
    void jitterWithCenterIsOffsetAroundCenter() {
        var r = new Rng(17L);
        for (int i = 0; i < 10_000; i++) {
            double v = r.jitter(100.0, 5.0);
            assertTrue(v >= 95.0 && v < 105.0);
        }
    }

    @Test
    void chanceExtremes() {
        var r = new Rng(19L);
        for (int i = 0; i < 1000; i++) {
            assertFalse(r.chance(0.0));
            assertTrue(r.chance(1.0));
        }
    }

    @Test
    void nextBooleanProducesBothValues() {
        var r = new Rng(23L);
        boolean t = false, f = false;
        for (int i = 0; i < 1000; i++) {
            if (r.nextBoolean()) t = true; else f = true;
        }
        assertTrue(t && f);
    }
}
