package wartech.bridge;

// TeaVM entry point and browser bridge.
// Owns the canvas, input bindings, frame loop, DOM HUD, and campaign
// persistence. Talks to the simulation through WarTech.wasm_*.

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.AnimationFrameCallback;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Performance;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.Touch;
import org.teavm.jso.dom.events.TouchEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import wartech.audio.AudioEngine;
import wartech.audio.AudioSettings;
import wartech.audio.WebAudioBackend;
import wartech.campaign.CampaignSave;
import wartech.core.Display;
import wartech.core.KeyCode;
import wartech.core.MatchState;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.render.AssetData;

public final class BrowserApp {
    private static HTMLCanvasElement canvas;
    private static CanvasRenderingContext2D ctx;
    private static HTMLDocument document;
    private static HTMLElement fpsEl;
    private static HTMLElement creditsEl;
    private static HTMLElement powerEl;
    private static HTMLElement zoomEl;
    private static HTMLElement versionEl;
    private static HTMLElement btnRestart;
    private static HTMLElement btnZoomIn;
    private static HTMLElement btnZoomOut;

    private static double lastTime;
    private static int frameCount = 0;
    private static double lastFpsTime;
    private static boolean loopErrorLogged = false;

    private static double rectLeft;
    private static double rectTop;
    private static double rectWidth = 1;
    private static double rectHeight = 1;
    private static boolean rectValid = false;

    private static int scratchX;
    private static int scratchY;

    private static final int NO_TOUCH = -1;
    private static final int LONG_PRESS_MS = 500;
    private static final int LONG_PRESS_SLOP = 8;
    private static final int SECOND_TAP_MS = 300;

    private static final int WORLD_VIEW_WIDTH = Display.VIEW_WIDTH;
    private static int touchId = NO_TOUCH;
    private static int touchStartX;
    private static int touchStartY;
    private static boolean touchMoved;
    private static int longPressTimerId;
    private static boolean longPressFired;
    private static boolean multiTouch;
    private static int secondId = NO_TOUCH;
    private static int secondX;
    private static int secondY;
    private static double secondAt;
    private static boolean secondMoved;

    private static double wheelAccum = 0;
    private static int lastSavedCampaignState = -1;

    @JSBody(script = "return document.getElementById('game').getBoundingClientRect().left;")
    private static native double canvasRectLeft();

    @JSBody(script = "return document.getElementById('game').getBoundingClientRect().top;")
    private static native double canvasRectTop();

    @JSBody(script = "return document.getElementById('game').getBoundingClientRect().width;")
    private static native double canvasRectWidth();

    @JSBody(script = "return document.getElementById('game').getBoundingClientRect().height;")
    private static native double canvasRectHeight();

    @JSBody(script = "return document.hidden;")
    private static native boolean documentHidden();

    @JSBody(params = {"id", "e"}, script = "var el = document.getElementById(id); return !!el && el.contains(e.target);")
    private static native boolean eventInside(String id, JSObject e);

    @JSBody(params = "msg", script = "console.error(msg);")
    private static native void consoleError(String msg);

    @JSBody(params = "v", script = "window.__wartechReady = v;")
    private static native void setReadyFlag(boolean v);

    public static void main(String[] args) {
        document = HTMLDocument.current();
        canvas = (HTMLCanvasElement) document.getElementById("game");
        if (canvas == null) return;
        canvas.setWidth(WarTech.WIDTH);
        canvas.setHeight(WarTech.HEIGHT);
        ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
        if (ctx == null) return;
        Surface.bind(ctx);
        fpsEl = document.getElementById("fps");
        creditsEl = document.getElementById("credits");
        powerEl = document.getElementById("power");
        zoomEl = document.getElementById("zoomDisplay");
        btnRestart = document.getElementById("btnRestart");
        btnZoomIn = document.getElementById("btnZoomIn");
        btnZoomOut = document.getElementById("btnZoomOut");
        versionEl = document.getElementById("version");

        try {
            AssetData.init();
            WarTech.wasm_init(0);
        } catch (RuntimeException e) {
            drawBootError(e);
            return;
        }
        loadSavedCampaign();
        bindButtons();
        try {
            initAudio();
        } catch (RuntimeException | Error e) {
            // Sound is optional: a failure here must never stop the game from starting.
            consoleError("Audio disabled: " + e);
        }
        bindMouse();
        bindTouch();
        bindWheel();
        bindKeys();
        bindWindowEvents();
        updateHUD();
        if (versionEl != null) versionEl.setInnerText(WarTech.VERSION);
        setReadyFlag(true);
        lastTime = Performance.now();
        lastFpsTime = lastTime;
        Window.requestAnimationFrame(new FrameLoop());
    }

    private static class FrameLoop implements AnimationFrameCallback {
        @Override
        public void onAnimationFrame(double timestamp) {
            try {
                double now = Performance.now();
                double dt = (now - lastTime) / 1000.0;
                lastTime = now;
                WarTech.wasm_update(dt);
                AudioEngine.tick(dt);
                autoSaveCampaign();
                frameCount++;
                if (now - lastFpsTime >= 500) {
                    int fps = (int) Math.round(frameCount * 1000.0 / (now - lastFpsTime));
                    if (fpsEl != null) fpsEl.setInnerText(String.valueOf(fps));
                    frameCount = 0;
                    lastFpsTime = now;
                    updateHUD();
                }
            } catch (RuntimeException e) {
                if (!loopErrorLogged) {
                    loopErrorLogged = true;
                    consoleError("Frame loop error (loop continues): " + e);
                }
                drawFrameErrorBanner();
            } finally {
                Window.requestAnimationFrame(this);
            }
        }
    }

    private static void updateHUD() {
        if (creditsEl != null) creditsEl.setInnerText(String.valueOf(WarTech.wasm_get_credits()));
        if (powerEl != null) {
            int p = WarTech.wasm_get_power();
            powerEl.setInnerText((p >= 0 ? "+" : "") + p);
            powerEl.getStyle().setProperty("color", p >= 0 ? "var(--green)" : "var(--red)");
        }
        if (zoomEl != null) zoomEl.setInnerText(WarTech.wasm_get_zoom() + "%");
    }

    private static void drawBootError(RuntimeException e) {
        consoleError("Failed to initialize WarTech engine: " + e);
        ctx.setFillStyle("#1e1e1e");
        ctx.fillRect(0, 0, WarTech.WIDTH, WarTech.HEIGHT);
        ctx.setFillStyle("#ff6b6b");
        ctx.setFont("18px 'JetBrains Mono', monospace");
        ctx.fillText("Error loading WarTech engine: " + e.getMessage(), 40, 80);
    }

    private static void drawFrameErrorBanner() {
        try {
            ctx.setFillStyle("rgba(0, 0, 0, 0.75)");
            ctx.fillRect(0, 0, WarTech.WIDTH, 34);
            ctx.setFillStyle("#ff6b6b");
            ctx.setFont("16px 'JetBrains Mono', monospace");
            ctx.fillText("Render error - see console (game loop still running)", 20, 23);
        } catch (RuntimeException ignored) {
            // The error banner itself must never break the loop.
        }
    }

    private static void toCanvasCoords(double clientX, double clientY) {
        if (!rectValid) {
            rectLeft = canvasRectLeft();
            rectTop = canvasRectTop();
            rectWidth = canvasRectWidth();
            rectHeight = canvasRectHeight();
            rectValid = true;
        }
        double scaleX = WarTech.WIDTH / rectWidth;
        double scaleY = WarTech.HEIGHT / rectHeight;
        int px = (int) Math.floor((clientX - rectLeft) * scaleX);
        int py = (int) Math.floor((clientY - rectTop) * scaleY);
        scratchX = Math.max(0, Math.min(WarTech.WIDTH - 1, px));
        scratchY = Math.max(0, Math.min(WarTech.HEIGHT - 1, py));
    }

    private static void bindButtons() {
        if (btnRestart != null) {
            btnRestart.onClick(_ -> {
                WarTech.wasm_restart_match();
                btnRestart.blur();
                updateHUD();
            });
        }
        if (btnZoomIn != null) {
            btnZoomIn.onClick(_ -> {
                WarTech.wasm_zoom_in();
                btnZoomIn.blur();
                updateHUD();
            });
        }
        if (btnZoomOut != null) {
            btnZoomOut.onClick(_ -> {
                WarTech.wasm_zoom_out();
                btnZoomOut.blur();
                updateHUD();
            });
        }
    }

    private static void bindMouse() {
        canvas.onMouseDown(e -> {
            toCanvasCoords(e.getClientX(), e.getClientY());
            WarTech.wasm_mouse_down(scratchX, scratchY, e.getButton());
            updateHUD();
        });

        Window.current().onMouseUp(e -> {
            toCanvasCoords(e.getClientX(), e.getClientY());
            WarTech.wasm_mouse_up(scratchX, scratchY, e.getButton());
            updateHUD();
        });
        canvas.onMouseMove(e -> {
            toCanvasCoords(e.getClientX(), e.getClientY());
            WarTech.wasm_mouse_move(scratchX, scratchY);
        });
        canvas.addEventListener("contextmenu", (Event e) -> e.preventDefault());
        canvas.onMouseLeave(e -> {
            if (e.getButtons() == 0) releaseAllInput();
        });
    }

    private static void clearLongPress() {
        if (longPressTimerId != 0) {
            Window.clearTimeout(longPressTimerId);
            longPressTimerId = 0;
        }
    }

    private static void resetTouchState() {
        clearLongPress();
        touchId = NO_TOUCH;
        longPressFired = false;
        touchMoved = false;
        multiTouch = false;
        secondId = NO_TOUCH;
        secondMoved = false;
    }

    private static void rightClickAt(int x, int y) {
        WarTech.wasm_mouse_down(x, y, 2);
        WarTech.wasm_mouse_up(x, y, 2);
        updateHUD();
    }

    private static void bindTouch() {
        canvas.onTouchStart(e -> {
            e.preventDefault();
            var changed = e.getChangedTouches();
            for (int i = 0; i < changed.getLength(); i++) {
                Touch t = changed.get(i);
                toCanvasCoords(t.getClientX(), t.getClientY());
                int x = scratchX;
                int y = scratchY;
                if (touchId != NO_TOUCH) {
                    if (secondId == NO_TOUCH) {
                        secondId = t.getIdentifier();
                        secondX = x;
                        secondY = y;
                        secondAt = Performance.now();
                        secondMoved = false;
                        clearLongPress();
                    }
                    continue;
                }
                touchId = t.getIdentifier();
                longPressFired = false;
                multiTouch = false;
                touchMoved = false;
                secondId = NO_TOUCH;
                touchStartX = x;
                touchStartY = y;
                WarTech.wasm_mouse_down(x, y, 0);
                clearLongPress();

                if (x >= WORLD_VIEW_WIDTH) continue;
                longPressTimerId = Window.setTimeout(() -> {
                    longPressTimerId = 0;
                    if (touchMoved || multiTouch) return;

                    WarTech.wasm_release_input();
                    rightClickAt(touchStartX, touchStartY);
                    longPressFired = true;
                }, LONG_PRESS_MS);
            }
        });
        canvas.onTouchMove(e -> {
            var changed = e.getChangedTouches();
            for (int i = 0; i < changed.getLength(); i++) {
                Touch t = changed.get(i);
                toCanvasCoords(t.getClientX(), t.getClientY());
                int x = scratchX;
                int y = scratchY;
                if (t.getIdentifier() == secondId) {
                    if (Math.abs(x - secondX) > LONG_PRESS_SLOP || Math.abs(y - secondY) > LONG_PRESS_SLOP) {
                        secondMoved = true;
                    }
                    continue;
                }
                if (t.getIdentifier() != touchId) continue;
                if (Math.abs(x - touchStartX) > LONG_PRESS_SLOP || Math.abs(y - touchStartY) > LONG_PRESS_SLOP) {
                    touchMoved = true;
                    clearLongPress();
                }
                WarTech.wasm_mouse_move(x, y);
            }
        });
        EventListener<TouchEvent> endTouch = e -> {
            var changed = e.getChangedTouches();
            for (int i = 0; i < changed.getLength(); i++) {
                Touch t = changed.get(i);
                int id = t.getIdentifier();
                if (id == secondId) {
                    boolean quickTap = !secondMoved && (Performance.now() - secondAt) <= SECOND_TAP_MS;
                    secondId = NO_TOUCH;
                    if (!quickTap) continue;

                    multiTouch = true;
                    clearLongPress();
                    WarTech.wasm_release_input();
                    rightClickAt(secondX, secondY);
                    continue;
                }
                if (id != touchId) continue;
                clearLongPress();
                if (!longPressFired && !multiTouch) {

                    toCanvasCoords(t.getClientX(), t.getClientY());
                    WarTech.wasm_mouse_up(scratchX, scratchY, 0);
                }
                resetTouchState();
                updateHUD();
            }
        };
        canvas.onTouchEnd(endTouch);
        canvas.onTouchCancel(endTouch);

        Window.current().onTouchEnd(endTouch);
        Window.current().onTouchCancel(endTouch);
    }

    private static void bindWheel() {
        canvas.onWheel(e -> {
            e.preventDefault();
            int mode = e.getDeltaMode();
            if (mode == WheelEvent.DOM_DELTA_LINE || mode == WheelEvent.DOM_DELTA_PAGE) {
                if (e.getDeltaY() < 0) WarTech.wasm_zoom_in();
                else if (e.getDeltaY() > 0) WarTech.wasm_zoom_out();
                wheelAccum = 0;
                updateHUD();
                return;
            }
            wheelAccum += e.getDeltaY();
            while (wheelAccum <= -100) {
                wheelAccum += 100;
                WarTech.wasm_zoom_in();
            }
            while (wheelAccum >= 100) {
                wheelAccum -= 100;
                WarTech.wasm_zoom_out();
            }
            updateHUD();
        });
    }

    private static int mapKey(String code) {
        return switch (code) {
            case "ArrowLeft" -> KeyCode.LEFT;
            case "ArrowUp" -> KeyCode.UP;
            case "ArrowRight" -> KeyCode.RIGHT;
            case "ArrowDown" -> KeyCode.DOWN;
            case "Enter" -> KeyCode.ENTER;
            case "Escape" -> KeyCode.ESCAPE;
            case "Space" -> KeyCode.SPACE;
            case "KeyA" -> KeyCode.A;
            case "KeyD" -> KeyCode.D;
            case "KeyO" -> KeyCode.O;
            case "KeyS" -> KeyCode.S;
            case "KeyW" -> KeyCode.W;
            case "ControlLeft", "ControlRight" -> KeyCode.CONTROL;
            case "Digit0", "Numpad0" -> KeyCode.DIGIT0;
            case "Digit1", "Numpad1" -> KeyCode.DIGIT1;
            case "Digit2", "Numpad2" -> KeyCode.DIGIT2;
            case "Digit3", "Numpad3" -> KeyCode.DIGIT3;
            case "Digit4", "Numpad4" -> KeyCode.DIGIT4;
            case "Digit5", "Numpad5" -> KeyCode.DIGIT5;
            case "Digit6", "Numpad6" -> KeyCode.DIGIT6;
            case "Digit7", "Numpad7" -> KeyCode.DIGIT7;
            case "Digit8", "Numpad8" -> KeyCode.DIGIT8;
            case "Digit9", "Numpad9" -> KeyCode.DIGIT9;
            case "Equal" -> KeyCode.EQUALS;
            case "Minus" -> KeyCode.MINUS;
            case "NumpadAdd" -> KeyCode.NUMPAD_ADD;
            case "NumpadSubtract" -> KeyCode.NUMPAD_SUBTRACT;
            case "BracketLeft" -> KeyCode.BRACKET_LEFT;
            case "BracketRight" -> KeyCode.BRACKET_RIGHT;
            default -> KeyCode.UNKNOWN;
        };
    }

    private static boolean focusInControl() {
        HTMLElement active = document.getActiveElement();
        if (active == null) return false;
        return switch (active.getTagName()) {
            case "BUTTON", "INPUT", "SELECT", "TEXTAREA" -> true;
            default -> false;
        };
    }

    private static void bindKeys() {
        Window.current().onKeyDown(e -> {
            if (focusInControl()) return;
            String code = e.getCode();
            if ("Space".equals(code) || code.startsWith("Arrow")) e.preventDefault();
            if (e.isRepeat()) return;
            int key = mapKey(code);
            if (key < 0) return;
            if (e.isCtrlKey() && key >= KeyCode.DIGIT0 && key <= KeyCode.DIGIT9) e.preventDefault();
            WarTech.wasm_key_down(key);
            updateHUD();
        });
        Window.current().onKeyUp(e -> {
            if (focusInControl()) return;
            int key = mapKey(e.getCode());
            if (key < 0) return;
            WarTech.wasm_key_up(key);
        });
    }

    private static void releaseAllInput() {
        resetTouchState();
        WarTech.wasm_release_input();
    }

    private static final String KEY_VOL_MUSIC = "wartech.vol.music";
    private static final String KEY_VOL_SFX = "wartech.vol.sfx";
    private static final String KEY_VOL_AMBIENCE = "wartech.vol.ambience";
    private static final String KEY_MUTED = "wartech.muted";

    private static HTMLElement btnSound;
    private static HTMLElement soundPanel;
    private static boolean audioUnlockBound = false;

    private static Storage safeStorage() {
        try {
            return Storage.getLocalStorage();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void saveSetting(String key, String value) {
        try {
            Storage storage = safeStorage();
            if (storage != null) storage.setItem(key, value);
        } catch (RuntimeException ignored) {
            // Storage unavailable (private mode, quota); volumes just won't persist.
        }
    }

    private static String loadSetting(String key) {
        try {
            Storage storage = safeStorage();
            return storage == null ? null : storage.getItem(key);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static HTMLInputElement input(String id) {
        return (HTMLInputElement) document.getElementById(id);
    }

    private static void initAudio() {
        AudioEngine.install(new WebAudioBackend(), (long) Performance.now() ^ 0x5DEECE66DL);
        AudioEngine audio = AudioEngine.get();

        audio.setMusicVolume(AudioSettings.parse(loadSetting(KEY_VOL_MUSIC), AudioSettings.DEFAULT_MUSIC));
        audio.setSfxVolume(AudioSettings.parse(loadSetting(KEY_VOL_SFX), AudioSettings.DEFAULT_SFX));
        audio.setAmbienceVolume(AudioSettings.parse(loadSetting(KEY_VOL_AMBIENCE), AudioSettings.DEFAULT_AMBIENCE));
        audio.setMuted("1".equals(loadSetting(KEY_MUTED)));

        btnSound = document.getElementById("btnSound");
        soundPanel = document.getElementById("soundPanel");

        bindSlider("volMusic", "volMusicVal", audio.settings().music, v -> {
            audio.setMusicVolume(v);
            saveSetting(KEY_VOL_MUSIC, String.valueOf(v));
        });
        bindSlider("volSfx", "volSfxVal", audio.settings().sfx, v -> {
            audio.setSfxVolume(v);
            saveSetting(KEY_VOL_SFX, String.valueOf(v));
        });
        bindSlider("volAmbience", "volAmbienceVal", audio.settings().ambience, v -> {
            audio.setAmbienceVolume(v);
            saveSetting(KEY_VOL_AMBIENCE, String.valueOf(v));
        });

        HTMLInputElement mute = input("soundMute");
        if (mute != null) {
            mute.setChecked(audio.settings().muted);
            mute.addEventListener("change", (Event _) -> {
                audio.setMuted(mute.isChecked());
                saveSetting(KEY_MUTED, mute.isChecked() ? "1" : "0");
                updateSoundIcon();
                mute.blur();
            });
        }
        updateSoundIcon();

        if (btnSound != null && soundPanel != null) {
            btnSound.onClick(_ -> {
                boolean open = !soundPanel.getClassList().contains("open");
                if (open) soundPanel.getClassList().add("open"); else soundPanel.getClassList().remove("open");
                btnSound.setAttribute("aria-expanded", String.valueOf(open));
                btnSound.blur();
            });
            Window.current().addEventListener("pointerdown", (Event e) -> {
                if (soundPanel.getClassList().contains("open")
                    && !eventInside("soundPanel", e) && !eventInside("btnSound", e)) {
                    soundPanel.getClassList().remove("open");
                    btnSound.setAttribute("aria-expanded", "false");
                }
            });
        }

        // Browsers only allow audio after a user gesture.
        if (!audioUnlockBound) {
            audioUnlockBound = true;
            Window window = Window.current();
            window.addEventListener("pointerdown", (Event _) -> audio.unlock());
            window.addEventListener("keydown", (Event _) -> audio.unlock());
        }
    }

    private interface VolumeSink {
        void set(double v);
    }

    private static void bindSlider(String sliderId, String valueId, double initial, VolumeSink sink) {
        HTMLInputElement slider = input(sliderId);
        HTMLElement label = document.getElementById(valueId);
        if (slider == null) return;
        int pct = (int) Math.round(initial * 100.0);
        slider.setValue(String.valueOf(pct));
        if (label != null) label.setInnerText(String.valueOf(pct));
        slider.addEventListener("input", (Event _) -> {
            double v = AudioSettings.fromPercent(slider.getValue());
            if (label != null) label.setInnerText(String.valueOf((int) Math.round(v * 100.0)));
            sink.set(v);
        });
    }

    private static void updateSoundIcon() {
        if (btnSound == null) return;
        AudioEngine audio = AudioEngine.get();
        boolean muted = audio != null && audio.settings().muted;
        btnSound.setInnerHTML(muted ? "&#128263;" : "&#128266;");
    }

    private static void bindWindowEvents() {
        Window window = Window.current();
        window.addEventListener("blur", (Event _) -> releaseAllInput());
        document.addEventListener("visibilitychange", (Event _) -> {
            if (documentHidden()) releaseAllInput();
            AudioEngine audio = AudioEngine.get();
            if (audio != null) audio.setSuspended(documentHidden());
        });
        window.addEventListener("resize", (Event _) -> rectValid = false);
        window.addEventListener("scroll", (Event _) -> rectValid = false, true);
    }

    private static void loadSavedCampaign() {
        try {
            Storage storage = Storage.getLocalStorage();
            if (storage == null) return;
            String search = Location.current().getSearch();
            boolean forceReset = CampaignSave.hasQueryFlag(search, "reset_campaign")
                || CampaignSave.hasQueryFlag(search, "clearsave");
            String savedVersion = storage.getItem(CampaignSave.VERSION_STAMP_KEY);
            if (CampaignSave.shouldDiscardSave(savedVersion, forceReset)) {
                storage.removeItem(CampaignSave.SAVE_KEY);
                storage.setItem(CampaignSave.VERSION_STAMP_KEY, CampaignSave.CURRENT_VERSION);
                return;
            }
            int save = CampaignSave.parseSave(storage.getItem(CampaignSave.SAVE_KEY));
            if (save >= 0) WarTech.wasm_load_campaign_save(save);
        } catch (RuntimeException _) {
            // localStorage not accessible, not sure what we should do about it yet
            // maybe an alert or something
        }
    }

    private static void autoSaveCampaign() {
        try {
            Storage storage = Storage.getLocalStorage();
            if (storage == null) return;
            if (!CampaignSave.shouldPersist(WarTech.wasm_is_campaign_active() == 1,
                    MatchState.fromCode(WarTech.wasm_get_match_state()))) return;
            int save = WarTech.wasm_get_campaign_save();
            if (save != lastSavedCampaignState) {
                lastSavedCampaignState = save;
                storage.setItem(CampaignSave.SAVE_KEY, String.valueOf(save));
                storage.setItem(CampaignSave.VERSION_STAMP_KEY, CampaignSave.CURRENT_VERSION);
            }
        } catch (RuntimeException _) {
            // ignore for now, I don't really log things properly
        }
    }
}
