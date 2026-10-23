package wartech.map;

// Procedural terrain, crystal fields, geode nests, fog of war

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.unit.Unit;

public final class GameMap {
    public static final int TILE_SIZE = 64;

    public static final byte TERRAIN_SAND = 0;
    public static final byte TERRAIN_BEDROCK = 1;
    public static final byte TERRAIN_CLIFF = 2;
    public static final byte TERRAIN_RUIN = 3;

    public static final byte GEODE_NONE = 0;
    public static final byte GEODE_DORMANT = 1;
    public static final byte GEODE_BREACHED = 2;

    public static final int INFLUENCE_RADIUS_TILES = 7;

    public final int width;
    public final int height;
    public final int totalTiles;

    public final byte[] terrain;
    public final int[] crystals;
    public final byte[] geode;
    public final boolean[] explored;
    public final boolean[] visible;
    public final boolean[] playerInfluence;
    public final boolean[] enemyInfluence;
    public final boolean[] cliffBarrier;
    public final Building[] buildingGrid;

    public int version = 0;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.totalTiles = width * height;

        this.terrain = new byte[totalTiles];
        this.crystals = new int[totalTiles];
        this.geode = new byte[totalTiles];
        this.explored = new boolean[totalTiles];
        this.visible = new boolean[totalTiles];
        this.playerInfluence = new boolean[totalTiles];
        this.enemyInfluence = new boolean[totalTiles];
        this.cliffBarrier = new boolean[totalTiles];
        this.buildingGrid = new Building[totalTiles];
    }

    public boolean visibleAt(double wx, double wy) {
        int tx = (int) (wx / TILE_SIZE);
        int ty = (int) (wy / TILE_SIZE);
        return inBounds(tx, ty) && visible[index(tx, ty)];
    }

    public boolean exploredAt(double wx, double wy) {
        int tx = (int) (wx / TILE_SIZE);
        int ty = (int) (wy / TILE_SIZE);
        return inBounds(tx, ty) && explored[index(tx, ty)];
    }

    public int index(int tx, int ty) {
        return ty * width + tx;
    }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && tx < width && ty >= 0 && ty < height;
    }

    public void generate(long seed) {
        // Clear all arrays
        for (int i = 0; i < totalTiles; i++) {
            terrain[i] = TERRAIN_SAND;
            crystals[i] = 0;
            geode[i] = GEODE_NONE;
            explored[i] = false;
            visible[i] = false;
            playerInfluence[i] = false;
            enemyInfluence[i] = false;
            cliffBarrier[i] = false;
            buildingGrid[i] = null;
        }

        var rng = new SimpleRng(seed);

        createStartingBasePlateau(6, height - 14, 12, 9, true, rng);
        createStartingBasePlateau(width - 17, 4, 12, 9, false, rng);

        int numRocks = (width == 64) ? 8 : 24;
        for (int r = 0; r < numRocks; r++) {
            int rx = 4 + rng.nextInt(width - 12);
            int ry = 4 + rng.nextInt(height - 12);
            int rw = 4 + rng.nextInt(7);
            int rh = 4 + rng.nextInt(6);
            if (!isBaseArea(rx, ry)) {
                createRockPlateau(rx, ry, rw, rh, rng, false);
            }
        }

        int numRidges = (width == 64) ? 5 : 14;
        for (int c = 0; c < numRidges; c++) {
            int cx = 8 + rng.nextInt(width - 16);
            int cy = 8 + rng.nextInt(height - 16);
            int len = 5 + rng.nextInt(10);
            boolean horizontal = rng.nextBoolean();
            for (int step = 0; step < len; step++) {
                int tx = horizontal ? cx + step : cx;
                int ty = horizontal ? cy : cy + step;
                if (inBounds(tx, ty)) {
                    // Do not block starting base cores
                    if (!isBaseArea(tx, ty)) {
                        terrain[index(tx, ty)] = TERRAIN_CLIFF;
                    }
                }
            }
        }

        int numRuins = (width == 64) ? 2 : 5;
        for (int u = 0; u < numRuins; u++) {
            int ux = (width / 4) + rng.nextInt(width / 2);
            int uy = (height / 4) + rng.nextInt(height / 2);
            for (int dy = 0; dy < 3; dy++) {
                for (int dx = 0; dx < 3; dx++) {
                    int tx = ux + dx;
                    int ty = uy + dy;
                    if (inBounds(tx, ty) && !isBaseArea(tx, ty)) {
                        terrain[index(tx, ty)] = TERRAIN_RUIN;
                    }
                }
            }
        }

        int pSector = rng.nextInt(3);
        int pcx = (pSector == 0) ? (18 + rng.nextInt(5)) : ((pSector == 1) ? (15 + rng.nextInt(5)) : (8 + rng.nextInt(7)));
        int pcy = (pSector == 0) ? (height - 14 + rng.nextInt(7)) : ((pSector == 1) ? (height - 21 + rng.nextInt(6)) : (height - 22 + rng.nextInt(5)));
        createCrystalField(pcx, pcy, rng);
        int pAuxX = pcx + (rng.nextBoolean() ? 4 : -4);
        int pAuxY = pcy + (rng.nextBoolean() ? 4 : -4);
        if (inBounds(pAuxX, pAuxY) && !isBaseArea(pAuxX, pAuxY)) {
            createSmallCrystalField(pAuxX, pAuxY, rng);
        }

        int eSector = rng.nextInt(3);
        int ecx = (eSector == 0) ? (width - 19 - rng.nextInt(5)) : ((eSector == 1) ? (width - 16 - rng.nextInt(5)) : (width - 15 + rng.nextInt(7)));
        int ecy = (eSector == 0) ? (5 + rng.nextInt(7)) : ((eSector == 1) ? (13 + rng.nextInt(6)) : (15 + rng.nextInt(5)));
        createCrystalField(ecx, ecy, rng);
        int eAuxX = ecx + (rng.nextBoolean() ? 4 : -4);
        int eAuxY = ecy + (rng.nextBoolean() ? 4 : -4);
        if (inBounds(eAuxX, eAuxY) && !isBaseArea(eAuxX, eAuxY)) {
            createSmallCrystalField(eAuxX, eAuxY, rng);
        }

        int numFields = (width == 64) ? 7 : 18;
        for (int f = 0; f < numFields; f++) {
            int fx = 6 + rng.nextInt(width - 12);
            int fy = 6 + rng.nextInt(height - 12);
            if (!isBaseArea(fx, fy)) {
                createCrystalField(fx, fy, rng);
            }
        }

        rebuildCliffBarrier();
    }

    public void rebuildCliffBarrier() {
        for (int i = 0; i < totalTiles; i++) {
            cliffBarrier[i] = false;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (terrain[index(x, y)] == TERRAIN_CLIFF) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (inBounds(nx, ny) && terrain[index(nx, ny)] != TERRAIN_CLIFF) {
                                cliffBarrier[index(nx, ny)] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isBaseArea(int tx, int ty) {
        if (tx < 20 && ty > height - 20) return true;
        // Enemy base: [width-20..width, 0..20]
        if (tx > width - 20 && ty < 20) return true;
        return false;
    }

    private void createStartingBasePlateau(int coreX0, int coreY0, int coreW, int coreH, boolean isPlayer, SimpleRng rng) {
        for (int y = coreY0; y < coreY0 + coreH; y++) {
            for (int x = coreX0; x < coreX0 + coreW; x++) {
                if (inBounds(x, y)) {
                    terrain[index(x, y)] = TERRAIN_BEDROCK;
                }
            }
        }

        if (isPlayer) {
            int numEastLobes = 2 + rng.nextInt(3);
            for (int i = 0; i < numEastLobes; i++) {
                int ly = coreY0 + rng.nextInt(coreH);
                int reach = 2 + rng.nextInt(5);
                int thickness = 1 + rng.nextInt(2);
                for (int ty = ly; ty < ly + thickness && ty < coreY0 + coreH; ty++) {
                    for (int step = 0; step < reach; step++) {
                        int tx = coreX0 + coreW + step;
                        if (inBounds(tx, ty)) {
                            terrain[index(tx, ty)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }

            int numNorthLobes = 2 + rng.nextInt(3);
            for (int i = 0; i < numNorthLobes; i++) {
                int lx = coreX0 + rng.nextInt(coreW);
                int reach = 2 + rng.nextInt(5);
                int thickness = 1 + rng.nextInt(2);
                for (int tx = lx; tx < lx + thickness && tx < coreX0 + coreW; tx++) {
                    for (int step = 1; step <= reach; step++) {
                        int ty = coreY0 - step;
                        if (inBounds(tx, ty)) {
                            terrain[index(tx, ty)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }

            for (int y = coreY0; y < height; y++) {
                for (int x = 0; x < coreX0; x++) {
                    if (rng.nextInt(100) < 65) {
                        terrain[index(x, y)] = TERRAIN_BEDROCK;
                    }
                }
            }
            for (int y = coreY0 + coreH; y < height; y++) {
                for (int x = coreX0; x < coreX0 + coreW + 3; x++) {
                    if (inBounds(x, y) && rng.nextInt(100) < 65) {
                        terrain[index(x, y)] = TERRAIN_BEDROCK;
                    }
                }
            }

            int numOutcrops = 2 + rng.nextInt(3);
            for (int o = 0; o < numOutcrops; o++) {
                int ox = coreX0 + coreW + 2 + rng.nextInt(4);
                int oy = coreY0 - 2 - rng.nextInt(4);
                int ow = 2 + rng.nextInt(2);
                int oh = 2 + rng.nextInt(2);
                for (int sy = oy; sy < oy + oh; sy++) {
                    for (int sx = ox; sx < ox + ow; sx++) {
                        if (inBounds(sx, sy)) {
                            terrain[index(sx, sy)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }
        } else {
            int numWestLobes = 2 + rng.nextInt(3);
            for (int i = 0; i < numWestLobes; i++) {
                int ly = coreY0 + rng.nextInt(coreH);
                int reach = 2 + rng.nextInt(5);
                int thickness = 1 + rng.nextInt(2);
                for (int ty = ly; ty < ly + thickness && ty < coreY0 + coreH; ty++) {
                    for (int step = 1; step <= reach; step++) {
                        int tx = coreX0 - step;
                        if (inBounds(tx, ty)) {
                            terrain[index(tx, ty)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }

            int numSouthLobes = 2 + rng.nextInt(3);
            for (int i = 0; i < numSouthLobes; i++) {
                int lx = coreX0 + rng.nextInt(coreW);
                int reach = 2 + rng.nextInt(5);
                int thickness = 1 + rng.nextInt(2);
                for (int tx = lx; tx < lx + thickness && tx < coreX0 + coreW; tx++) {
                    for (int step = 0; step < reach; step++) {
                        int ty = coreY0 + coreH + step;
                        if (inBounds(tx, ty)) {
                            terrain[index(tx, ty)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }

            for (int y = 0; y < coreY0; y++) {
                for (int x = coreX0 - 2; x < width; x++) {
                    if (inBounds(x, y) && rng.nextInt(100) < 65) {
                        terrain[index(x, y)] = TERRAIN_BEDROCK;
                    }
                }
            }
            for (int y = coreY0; y < coreY0 + coreH; y++) {
                for (int x = coreX0 + coreW; x < width; x++) {
                    if (inBounds(x, y) && rng.nextInt(100) < 65) {
                        terrain[index(x, y)] = TERRAIN_BEDROCK;
                    }
                }
            }

            int numOutcrops = 2 + rng.nextInt(3);
            for (int o = 0; o < numOutcrops; o++) {
                int ox = coreX0 - 4 - rng.nextInt(4);
                int oy = coreY0 + coreH + 2 + rng.nextInt(4);
                int ow = 2 + rng.nextInt(2);
                int oh = 2 + rng.nextInt(2);
                for (int sy = oy; sy < oy + oh; sy++) {
                    for (int sx = ox; sx < ox + ow; sx++) {
                        if (inBounds(sx, sy)) {
                            terrain[index(sx, sy)] = TERRAIN_BEDROCK;
                        }
                    }
                }
            }
        }
    }

    private void createRockPlateau(int x0, int y0, int w, int h, SimpleRng rng, boolean solid) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if (!inBounds(x, y)) continue;
                boolean border = (x == x0 || y == y0 || x == x0 + w - 1 || y == y0 + h - 1);
                if (!solid && border && rng.nextInt(100) < 30) continue;
                terrain[index(x, y)] = TERRAIN_BEDROCK;
            }
        }
        if (!solid) {
            for (int s = 0; s < 4; s++) {
                int sx = x0 - 2 + rng.nextInt(w + 4);
                int sy = rng.nextBoolean() ? y0 - rng.nextInt(3) - 1 : y0 + h + rng.nextInt(3);
                if (inBounds(sx, sy) && terrain[index(sx, sy)] == TERRAIN_SAND) {
                    terrain[index(sx, sy)] = TERRAIN_BEDROCK;
                }
            }
        }
    }

    private void createCrystalField(int cx, int cy, SimpleRng rng) {
        int radius = 3 + rng.nextInt(3);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tx = cx + dx;
                int ty = cy + dy;
                if (inBounds(tx, ty) && terrain[index(tx, ty)] == TERRAIN_SAND) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radius) {
                        int idx = index(tx, ty);
                        int amt = (int) (400 * (1.0 - (dist / (radius + 0.5)))) + 50;
                        crystals[idx] = Math.max(crystals[idx], amt);
                    }
                }
            }
        }

        if (inBounds(cx, cy)) {
            int cidx = index(cx, cy);
            if (crystals[cidx] > 0) {
                geode[cidx] = GEODE_DORMANT;
            } else {
                int best = -1;
                int bestDist = Integer.MAX_VALUE;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int tx = cx + dx;
                        int ty = cy + dy;
                        if (!inBounds(tx, ty)) continue;
                        int idx = index(tx, ty);
                        if (crystals[idx] <= 0) continue;
                        int d = dx * dx + dy * dy;
                        if (d < bestDist) {
                            bestDist = d;
                            best = idx;
                        }
                    }
                }
                if (best >= 0) {
                    geode[best] = GEODE_DORMANT;
                }
            }
        }
    }

    private void createSmallCrystalField(int cx, int cy, SimpleRng rng) {
        int radius = 2 + rng.nextInt(2);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tx = cx + dx;
                int ty = cy + dy;
                if (inBounds(tx, ty) && terrain[index(tx, ty)] == TERRAIN_SAND) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radius) {
                        int idx = index(tx, ty);
                        int amt = (int) (250 * (1.0 - (dist / (radius + 0.5)))) + 30;
                        crystals[idx] = Math.max(crystals[idx], amt);
                    }
                }
            }
        }
    }

    public boolean canBuildAt(int tx, int ty, int w, int h, boolean isPlayer,
                              Building[] buildings, int buildingCount) {
        return buildRejection(tx, ty, w, h, isPlayer, buildings, buildingCount, false) == PlacementError.NONE;
    }

    public boolean canBuildAt(int tx, int ty, BuildingType type, boolean isPlayer,
                              Building[] buildings, int buildingCount) {
        return canBuildAt(tx, ty, type, isPlayer, buildings, buildingCount, null, 0);
    }

    public boolean canBuildAt(int tx, int ty, BuildingType type, boolean isPlayer,
                              Building[] buildings, int buildingCount,
                              Unit[] units, int unitCount) {
        return buildRejection(tx, ty, type.widthTiles, type.heightTiles, isPlayer,
            buildings, buildingCount, type.deploysUnitsAtExit(), units, unitCount) == PlacementError.NONE;
    }

    public PlacementError buildRejection(int tx, int ty, BuildingType type, boolean isPlayer,
                              Building[] buildings, int buildingCount) {
        return buildRejection(tx, ty, type, isPlayer, buildings, buildingCount, null, 0);
    }

    /** Rejection reason including a live-unit occupancy test. */
    public PlacementError buildRejection(int tx, int ty, BuildingType type, boolean isPlayer,
                              Building[] buildings, int buildingCount,
                              Unit[] units, int unitCount) {
        return buildRejection(tx, ty, type.widthTiles, type.heightTiles, isPlayer,
            buildings, buildingCount, type.deploysUnitsAtExit(), units, unitCount);
    }

    public PlacementError buildRejection(int tx, int ty, int w, int h, boolean isPlayer,
                              Building[] buildings, int buildingCount, boolean requireExit) {
        return buildRejection(tx, ty, w, h, isPlayer, buildings, buildingCount, requireExit, null, 0);
    }

    public PlacementError buildRejection(int tx, int ty, int w, int h, boolean isPlayer,
                              Building[] buildings, int buildingCount, boolean requireExit,
                              Unit[] units, int unitCount) {
        for (int y = ty; y < ty + h; y++) {
            for (int x = tx; x < tx + w; x++) {
                if (!inBounds(x, y)) return PlacementError.OUT_OF_BOUNDS;
                int idx = index(x, y);
                if (terrain[idx] != TERRAIN_BEDROCK) return PlacementError.NOT_BEDROCK;
                if (cliffBarrier[idx]) return PlacementError.CLIFF_BUFFER;
                boolean hasInf = isPlayer ? playerInfluence[idx] : enemyInfluence[idx];
                if (!hasInf) return PlacementError.NO_INFLUENCE;
            }
        }

        for (int y = ty - 1; y <= ty + h; y++) {
            for (int x = tx - 1; x <= tx + w; x++) {
                if (!inBounds(x, y)) continue;
                Building b = buildingGrid[index(x, y)];
                if (b != null && !b.isDestroyed()) return PlacementError.TOO_CLOSE;
            }
        }

        if (buildings != null) {
            for (int i = 0; i < buildingCount; i++) {
                Building b = buildings[i];
                if (b == null || b.isDestroyed()) continue;
                boolean overlapX = Math.max(tx - 1, b.tileX) <= Math.min(tx + w, b.tileX + b.type.widthTiles - 1);
                boolean overlapY = Math.max(ty - 1, b.tileY) <= Math.min(ty + h, b.tileY + b.type.heightTiles - 1);
                if (overlapX && overlapY) return PlacementError.TOO_CLOSE;
            }
        }

        if (requireExit) {
            int exitY = ty + h;
            if (exitY >= height) return PlacementError.NO_EXIT;
            for (int x = tx; x < tx + w; x++) {
                if (!inBounds(x, exitY)) return PlacementError.NO_EXIT;
                int idx = index(x, exitY);
                if (terrain[idx] == TERRAIN_CLIFF) return PlacementError.NO_EXIT;
                Building b = buildingGrid[idx];
                if (b != null && !b.isDestroyed()) return PlacementError.NO_EXIT;
            }
        }

        if (units != null) {
            double minX = tx * TILE_SIZE;
            double minY = ty * TILE_SIZE;
            double maxX = (tx + w) * TILE_SIZE;
            double maxY = (ty + h) * TILE_SIZE;
            for (int i = 0; i < unitCount; i++) {
                Unit u = units[i];
                if (u == null || u.isDestroyed()) continue;
                if (u.x >= minX && u.x < maxX && u.y >= minY && u.y < maxY) {
                    return PlacementError.UNIT_OCCUPIED;
                }
            }
        }

        return PlacementError.NONE;
    }

    public void updateInfluence(Building[] buildings, int count) {
        version++;
        for (int i = 0; i < totalTiles; i++) {
            playerInfluence[i] = false;
            enemyInfluence[i] = false;
            buildingGrid[i] = null;
        }

        for (int bIdx = 0; bIdx < count; bIdx++) {
            var b = buildings[bIdx];
            if (b.isDestroyed()) continue;

            for (int ty = b.tileY; ty < b.tileY + b.type.heightTiles; ty++) {
                for (int tx = b.tileX; tx < b.tileX + b.type.widthTiles; tx++) {
                    if (inBounds(tx, ty)) {
                        buildingGrid[index(tx, ty)] = b;
                    }
                }
            }

            boolean isP = (b.faction == Faction.PLAYER);
            int cx = b.centerTileX();
            int cy = b.centerTileY();

            int x0 = Math.max(0, cx - INFLUENCE_RADIUS_TILES);
            int x1 = Math.min(width - 1, cx + INFLUENCE_RADIUS_TILES);
            int y0 = Math.max(0, cy - INFLUENCE_RADIUS_TILES);
            int y1 = Math.min(height - 1, cy + INFLUENCE_RADIUS_TILES);

            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    int distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                    if (distSq <= INFLUENCE_RADIUS_TILES * INFLUENCE_RADIUS_TILES) {
                        int idx = index(x, y);
                        if (isP) playerInfluence[idx] = true;
                        else enemyInfluence[idx] = true;
                    }
                }
            }
        }
    }

    public boolean isTileBlocked(int tx, int ty, Building allowedBuilding) {
        if (!inBounds(tx, ty)) return true;
        int idx = index(tx, ty);
        if (terrain[idx] == TERRAIN_CLIFF) return true;
        Building b = buildingGrid[idx];
        if (b != null && !b.isDestroyed() && b != allowedBuilding) return true;
        return false;
    }

    public boolean blocksShot(int tx, int ty, Building allowedBuilding) {
        if (!inBounds(tx, ty)) return true;
        int idx = index(tx, ty);
        if (terrain[idx] == TERRAIN_CLIFF) return true;
        Building b = buildingGrid[idx];
        return b != null && !b.isDestroyed() && b != allowedBuilding;
    }

    public boolean hasLineOfSight(double ax, double ay, double bx, double by, Building allowed) {
        if (!isFinite(ax) || !isFinite(ay) || !isFinite(bx) || !isFinite(by)) return false;
        int tx = (int) Math.floor(ax / TILE_SIZE);
        int ty = (int) Math.floor(ay / TILE_SIZE);
        int endX = (int) Math.floor(bx / TILE_SIZE);
        int endY = (int) Math.floor(by / TILE_SIZE);
        if (!inBounds(tx, ty) || !inBounds(endX, endY)) return false;
        if (tx == endX && ty == endY) return true;

        double dx = bx - ax;
        double dy = by - ay;
        int stepX = dx > 0.0 ? 1 : (dx < 0.0 ? -1 : 0);
        int stepY = dy > 0.0 ? 1 : (dy < 0.0 ? -1 : 0);
        double tDeltaX = stepX != 0 ? Math.abs(TILE_SIZE / dx) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? Math.abs(TILE_SIZE / dy) : Double.MAX_VALUE;
        double tMaxX = stepX > 0 ? ((tx + 1) * TILE_SIZE - ax) / dx
                    : (stepX < 0 ? (tx * TILE_SIZE - ax) / dx : Double.MAX_VALUE);
        double tMaxY = stepY > 0 ? ((ty + 1) * TILE_SIZE - ay) / dy
                    : (stepY < 0 ? (ty * TILE_SIZE - ay) / dy : Double.MAX_VALUE);

        // Bounded by the longest possible staircase across the map.
        int guard = width + height + 4;
        while (guard-- > 0) {
            if (tMaxX < tMaxY) {
                if (tMaxX > 1.0) return true;
                tx += stepX;
                tMaxX += tDeltaX;
            } else {
                if (tMaxY > 1.0) return true;
                ty += stepY;
                tMaxY += tDeltaY;
            }
            if (tx == endX && ty == endY) return true;
            if (blocksShot(tx, ty, allowed)) return false;
        }
        return true;
    }

    private static boolean isFinite(double v) {
        return v > -1.0e12 && v < 1.0e12;
    }

    public void revealCircle(int cx, int cy, int radius) {
        int x0 = Math.max(0, cx - radius);
        int x1 = Math.min(width - 1, cx + radius);
        int y0 = Math.max(0, cy - radius);
        int y1 = Math.min(height - 1, cy + radius);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if (distSq <= radius * radius) {
                    int idx = index(x, y);
                    explored[idx] = true;
                    visible[idx] = true;
                }
            }
        }
    }

    public void resetVisibility() {
        for (int i = 0; i < totalTiles; i++) {
            visible[i] = false;
        }
    }

    public static final class SimpleRng {
        private long state;

        public SimpleRng(long seed) {
            this.state = seed != 0 ? seed : 123456789L;
        }

        public int nextInt(int bound) {
            if (bound <= 0) return 0;
            state = (state * 6364136223846793005L + 1442695040888963407L);
            long val = (state >>> 32) & 0x7FFFFFFFL;
            return (int) (val % bound);
        }

        public boolean nextBoolean() {
            return nextInt(2) == 0;
        }
    }
}
