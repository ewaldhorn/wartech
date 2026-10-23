package wartech.render;

// Colours are stored in AABBGGRR format, litte-endian because that's how wasm rolls

public final class Palette {
    private Palette() {
    }

    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY_LIGHT = 0xFFE0E0E0;
    public static final int GRAY = 0xFFB0B0B0;
    public static final int GRAY_DARK = 0xFF202020;

    public static final int GRAY_DISABLED = 0xFF706050;
    public static final int GREEN = 0xFF00FF70;
    public static final int GREEN_BRIGHT = 0xFF40FF70;

    public static final int GREEN_PURE = 0xFF00FF00;

    public static final int GREEN_DONE = 0xFF4ADE80;
    public static final int RED = 0xFF3030FF;

    public static final int TEXT_FAIL = 0xFFF87171;

    public static final int GOLD = 0xFF00D4FF;
    public static final int GOLD_LIGHT = 0xFF00E5FF;
    public static final int GOLD_DARK = 0xFF00C8E8;

    public static final int YELLOW_PALE = 0xFFE0FFFF;
    public static final int BLUE_SKY = 0xFFFFD040;
    public static final int BLUE_LIGHT = 0xFFE0B040;
    public static final int BLUE_PALE = 0xFFE0C060;

    public static final int BLUE_SOFT = 0xFFFCD34D;

    public static final int TAN = 0xFF38BDF8;

    
    public static final int BLACK = 0xFF000000;
    public static final int BLACK_EDGE = 0xFF140E0A;
    
    public static final int BLACK_PANEL = 0xFF100C08;
    
    public static final int BLACK_SHADOW = 0xFF0A0806;
    
    public static final int PANEL_DARK = 0xFF241C14;
    
    public static final int BROWN_DARK = 0xFF1E293B;
    
    public static final int BROWN_BUTTON = 0xFF2A1E14;
    
    public static final int BROWN_HOVER = 0xFF503525;
    
    public static final int BROWN_BORDER = 0xFF655038;
    
    public static final int SLATE_DARK = 0xFF584838;
    
    public static final int UMBER = 0xFF403424;
    
    public static final int ORANGE_DEEP = 0xFF0284C7;
}
