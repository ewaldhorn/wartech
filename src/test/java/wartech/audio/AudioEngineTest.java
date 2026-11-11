package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioEngineTest {
    private static RecordingBackend b;

    private static AudioEngine unlocked() {
        b = new RecordingBackend();
        var e = new AudioEngine(b, 1);
        e.setView(0, 0, 1000, 1000);
        e.unlock();
        return e;
    }

    @Test
    void nothingPlaysBeforeUnlock() {
        b = new RecordingBackend();
        var e = new AudioEngine(b, 1);
        e.sfx(Sfx.FIRE, 10, 10);
        e.update(0.1);
        assertTrue(b.notes.isEmpty() && b.noises.isEmpty());
        assertFalse(b.inited);
    }

    @Test
    void failedInitStaysLocked() {
        b = new RecordingBackend();
        b.initOk = false;
        var e = new AudioEngine(b, 1);
        e.unlock();
        assertFalse(e.isUnlocked());
    }

    @Test
    void unlockAppliesVolumes() {
        var e = unlocked();
        assertEquals(AudioSettings.curve(AudioSettings.DEFAULT_MUSIC), b.busGain[AudioBus.MUSIC], 1e-12);
        assertEquals(AudioSettings.curve(AudioSettings.DEFAULT_SFX), b.busGain[AudioBus.SFX], 1e-12);
        assertEquals(AudioSettings.curve(AudioSettings.DEFAULT_AMBIENCE), b.busGain[AudioBus.AMBIENCE], 1e-12);
        assertEquals(1.0, b.master);
        assertTrue(e.isUnlocked());
    }

    @Test
    void volumeChangesBeforeUnlockAreAppliedAtUnlock() {
        b = new RecordingBackend();
        var e = new AudioEngine(b, 1);
        e.setMusicVolume(1.0);
        e.setSfxVolume(0.0);
        e.setAmbienceVolume(0.5);
        e.unlock();
        assertEquals(1.0, b.busGain[AudioBus.MUSIC]);
        assertEquals(0.0, b.busGain[AudioBus.SFX]);
        assertEquals(0.25, b.busGain[AudioBus.AMBIENCE], 1e-12);
    }

    @Test
    void slidersAreIndependent() {
        var e = unlocked();
        double sfxBefore = b.busGain[AudioBus.SFX];
        e.setMusicVolume(0.9);
        assertEquals(0.81, b.busGain[AudioBus.MUSIC], 1e-12);
        assertEquals(sfxBefore, b.busGain[AudioBus.SFX]);
        e.setAmbienceVolume(0.2);
        assertEquals(0.04, b.busGain[AudioBus.AMBIENCE], 1e-12);
        assertEquals(0.81, b.busGain[AudioBus.MUSIC], 1e-12);
    }

    @Test
    void muteSilencesMasterAndRestores() {
        var e = unlocked();
        e.setMuted(true);
        assertEquals(0.0, b.master);
        e.setMuted(false);
        assertEquals(1.0, b.master);
    }

    @Test
    void volumesAreClamped() {
        var e = unlocked();
        e.setMusicVolume(7);
        assertEquals(1.0, e.settings().music);
        e.setSfxVolume(-3);
        assertEquals(0.0, e.settings().sfx);
    }

    @Test
    void sfxHonoursCooldown() {
        var e = unlocked();
        e.sfx(Sfx.FIRE, 500, 500);
        int first = b.noises.size();
        assertTrue(first > 0);
        e.sfx(Sfx.FIRE, 500, 500);
        assertEquals(first, b.noises.size());
        b.clock += Sfx.cooldown(Sfx.FIRE) + 0.01;
        e.sfx(Sfx.FIRE, 500, 500);
        assertTrue(b.noises.size() > first);
    }

    @Test
    void sfxPlayOnSfxBus() {
        var e = unlocked();
        e.sfx(Sfx.EXPLOSION_SMALL, 500, 500);
        assertTrue(b.count(AudioBus.SFX) > 0);
        assertEquals(0, b.count(AudioBus.MUSIC));
    }

    @Test
    void farOffscreenSoundsAreSkipped() {
        var e = unlocked();
        e.sfx(Sfx.EXPLOSION_BIG, 10_000, 500);
        assertEquals(0, b.count(AudioBus.SFX));
    }

    @Test
    void nonPositionalSoundsIgnoreView() {
        var e = unlocked();
        e.sfx(Sfx.ORDER_MOVE, 100_000, 100_000);
        assertEquals(1, b.count(AudioBus.SFX));
    }

    @Test
    void voiceCapLimitsSimultaneousSounds() {
        var e = unlocked();
        int played = 0;
        for (int i = 0; i < 200; i++) {
            b.clock += 0.001; // too short for cooldowns of most, but varied types below
            int before = b.count(AudioBus.SFX);
            e.sfx(Sfx.UNIT_READY, 500, 500);
            e.sfx(Sfx.ORDER_MOVE, 500, 500);
            e.sfx(Sfx.HIT, 500, 500);
            if (b.count(AudioBus.SFX) > before)
                played++;
        }
        assertTrue(played < 200);
    }

    @Test
    void combatEventsRaiseTension() {
        var e = unlocked();
        for (int i = 0; i < 30; i++) {
            b.clock += 1.0;
            e.sfx(Sfx.EXPLOSION_BIG, 500, 500);
            e.update(1.0);
        }
        assertTrue(e.tension().level() > 0.3);
    }

    @Test
    void tensionStillBuildsWhileLocked() {
        b = new RecordingBackend();
        var e = new AudioEngine(b, 1);
        for (int i = 0; i < 20; i++) {
            e.sfx(Sfx.EXPLOSION_BIG, 0, 0);
            e.update(0.5);
        }
        assertTrue(e.tension().level() > 0.0);
    }

    @Test
    void suspendStopsSchedulingAndResumes() {
        var e = unlocked();
        e.update(0.1);
        e.setSuspended(true);
        assertTrue(b.suspended);
        int n = b.notes.size() + b.noises.size();
        b.clock += 5;
        e.update(0.1);
        e.sfx(Sfx.FIRE, 500, 500);
        assertEquals(n, b.notes.size() + b.noises.size());
        e.setSuspended(false);
        assertFalse(b.suspended);
    }

    @Test
    void updateDrivesMusicAndAmbience() {
        var e = unlocked();
        for (int i = 0; i < 100; i++) {
            b.clock = i * 0.05;
            e.update(0.05);
        }
        assertTrue(b.count(AudioBus.MUSIC) > 0);
        assertTrue(b.windStarted);
    }

    @Test
    void staticFacadeIsSafeWithoutEngine() {
        AudioEngine.install(new RecordingBackend(), 1);
        AudioEngine.playSfx(Sfx.FIRE, 0, 0);
        AudioEngine.viewChanged(0, 0, 10, 10);
        AudioEngine.tick(0.1);
        assertNotNull(AudioEngine.get());
    }
}
