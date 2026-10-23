package wartech.core;

// Viewport coordinate management, zoom scaling, scrolling

public final class Camera {
    public static final int VIEW_WIDTH = Display.VIEW_WIDTH;
    public static final int VIEW_HEIGHT = Display.HEIGHT;
    public static final double PAN_SPEED = 1200.0; // px/sec (18.75 tiles/sec)

    public static final double ZOOM_MIN     = 0.20; // 20%
    public static final double ZOOM_DEFAULT = 0.50; // 50% Default
    public static final double ZOOM_MAX     = 1.00; // 100% Real Size
    public static final double ZOOM_STEP    = 0.10; // 10% increments

    private static final int MIN_STEP = (int) Math.round(ZOOM_MIN / ZOOM_STEP);
    private static final int MAX_STEP = (int) Math.round(ZOOM_MAX / ZOOM_STEP);
    private static final double STEPS_PER_UNIT = Math.round(1.0 / ZOOM_STEP);

    private static int stepOf(double zoom) {
        return (int) Math.round(zoom * STEPS_PER_UNIT);
    }

    public double zoom = ZOOM_DEFAULT;
    public double x;
    public double y;
    public int worldWidthPx;
    public int worldHeightPx;
    public int maxWorldX;
    public int maxWorldY;

    public Camera(int worldWidthPx, int worldHeightPx) {
        this.worldWidthPx = worldWidthPx;
        this.worldHeightPx = worldHeightPx;
        this.x = 0;
        this.y = 0;
        updateWorldBounds();
    }

    public double getWorldViewWidth() {
        return VIEW_WIDTH / zoom;
    }

    public double getWorldViewHeight() {
        return VIEW_HEIGHT / zoom;
    }

    private void updateWorldBounds() {
        this.maxWorldX = Math.max(0, (int) (worldWidthPx - getWorldViewWidth()));
        this.maxWorldY = Math.max(0, (int) (worldHeightPx - getWorldViewHeight()));
        clamp();
    }

    public void clamp() {
        x = Math.clamp(x, 0.0, maxWorldX);
        y = Math.clamp(y, 0.0, maxWorldY);
    }

    public void pan(double dx, double dy) {
        x += dx;
        y += dy;
        clamp();
    }

    public void centerOn(double worldX, double worldY) {
        x = worldX - (getWorldViewWidth() / 2.0);
        y = worldY - (getWorldViewHeight() / 2.0);
        clamp();
    }

    public void zoomIn() {
        zoomIn(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
    }

    public void zoomOut() {
        zoomOut(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
    }

    public void zoomIn(double screenAnchorX, double screenAnchorY) {
        setZoomAround(screenAnchorX, screenAnchorY,
            Math.min(MAX_STEP, stepOf(zoom) + 1) / STEPS_PER_UNIT);
    }

    public void zoomOut(double screenAnchorX, double screenAnchorY) {
        setZoomAround(screenAnchorX, screenAnchorY,
            Math.max(MIN_STEP, stepOf(zoom) - 1) / STEPS_PER_UNIT);
    }

    public void setZoom(double newZoom) {
        setZoomAround(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, newZoom);
    }

    public void setZoomAround(double screenAnchorX, double screenAnchorY, double newZoom) {
        if (newZoom < ZOOM_MIN) newZoom = ZOOM_MIN;
        if (newZoom > ZOOM_MAX) newZoom = ZOOM_MAX;
        int step = Math.clamp(stepOf(newZoom), MIN_STEP, MAX_STEP);
        newZoom = step / STEPS_PER_UNIT;
        if (Math.abs(this.zoom - newZoom) < 0.001) return;

        double worldAnchorX = screenToWorldX(screenAnchorX);
        double worldAnchorY = screenToWorldY(screenAnchorY);

        this.zoom = newZoom;
        this.x = worldAnchorX - (screenAnchorX / newZoom);
        this.y = worldAnchorY - (screenAnchorY / newZoom);
        updateWorldBounds();
    }

    public int getZoomPercent() {
        return (int) Math.round(zoom * 100.0);
    }

    public double screenToWorldX(double screenX) {
        return (screenX / zoom) + x;
    }

    public double screenToWorldY(double screenY) {
        return (screenY / zoom) + y;
    }

    public int worldToScreenX(double worldX) {
        return (int) ((worldX - x) * zoom);
    }

    public int worldToScreenY(double worldY) {
        return (int) ((worldY - y) * zoom);
    }
}

