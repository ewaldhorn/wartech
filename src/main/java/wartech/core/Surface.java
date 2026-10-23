package wartech.core;

import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public final class Surface {
    public final int base;
    public final int width;
    public final int height;
    public final int byteSize;

    private static CanvasRenderingContext2D ctx;
    private static ImageData frame;
    private static Uint32Array frameWords;

    public Surface(int base, int width, int height) {
        this.base = base;
        this.width = width;
        this.height = height;
        this.byteSize = width * height * 4;
    }

    public static void bind(CanvasRenderingContext2D context) {
        ctx = context;
        frame = null;
        frameWords = null;
    }

    private void ensureFrame() {
        if (frame == null && ctx != null) {
            ArrayBuffer buf = new ArrayBuffer(byteSize);
            frame = new ImageData(new Uint8ClampedArray(buf), width, height);
            frameWords = new Uint32Array(buf);
        }
    }

    public void present() {
        if (ctx == null) return;
        ensureFrame();
        frameWords.set(Memory.HEAP);
        ctx.putImageData(frame, 0, 0);
    }

    public void setPixel(int x, int y, int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            Memory.put32(base + (y * width + x) * 4, color);
        }
    }

    public void clear(int color) {
        Memory.fill32(base, width * height, color);
    }

    public void fillRect(int x, int y, int w, int h, int color) {
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(width, x + w);
        int y1 = Math.min(height, y + h);
        int span = x1 - x0;
        int spanH = y1 - y0;
        if (span <= 0 || spanH <= 0) return;

        for (int row = y0; row < y1; row++) {
            Memory.fill32(base + (row * width + x0) * 4, span, color);
        }
    }

    public void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        int cx = x0;
        int cy = y0;
        while (true) {
            if (cx >= 0 && cx < width && cy >= 0 && cy < height) {
                Memory.put32(base + (cy * width + cx) * 4, color);
            }
            if (cx == x1 && cy == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; cx += sx; }
            if (e2 <= dx) { err += dx; cy += sy; }
        }
    }
}
