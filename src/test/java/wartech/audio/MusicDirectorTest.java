package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wartech.util.Rng;

class MusicDirectorTest {
    private static RecordingBackend run(long seed, double tension, double seconds) {
        var b = new RecordingBackend();
        var m = new MusicDirector(b, new Rng(seed));
        for (double t = 0; t < seconds; t += 0.05) {
            b.clock = t;
            m.update(t, tension);
        }
        return b;
    }

    @Test
    void midiToFreqMatchesConcertPitch() {
        assertEquals(440.0, MusicDirector.midiToFreq(69), 1e-9);
        assertEquals(880.0, MusicDirector.midiToFreq(81), 1e-9);
        assertEquals(73.416, MusicDirector.midiToFreq(38), 0.01);
    }

    @Test
    void layerLevelsFollowTension() {
        assertTrue(MusicDirector.drumLevel(0.0) < MusicDirector.drumLevel(1.0));
        assertEquals(0.0, MusicDirector.snareLevel(0.0));
        assertEquals(0.0, MusicDirector.snareLevel(0.15));
        assertEquals(1.0, MusicDirector.snareLevel(1.0), 1e-9);
        assertTrue(MusicDirector.motifChance(1.0) > MusicDirector.motifChance(0.0));
    }

    @Test
    void sameSeedIsDeterministic() {
        var a = run(5, 0.5, 30);
        var b = run(5, 0.5, 30);
        assertEquals(a.notes, b.notes);
        assertEquals(a.noises, b.noises);
    }

    @Test
    void schedulesOnlyOnMusicBusAndAhead() {
        var b = run(1, 0.5, 20);
        assertFalse(b.notes.isEmpty());
        for (var n : b.notes)
            assertEquals(AudioBus.MUSIC, n.bus());
        for (var n : b.noises)
            assertEquals(AudioBus.MUSIC, n.bus());
    }

    @Test
    void calmMusicHasNoSnare() {
        var b = run(2, 0.0, 30);
        assertTrue(b.noises.isEmpty());
        assertFalse(b.notes.isEmpty()); // pad, drone and soft drum still play
    }

    @Test
    void tensionAddsPercussion() {
        var calm = run(3, 0.0, 30);
        var tense = run(3, 1.0, 30);
        assertTrue(tense.noises.size() > 10);
        assertTrue(tense.notes.size() > calm.notes.size());
    }

    @Test
    void stepsAreEvenlySpaced() {
        var b = run(4, 1.0, 10);
        double bar = MusicDirector.STEP_SECONDS * MusicDirector.STEPS_PER_BAR;
        double prev = -1;
        for (var n : b.notes) {
            if (n.wave() == AudioBackend.WAVE_SINE && n.f0() == 92.0) {
                if (prev >= 0)
                    assertEquals(bar, n.start() - prev, 1e-9);
                prev = n.start();
            }
        }
        assertTrue(prev > 0);
    }

    @Test
    void doesNotCatchUpAfterLongPause() {
        var b = new RecordingBackend();
        var m = new MusicDirector(b, new Rng(9));
        m.update(0.0, 0.0);
        int before = b.notes.size();
        m.update(600.0, 0.0); // tab was suspended for ten minutes
        assertTrue(b.notes.size() - before < 200);
    }
}
