package wartech.audio;

import java.util.ArrayList;
import java.util.List;

final class RecordingBackend implements AudioBackend {
    record Note(int bus, int wave, double f0, double start, double dur, double gain) {
    }

    record Noise(int bus, double start, double gain) {
    }

    double clock = 0.0;
    boolean initOk = true;
    boolean inited = false;
    boolean suspended = false;
    boolean windStarted = false;
    double master = -1;
    final double[] busGain = { -1, -1, -1 };
    final List<Note> notes = new ArrayList<>();
    final List<Noise> noises = new ArrayList<>();
    final List<double[]> windSets = new ArrayList<>();

    @Override
    public boolean init() {
        inited = initOk;
        return initOk;
    }

    @Override
    public double now() {
        return clock;
    }

    @Override
    public void setBusGain(int bus, double gain) {
        busGain[bus] = gain;
    }

    @Override
    public void setMasterGain(double gain) {
        master = gain;
    }

    @Override
    public void suspend() {
        suspended = true;
    }

    @Override
    public void resume() {
        suspended = false;
    }

    @Override
    public void osc(int bus, int wave, double f0, double f1, double start, double dur,
            double gain, double attack, double release, double cutoff, double pan) {
        notes.add(new Note(bus, wave, f0, start, dur, gain));
    }

    @Override
    public void noise(int bus, int filter, double f0, double f1, double q, double start, double dur,
            double gain, double attack, double release, double pan) {
        noises.add(new Noise(bus, start, gain));
    }

    @Override
    public void startWind() {
        windStarted = true;
    }

    @Override
    public void setWind(double freq, double gain, double seconds) {
        windSets.add(new double[] { freq, gain, seconds });
    }

    int count(int bus) {
        int n = 0;
        for (Note x : notes)
            if (x.bus == bus)
                n++;
        for (Noise x : noises)
            if (x.bus == bus)
                n++;
        return n;
    }
}
