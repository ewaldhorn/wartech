package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wartech.util.Rng;

class AmbienceDirectorTest {
    @Test
    void startsWindOnFirstUpdate() {
        var b = new RecordingBackend();
        var a = new AmbienceDirector(b, new Rng(1));
        a.update(0.0, 0.0);
        assertTrue(b.windStarted);
        assertFalse(b.windSets.isEmpty());
    }

    @Test
    void gustsOccurWithinTheGapWindow() {
        var b = new RecordingBackend();
        var a = new AmbienceDirector(b, new Rng(2));
        boolean sawGust = false;
        double firstGust = -1;
        for (double t = 0; t < AmbienceDirector.GUST_GAP_MAX + 1; t += 0.1) {
            a.update(t, 0.0);
            if (a.gustActive() && !sawGust) {
                sawGust = true;
                firstGust = t;
            }
        }
        assertTrue(sawGust);
        assertTrue(firstGust >= AmbienceDirector.GUST_GAP_MIN - 0.2);
    }

    @Test
    void gustEndsAndWindReturnsToBase() {
        var b = new RecordingBackend();
        var a = new AmbienceDirector(b, new Rng(3));
        boolean wasActive = false;
        boolean ended = false;
        for (double t = 0; t < 60; t += 0.1) {
            a.update(t, 0.0);
            if (a.gustActive())
                wasActive = true;
            else if (wasActive) {
                ended = true;
                break;
            }
        }
        assertTrue(ended);
    }

    @Test
    void windGainStaysQuietExceptInGusts() {
        var b = new RecordingBackend();
        var a = new AmbienceDirector(b, new Rng(4));
        for (double t = 0; t < 120; t += 0.1)
            a.update(t, 0.0);
        for (double[] set : b.windSets) {
            assertTrue(set[1] <= AmbienceDirector.GUST_GAIN + 1e-9);
            assertTrue(set[1] >= AmbienceDirector.BASE_GAIN_MIN - 1e-9);
        }
    }

    @Test
    void softensUnderTension() {
        assertEquals(1.0, AmbienceDirector.softenAmbientNoise(0.0));
        assertEquals(0.6, AmbienceDirector.softenAmbientNoise(1.0), 1e-9);
    }

    @Test
    void ambienceOscsUseAmbienceBus() {
        var b = new RecordingBackend();
        var a = new AmbienceDirector(b, new Rng(5));
        for (double t = 0; t < 300; t += 0.1)
            a.update(t, 0.0);
        for (var n : b.notes)
            assertEquals(AudioBus.AMBIENCE, n.bus());
    }
}
