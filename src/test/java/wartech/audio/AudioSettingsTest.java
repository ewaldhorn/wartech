package wartech.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioSettingsTest {
    @Test
    void curveIsSquareAndClamped() {
        assertEquals(0.0, AudioSettings.curve(0.0));
        assertEquals(0.25, AudioSettings.curve(0.5), 1e-12);
        assertEquals(1.0, AudioSettings.curve(1.0));
        assertEquals(1.0, AudioSettings.curve(5.0));
        assertEquals(0.0, AudioSettings.curve(-1.0));
        assertEquals(0.0, AudioSettings.curve(Double.NaN));
    }

    @Test
    void parseFallsBackOnBadInput() {
        assertEquals(0.4, AudioSettings.parse(null, 0.4));
        assertEquals(0.4, AudioSettings.parse("abc", 0.4));
        assertEquals(0.4, AudioSettings.parse("NaN", 0.4));
        assertEquals(0.7, AudioSettings.parse(" 0.7 ", 0.4), 1e-12);
        assertEquals(1.0, AudioSettings.parse("9", 0.4));
    }

    @Test
    void fromPercentMapsSliderRange() {
        assertEquals(0.35, AudioSettings.fromPercent("35"), 1e-12);
        assertEquals(1.0, AudioSettings.fromPercent("100"));
        assertEquals(1.0, AudioSettings.fromPercent("250"));
        assertEquals(0.0, AudioSettings.fromPercent("-5"));
        assertEquals(0.0, AudioSettings.fromPercent("x"));
        assertEquals(0.0, AudioSettings.fromPercent(null));
    }

    @Test
    void busGainUsesTheMatchingSlider() {
        var s = new AudioSettings();
        s.music = 1.0;
        s.sfx = 0.5;
        s.ambience = 0.0;
        assertEquals(1.0, s.busGain(AudioBus.MUSIC));
        assertEquals(0.25, s.busGain(AudioBus.SFX), 1e-12);
        assertEquals(0.0, s.busGain(AudioBus.AMBIENCE));
        assertEquals(0.0, s.busGain(99));
    }
}
