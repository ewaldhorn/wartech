package wartech.core;

// Framebuffer geometry

public final class Display {
    private Display() {
    }

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 768;

    public static final int VIEW_WIDTH = 704;
    public static final int SIDEBAR_WIDTH = WIDTH - VIEW_WIDTH;
    public static final int SIDEBAR_X = VIEW_WIDTH;
}
