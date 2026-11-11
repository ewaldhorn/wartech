package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Audio failures must not crash the game loop. */
class AudioFailureTest {
    static final class Bomb implements AudioBackend {
        boolean armed = false;
        int calls = 0;

        private void boom() {
            calls++;
            if (armed)
                throw new IllegalStateException("boom");
        }

        @Override
        public boolean init() {
            boom();
            return true;
        }

        @Override
        public double now() {
            boom();
            return 0.0;
        }

        @Override
        public void setBusGain(int bus, double gain) {
            boom();
        }

        @Override
        public void setMasterGain(double gain) {
            boom();
        }

        @Override
        public void suspend() {
            boom();
        }

        @Override
        public void resume() {
            boom();
        }

        @Override
        public void osc(int bus, int wave, double f0, double f1, double start, double dur,
                double gain, double attack, double release, double cutoff, double pan) {
            boom();
        }

        @Override
        public void noise(int bus, int filter, double f0, double f1, double q, double start, double dur,
                double gain, double attack, double release, double pan) {
            boom();
        }

        @Override
        public void startWind() {
            boom();
        }

        @Override
        public void setWind(double freq, double gain, double seconds) {
            boom();
        }
    }

    @Test
    void throwingInitDoesNotPropagate() {
        var b = new Bomb();
        b.armed = true;
        var e = new AudioEngine(b, 1);
        assertDoesNotThrow(e::unlock);
        assertTrue(e.hasFailed());
        assertFalse(e.isUnlocked());
    }

    @Test
    void throwingUpdateDisablesAudioPermanently() {
        var b = new Bomb();
        var e = new AudioEngine(b, 1);
        e.unlock();
        b.armed = true;
        assertDoesNotThrow(() -> e.update(0.1));
        assertTrue(e.hasFailed());
        int calls = b.calls;
        e.update(0.1);
        e.sfx(Sfx.FIRE, 0, 0);
        e.setMusicVolume(0.5);
        e.setMuted(true);
        e.setSuspended(true);
        assertEquals(calls, b.calls, "no backend calls after failure");
    }

    @Test
    void throwingSfxDoesNotPropagate() {
        var b = new Bomb();
        var e = new AudioEngine(b, 1);
        e.setView(0, 0, 100, 100);
        e.unlock();
        b.armed = true;
        assertDoesNotThrow(() -> e.sfx(Sfx.EXPLOSION_BIG, 50, 50));
        assertTrue(e.hasFailed());
    }

    @Test
    void throwingVolumeAndSuspendDoNotPropagate() {
        var b = new Bomb();
        var e = new AudioEngine(b, 1);
        e.unlock();
        b.armed = true;
        assertDoesNotThrow(() -> e.setSfxVolume(0.2));
        assertTrue(e.hasFailed());

        var b2 = new Bomb();
        var e2 = new AudioEngine(b2, 1);
        e2.unlock();
        b2.armed = true;
        assertDoesNotThrow(() -> e2.setSuspended(true));
        assertTrue(e2.hasFailed());
    }

    @Test
    void failedEngineStillTracksTensionSafely() {
        var b = new Bomb();
        var e = new AudioEngine(b, 1);
        e.unlock();
        b.armed = true;
        e.update(0.1);
        assertDoesNotThrow(() -> e.sfx(Sfx.FIRE, 0, 0));
    }

    @Test
    void nonFiniteInputsAreIgnored() {
        var b = new RecordingBackend();
        var e = new AudioEngine(b, 1);
        e.setView(0, 0, 1000, 1000);
        e.unlock();
        e.setView(Double.NaN, 0, 1000, 1000);
        e.sfx(Sfx.FIRE, Double.NaN, 5);
        e.update(Double.NaN);
        e.update(Double.POSITIVE_INFINITY);
        e.sfx(Sfx.FIRE, 500, 500);
        assertFalse(e.hasFailed());
        assertTrue(Double.isFinite(e.tension().level()));
        for (var n : b.notes)
            assertTrue(Double.isFinite(n.gain()) && Double.isFinite(n.start()));
    }

    @Test
    void nonFiniteBackendClockIsIgnored() {
        var b = new RecordingBackend();
        b.clock = Double.NaN;
        var e = new AudioEngine(b, 1);
        e.unlock();
        assertDoesNotThrow(() -> e.update(0.1));
        e.sfx(Sfx.FIRE, 0, 0);
        assertTrue(b.notes.isEmpty() && b.noises.isEmpty());
    }

    @Test
    void tensionRejectsNaNAndInfinity() {
        var t = new Tension();
        t.add(Double.NaN);
        t.add(Double.POSITIVE_INFINITY);
        t.update(Double.NaN);
        t.update(Double.POSITIVE_INFINITY);
        assertEquals(0.0, t.level());
        t.add(1.0);
        t.update(1.0);
        assertTrue(t.level() > 0.0);
    }

    @Test
    void invalidSfxTypeIsIgnored() {
        var e = new AudioEngine(new RecordingBackend(), 1);
        e.unlock();
        assertDoesNotThrow(() -> e.sfx(-1, 0, 0));
        assertDoesNotThrow(() -> e.sfx(999, 0, 0));
    }
}
