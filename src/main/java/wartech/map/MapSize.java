package wartech.map;

public enum MapSize {
    SMALL_64(64),
    LARGE_128(128);

    public static final int CODE_SMALL_64 = 0;
    public static final int CODE_LARGE_128 = 1;

    public final int tiles;
    public final int worldPx;

    MapSize(int tiles) {
        this.tiles = tiles;
        this.worldPx = tiles * GameMap.TILE_SIZE;
    }

    public static MapSize fromCode(int code) {
        return switch (code) {
            case CODE_LARGE_128 -> LARGE_128;
            default -> SMALL_64;
        };
    }
}
