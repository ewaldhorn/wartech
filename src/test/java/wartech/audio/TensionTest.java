package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TensionTest {
    @Test
    void startsCalm() {
        assertEquals(0.0, new Tension().level());
    }

    @Test
    void risesGraduallyAfterEvents() {
        var t = new Tension();
        t.add(0.8);
        t.update(0.1);
        double early = t.level();
        assertTrue(early > 0.0 && early < 0.8);
        for (int i = 0; i < 100; i++)
            t.update(0.1);
        assertTrue(t.level() >= 0.0);
    }

    @Test
    void reachesAndCapsAtOne() {
        var t = new Tension();
        t.add(10.0);
        for (int i = 0; i < 25; i++)
            t.update(0.1);
        assertEquals(1.0, t.level(), 1e-9);
    }

    @Test
    void decaysBackToZeroWhenQuiet() {
        var t = new Tension();
        t.add(1.0);
        for (int i = 0; i < 50; i++)
            t.update(0.1);
        assertTrue(t.level() > 0.3);
        for (int i = 0; i < 2000; i++)
            t.update(0.1);
        assertEquals(0.0, t.level(), 1e-9);
    }

    @Test
    void ignoresNegativeAddAndZeroDt() {
        var t = new Tension();
        t.add(-5.0);
        t.update(0.0);
        t.update(-1.0);
        assertEquals(0.0, t.level());
    }

    @Test
    void resetClears() {
        var t = new Tension();
        t.add(1.0);
        t.update(1.0);
        t.reset();
        assertEquals(0.0, t.level());
    }
}
