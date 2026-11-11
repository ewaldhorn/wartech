package wartech.audio;

public interface AudioBackend {
    int WAVE_SINE = 0;
    int WAVE_TRIANGLE = 1;
    int WAVE_SAW = 2;
    int WAVE_SQUARE = 3;

    int FILTER_LOWPASS = 0;
    int FILTER_BANDPASS = 1;
    int FILTER_HIGHPASS = 2;

    boolean init();

    double now();

    void setBusGain(int bus, double gain);

    void setMasterGain(double gain);

    void suspend();

    void resume();

    void osc(int bus, int wave, double f0, double f1, double start, double dur,
            double gain, double attack, double release, double cutoff, double pan);

    void noise(int bus, int filter, double f0, double f1, double q, double start, double dur,
            double gain, double attack, double release, double pan);

    void startWind();

    void setWind(double freq, double gain, double seconds);
}
