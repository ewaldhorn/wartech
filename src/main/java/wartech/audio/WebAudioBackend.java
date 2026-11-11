package wartech.audio;

import org.teavm.jso.JSBody;

/**
 * {@link AudioBackend} backed by the browser's Web Audio API.
 *
 * <p>
 * Audio must never be able to take the page down, so every script body is
 * wrapped in
 * try/catch, validates its numeric inputs (Web Audio throws on NaN/Infinity),
 * and reports
 * failure through its return value instead of throwing across the JS/Wasm
 * boundary. After
 * {@link #MAX_FAILURES} consecutive failures the backend turns itself into a
 * no-op.
 */
public final class WebAudioBackend implements AudioBackend {
    public static final int MAX_FAILURES = 5;

    private int failures = 0;
    private boolean dead = false;

    private boolean ok(boolean success) {
        if (success) {
            failures = 0;
        } else if (++failures >= MAX_FAILURES) {
            dead = true;
        }
        return success;
    }

    public boolean isDead() {
        return dead;
    }

    @JSBody(script = """
            try {
              if (window.__wt) {
                if (window.__wt.ctx.state === 'suspended') { var p0 = window.__wt.ctx.resume(); if (p0 && p0.catch) p0.catch(function () {}); }
                return true;
              }
              var AC = window.AudioContext || window.webkitAudioContext;
              if (!AC) return false;
              var ctx = new AC();
              var comp = ctx.createDynamicsCompressor();
              var master = ctx.createGain();
              master.gain.value = 1;
              master.connect(comp);
              comp.connect(ctx.destination);
              var buses = [];
              for (var i = 0; i < 3; i++) { var g = ctx.createGain(); g.gain.value = 0.3; g.connect(master); buses.push(g); }
              var len = Math.max(1, Math.floor(ctx.sampleRate * 2));
              var buf = ctx.createBuffer(1, len, ctx.sampleRate);
              var d = buf.getChannelData(0);
              for (var j = 0; j < len; j++) d[j] = Math.random() * 2 - 1;
              window.__wt = { ctx: ctx, buses: buses, master: master, noise: buf, wind: null };
              if (ctx.state === 'suspended') { var p = ctx.resume(); if (p && p.catch) p.catch(function () {}); }
              return true;
            } catch (e) {
              window.__wt = null;
              return false;
            }
            """)
    private static native boolean nInit();

    @JSBody(script = """
            try { return window.__wt ? window.__wt.ctx.currentTime : 0; } catch (e) { return 0; }
            """)
    private static native double nNow();

    @JSBody(params = { "bus", "v" }, script = """
            try {
              var w = window.__wt; if (!w) return false;
              if (!isFinite(v) || !w.buses[bus]) return false;
              w.buses[bus].gain.setTargetAtTime(v, w.ctx.currentTime, 0.02);
              return true;
            } catch (e) { return false; }
            """)
    private static native boolean nBus(int bus, double v);

    @JSBody(params = { "v" }, script = """
            try {
              var w = window.__wt; if (!w) return false;
              if (!isFinite(v)) return false;
              w.master.gain.setTargetAtTime(v, w.ctx.currentTime, 0.02);
              return true;
            } catch (e) { return false; }
            """)
    private static native boolean nMaster(double v);

    @JSBody(script = """
            try { if (window.__wt) { var p = window.__wt.ctx.suspend(); if (p && p.catch) p.catch(function () {}); } return true; }
            catch (e) { return false; }
            """)
    private static native boolean nSuspend();

    @JSBody(script = """
            try { if (window.__wt && window.__wt.ctx.state === 'suspended') { var p = window.__wt.ctx.resume(); if (p && p.catch) p.catch(function () {}); } return true; }
            catch (e) { return false; }
            """)
    private static native boolean nResume();

    @JSBody(params = { "bus", "wave", "f0", "f1", "t", "dur", "gain", "atk", "rel", "cutoff",
            "pan" }, script = """
                    try {
                      var w = window.__wt; if (!w) return false;
                      if (!isFinite(f0) || !isFinite(f1) || !isFinite(t) || !isFinite(dur) || !isFinite(gain)
                          || !isFinite(atk) || !isFinite(rel) || !isFinite(cutoff) || !isFinite(pan)) return false;
                      if (f0 <= 0 || dur <= 0 || gain < 0 || !w.buses[bus]) return false;
                      var types = ['sine', 'triangle', 'sawtooth', 'square'];
                      if (!types[wave]) return false;
                      var c = w.ctx;
                      var now = c.currentTime;
                      if (t < now) t = now;
                      atk = Math.max(0, Math.min(atk, dur));
                      rel = Math.max(0, rel);
                      pan = Math.max(-1, Math.min(1, pan));
                      var o = c.createOscillator();
                      o.type = types[wave];
                      o.frequency.setValueAtTime(f0, t);
                      if (f1 > 0 && f1 !== f0) o.frequency.exponentialRampToValueAtTime(f1, t + dur);
                      var g = c.createGain();
                      g.gain.setValueAtTime(0.0001, t);
                      g.gain.linearRampToValueAtTime(Math.max(gain, 0.0001), t + atk);
                      var hold = Math.max(t + atk, t + dur - rel);
                      g.gain.setValueAtTime(Math.max(gain, 0.0001), hold);
                      g.gain.linearRampToValueAtTime(0.0001, t + dur);
                      var node = o;
                      var filt = null;
                      if (cutoff > 0) { filt = c.createBiquadFilter(); filt.type = 'lowpass'; filt.frequency.value = cutoff; o.connect(filt); node = filt; }
                      node.connect(g);
                      var out = g;
                      var pn = null;
                      if (pan !== 0 && c.createStereoPanner) { pn = c.createStereoPanner(); pn.pan.value = pan; g.connect(pn); out = pn; }
                      out.connect(w.buses[bus]);
                      o.onended = function () { try { o.disconnect(); if (filt) filt.disconnect(); g.disconnect(); if (pn) pn.disconnect(); } catch (e2) {} };
                      o.start(t);
                      o.stop(t + dur + 0.05);
                      return true;
                    } catch (e) { return false; }
                    """)
    private static native boolean nOsc(int bus, int wave, double f0, double f1, double t, double dur,
            double gain, double atk, double rel, double cutoff, double pan);

    @JSBody(params = { "bus", "filter", "f0", "f1", "q", "t", "dur", "gain", "atk", "rel",
            "pan" }, script = """
                    try {
                      var w = window.__wt; if (!w) return false;
                      if (!isFinite(f0) || !isFinite(f1) || !isFinite(q) || !isFinite(t) || !isFinite(dur) || !isFinite(gain)
                          || !isFinite(atk) || !isFinite(rel) || !isFinite(pan)) return false;
                      if (f0 <= 0 || dur <= 0 || gain < 0 || !w.buses[bus]) return false;
                      var ftypes = ['lowpass', 'bandpass', 'highpass'];
                      if (!ftypes[filter]) return false;
                      var c = w.ctx;
                      var now = c.currentTime;
                      if (t < now) t = now;
                      atk = Math.max(0, Math.min(atk, dur));
                      rel = Math.max(0, rel);
                      pan = Math.max(-1, Math.min(1, pan));
                      var s = c.createBufferSource();
                      s.buffer = w.noise;
                      s.loop = true;
                      var f = c.createBiquadFilter();
                      f.type = ftypes[filter];
                      f.Q.value = q;
                      f.frequency.setValueAtTime(f0, t);
                      if (f1 > 0 && f1 !== f0) f.frequency.exponentialRampToValueAtTime(f1, t + dur);
                      var g = c.createGain();
                      g.gain.setValueAtTime(0.0001, t);
                      g.gain.linearRampToValueAtTime(Math.max(gain, 0.0001), t + atk);
                      var hold = Math.max(t + atk, t + dur - rel);
                      g.gain.setValueAtTime(Math.max(gain, 0.0001), hold);
                      g.gain.linearRampToValueAtTime(0.0001, t + dur);
                      s.connect(f);
                      f.connect(g);
                      var out = g;
                      var pn = null;
                      if (pan !== 0 && c.createStereoPanner) { pn = c.createStereoPanner(); pn.pan.value = pan; g.connect(pn); out = pn; }
                      out.connect(w.buses[bus]);
                      s.onended = function () { try { s.disconnect(); f.disconnect(); g.disconnect(); if (pn) pn.disconnect(); } catch (e2) {} };
                      s.start(t, Math.random() * 1.5);
                      s.stop(t + dur + 0.05);
                      return true;
                    } catch (e) { return false; }
                    """)
    private static native boolean nNoise(int bus, int filter, double f0, double f1, double q, double t, double dur,
            double gain, double atk, double rel, double pan);

    @JSBody(script = """
            try {
              var w = window.__wt; if (!w) return false;
              if (w.wind) return true;
              var c = w.ctx;
              var s = c.createBufferSource();
              s.buffer = w.noise;
              s.loop = true;
              var bp = c.createBiquadFilter();
              bp.type = 'bandpass';
              bp.frequency.value = 400;
              bp.Q.value = 0.8;
              var g = c.createGain();
              g.gain.value = 0;
              s.connect(bp);
              bp.connect(g);
              g.connect(w.buses[2]);
              s.start();
              w.wind = { bp: bp, g: g };
              return true;
            } catch (e) { return false; }
            """)
    private static native boolean nStartWind();

    @JSBody(params = { "freq", "gain", "sec" }, script = """
            try {
              var w = window.__wt; if (!w || !w.wind) return false;
              if (!isFinite(freq) || !isFinite(gain) || !isFinite(sec) || freq <= 0 || gain < 0) return false;
              var t = w.ctx.currentTime;
              var k = Math.max(0.05, sec / 3);
              w.wind.bp.frequency.setTargetAtTime(freq, t, k);
              w.wind.g.gain.setTargetAtTime(gain, t, k);
              return true;
            } catch (e) { return false; }
            """)
    private static native boolean nSetWind(double freq, double gain, double sec);

    @Override
    public boolean isHealthy() {
        return !dead;
    }

    @Override
    public boolean init() {
        if (dead)
            return false;
        return ok(nInit());
    }

    @Override
    public double now() {
        if (dead)
            return 0.0;
        return nNow();
    }

    @Override
    public void setBusGain(int bus, double gain) {
        if (!dead)
            ok(nBus(bus, gain));
    }

    @Override
    public void setMasterGain(double gain) {
        if (!dead)
            ok(nMaster(gain));
    }

    @Override
    public void suspend() {
        if (!dead)
            ok(nSuspend());
    }

    @Override
    public void resume() {
        if (!dead)
            ok(nResume());
    }

    @Override
    public void osc(int bus, int wave, double f0, double f1, double start, double dur,
            double gain, double attack, double release, double cutoff, double pan) {
        if (!dead)
            ok(nOsc(bus, wave, f0, f1, start, dur, gain, attack, release, cutoff, pan));
    }

    @Override
    public void noise(int bus, int filter, double f0, double f1, double q, double start, double dur,
            double gain, double attack, double release, double pan) {
        if (!dead)
            ok(nNoise(bus, filter, f0, f1, q, start, dur, gain, attack, release, pan));
    }

    @Override
    public void startWind() {
        if (!dead)
            ok(nStartWind());
    }

    @Override
    public void setWind(double freq, double gain, double seconds) {
        if (!dead)
            ok(nSetWind(freq, gain, seconds));
    }
}
