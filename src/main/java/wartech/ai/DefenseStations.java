package wartech.ai;

// Defender stations and fallback base positions

import wartech.building.Building;
import wartech.map.GameMap;

public final class DefenseStations {
    private DefenseStations() {}

    // Base centre X is the Construction Yard or else the assumed spot near the east edge.
    public static double baseX(Building con, GameMap map) {
        return con != null ? con.getCenterX() : (map.width - AiTuning.FALLBACK_BASE_FROM_EAST_TILES) * GameMap.TILE_SIZE;
    }

    // Base centre Y is the Construction Yard, else the assumed row near the top.
    public static double baseY(Building con) {
        return con != null ? con.getCenterY() : AiTuning.FALLBACK_BASE_ROW_TILES * GameMap.TILE_SIZE;
    }

    public static double stationX(int slot, double bx, double by, Building ref, Building con, GameMap map) {
        int s = slot % AiTuning.DEF_STATION_SLOTS;
        double x = bx + AiTuning.DEF_STATION_DX[s];
        if (s == AiTuning.DEF_SLOT_REFINERY_GUARD && ref != null) x = ref.getCenterX() + AiTuning.DEF_REFINERY_DX;
        if (s == AiTuning.DEF_SLOT_CON_YARD_GUARD && con != null) x = con.getCenterX() + AiTuning.DEF_CON_YARD_DX;
        return Math.max(GameMap.TILE_SIZE * AiTuning.STATION_EDGE_MARGIN_TILES,
                Math.min((map.width - AiTuning.STATION_EDGE_MARGIN_TILES) * GameMap.TILE_SIZE, x));
    }

    public static double stationY(int slot, double bx, double by, Building ref, Building con, GameMap map) {
        int s = slot % AiTuning.DEF_STATION_SLOTS;
        double y = by + AiTuning.DEF_STATION_DY[s];
        if (s == AiTuning.DEF_SLOT_REFINERY_GUARD && ref != null) y = ref.getCenterY() + AiTuning.DEF_REFINERY_DY;
        if (s == AiTuning.DEF_SLOT_CON_YARD_GUARD && con != null) y = con.getCenterY() + AiTuning.DEF_CON_YARD_DY;
        return Math.max(GameMap.TILE_SIZE * AiTuning.STATION_EDGE_MARGIN_TILES,
                Math.min((map.height - AiTuning.STATION_EDGE_MARGIN_TILES) * GameMap.TILE_SIZE, y));
    }

    // Nudge a station off cliffs and buildings to the first free neighbouring tile
    public static double resolveClearX(double sx, double sy, GameMap map) {
        int tx = (int) (sx / GameMap.TILE_SIZE);
        int ty = (int) (sy / GameMap.TILE_SIZE);
        if (isBlocked(tx, ty, map)) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ntx = tx + dx;
                    int nty = ty + dy;
                    if (!isBlocked(ntx, nty, map)) {
                        return (ntx + 0.5) * GameMap.TILE_SIZE;
                    }
                }
            }
        }
        return sx;
    }

    public static double resolveClearY(double sx, double sy, GameMap map) {
        int tx = (int) (sx / GameMap.TILE_SIZE);
        int ty = (int) (sy / GameMap.TILE_SIZE);
        if (isBlocked(tx, ty, map)) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ntx = tx + dx;
                    int nty = ty + dy;
                    if (!isBlocked(ntx, nty, map)) {
                        return (nty + 0.5) * GameMap.TILE_SIZE;
                    }
                }
            }
        }
        return sy;
    }

    // If it's a cliff or already occupied, can't build there
    private static boolean isBlocked(int tx, int ty, GameMap map) {
        return !map.inBounds(tx, ty)
            || map.terrain[map.index(tx, ty)] == GameMap.TERRAIN_CLIFF
            || map.buildingGrid[map.index(tx, ty)] != null;
    }
}
