package wartech.core;

// Faction name and colour definitions

public enum Faction {
    PLAYER(0xFFFF6020, "Vanguard"),       // Little-endian ABGR (Blue)
    ENEMY(0xFF2020E0, "Crimson Syndicate"), // Little-endian ABGR (Red)
    NEUTRAL(0xFF30F080, "Wilderness");    // Green/Amber

    public final int primaryColor;
    public final String name;

    Faction(int primaryColor, String name) {
        this.primaryColor = primaryColor;
        this.name = name;
    }
}
