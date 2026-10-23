package wartech.render;

// Loaded from assets_data.js via Base64 to bypass javac 64KB bytecode limits.
// I really need to move this into some kind of loader on the JS side for PNG files eventually.

import java.util.Base64;
import org.teavm.jso.JSBody;

public final class AssetData {
    public static final int TILE_SIZE = 64;

    public static final int[] SAND_0 = new int[4096];
    public static final int[] SAND_1 = new int[4096];
    public static final int[] SAND_2 = new int[4096];
    public static final int[] SAND_3 = new int[4096];
    public static final int[] BEDROCK_0 = new int[4096];
    public static final int[] BEDROCK_1 = new int[4096];
    public static final int[] BEDROCK_2 = new int[4096];
    public static final int[] BEDROCK_3 = new int[4096];
    public static final int[] CLIFF_0 = new int[4096];
    public static final int[] CLIFF_1 = new int[4096];
    public static final int[] CLIFF_2 = new int[4096];
    public static final int[] CLIFF_3 = new int[4096];
    public static final int[] CRYSTAL_SPARSE = new int[4096];
    public static final int[] CRYSTAL_RICH = new int[4096];
    public static final int[] GEODE_DORMANT = new int[4096];
    public static final int[] GEODE_BREACHED = new int[4096];
    public static final int[] PRECURSOR_RUIN = new int[4096];
    public static final int[] WRECKAGE = new int[4096];
    public static final int[] UNIT_COLLECTOR = new int[4096];
    public static final int[] UNIT_SCOUT_TRIKE = new int[4096];
    public static final int[] UNIT_COMBAT_TANK = new int[4096];
    public static final int[] UNIT_SIEGE_TANK = new int[4096];
    public static final int[] UNIT_TROOPER_RIFLE = new int[4096];
    public static final int[] UNIT_TROOPER_MISSILE = new int[4096];
    public static final int[] UNIT_SHARD_SKITTERER = new int[4096];
    public static final int[] UNIT_SCAVENGER_DRONE = new int[4096];
    public static final int[] UNIT_PRECURSOR_WALKER = new int[4096];
    public static final int[] UNIT_REAVER_BUGGY = new int[4096];
    public static final int[] UNIT_RAVAGER_TANK = new int[4096];
    public static final int[] UNIT_DREADNOUGHT = new int[4096];
    public static final int[] UNIT_RAIDER = new int[4096];
    public static final int[] UNIT_VANDAL = new int[4096];
    public static final int[] UNIT_MECHANIC_VANGUARD = new int[4096];
    public static final int[] UNIT_MECHANIC_SYNDICATE = new int[4096];
    public static final int[] UNIT_MEDIC_VANGUARD = new int[4096];
    public static final int[] UNIT_MEDIC_SYNDICATE = new int[4096];
    public static final int[] BLDG_WIND_TRAP = new int[16384];
    public static final int[] BLDG_REFINERY = new int[24576];
    public static final int[] BLDG_BARRACKS = new int[16384];
    public static final int[] BLDG_LIGHT_FACTORY = new int[24576];
    public static final int[] BLDG_HEAVY_FACTORY = new int[24576];
    public static final int[] BLDG_REPAIR_VANGUARD = new int[24576];
    public static final int[] BLDG_REPAIR_SYNDICATE = new int[24576];
    public static final int[] BLDG_RADAR = new int[16384];
    public static final int[] BLDG_TURRET = new int[4096];
    public static final int[] BLDG_CON_YARD = new int[36864];
    public static final int[] COAST0_M01 = new int[4096];
    public static final int[] COAST0_M02 = new int[4096];
    public static final int[] COAST0_M03 = new int[4096];
    public static final int[] COAST0_M04 = new int[4096];
    public static final int[] COAST0_M05 = new int[4096];
    public static final int[] COAST0_M06 = new int[4096];
    public static final int[] COAST0_M07 = new int[4096];
    public static final int[] COAST0_M08 = new int[4096];
    public static final int[] COAST0_M09 = new int[4096];
    public static final int[] COAST0_M10 = new int[4096];
    public static final int[] COAST0_M11 = new int[4096];
    public static final int[] COAST0_M12 = new int[4096];
    public static final int[] COAST0_M13 = new int[4096];
    public static final int[] COAST0_M14 = new int[4096];
    public static final int[] COAST0_M15 = new int[4096];
    public static final int[] COAST1_M01 = new int[4096];
    public static final int[] COAST1_M02 = new int[4096];
    public static final int[] COAST1_M03 = new int[4096];
    public static final int[] COAST1_M04 = new int[4096];
    public static final int[] COAST1_M05 = new int[4096];
    public static final int[] COAST1_M06 = new int[4096];
    public static final int[] COAST1_M07 = new int[4096];
    public static final int[] COAST1_M08 = new int[4096];
    public static final int[] COAST1_M09 = new int[4096];
    public static final int[] COAST1_M10 = new int[4096];
    public static final int[] COAST1_M11 = new int[4096];
    public static final int[] COAST1_M12 = new int[4096];
    public static final int[] COAST1_M13 = new int[4096];
    public static final int[] COAST1_M14 = new int[4096];
    public static final int[] COAST1_M15 = new int[4096];
    public static final int[] CORN0_NE = new int[4096];
    public static final int[] CORN0_SE = new int[4096];
    public static final int[] CORN0_SW = new int[4096];
    public static final int[] CORN0_NW = new int[4096];
    public static final int[] CORN1_NE = new int[4096];
    public static final int[] CORN1_SE = new int[4096];
    public static final int[] CORN1_SW = new int[4096];
    public static final int[] CORN1_NW = new int[4096];

    // Palette helpers for dynamic UI and minimap
    public static final int COLOR_FOG_UNEXPLORED = 0xFF0A0806;
    public static final int COLOR_CRYSTAL_MINIMAP= 0xFF60FFA0; // ABGR
    public static final int COLOR_RUIN_MINIMAP   = 0xFFE0E000; // ABGR
    public static final int COLOR_ROCK_MINIMAP   = 0xFF404C5A; // ABGR
    public static final int COLOR_SAND_MINIMAP   = 0xFF6AB0D4; // ABGR

    @JSBody(script = "return globalThis.__assetsB64 || '';")
    private static native String fetchAssetsB64();

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            String b64 = fetchAssetsB64();
            if (b64 != null && b64.length() > 0) {
                byte[] bytes = Base64.getDecoder().decode(b64);
                loadFromBytes(bytes);
            }
        } catch (Throwable t) {
            System.err.println("AssetData init failed: " + t);
        }
    }

    private static void loadFromBytes(byte[] b) {
        int o = 0;
        for (int i = 0; i < 4096; i++) {
            SAND_0[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            SAND_1[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            SAND_2[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            SAND_3[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            BEDROCK_0[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            BEDROCK_1[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            BEDROCK_2[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            BEDROCK_3[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CLIFF_0[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CLIFF_1[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CLIFF_2[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CLIFF_3[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CRYSTAL_SPARSE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CRYSTAL_RICH[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            GEODE_DORMANT[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            GEODE_BREACHED[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            PRECURSOR_RUIN[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            WRECKAGE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_COLLECTOR[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_SCOUT_TRIKE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_COMBAT_TANK[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_SIEGE_TANK[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_TROOPER_RIFLE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_TROOPER_MISSILE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_SHARD_SKITTERER[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_SCAVENGER_DRONE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_PRECURSOR_WALKER[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_REAVER_BUGGY[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_RAVAGER_TANK[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_DREADNOUGHT[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_RAIDER[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_VANDAL[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_MECHANIC_VANGUARD[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_MECHANIC_SYNDICATE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_MEDIC_VANGUARD[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            UNIT_MEDIC_SYNDICATE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 16384; i++) {
            BLDG_WIND_TRAP[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 24576; i++) {
            BLDG_REFINERY[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 16384; i++) {
            BLDG_BARRACKS[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 24576; i++) {
            BLDG_LIGHT_FACTORY[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 24576; i++) {
            BLDG_HEAVY_FACTORY[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 24576; i++) {
            BLDG_REPAIR_VANGUARD[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 24576; i++) {
            BLDG_REPAIR_SYNDICATE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 16384; i++) {
            BLDG_RADAR[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            BLDG_TURRET[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 36864; i++) {
            BLDG_CON_YARD[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M01[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M02[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M03[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M04[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M05[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M06[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M07[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M08[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M09[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M10[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M11[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M12[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M13[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M14[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST0_M15[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M01[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M02[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M03[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M04[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M05[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M06[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M07[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M08[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M09[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M10[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M11[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M12[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M13[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M14[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            COAST1_M15[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN0_NE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN0_SE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN0_SW[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN0_NW[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN1_NE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN1_SE[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN1_SW[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
        for (int i = 0; i < 4096; i++) {
            CORN1_NW[i] = (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
            o += 4;
        }
    }
}
