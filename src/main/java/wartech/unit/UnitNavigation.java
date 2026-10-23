package wartech.unit;

// Obstacle queries, BFS pathfinding, local steering, and obstacle collision logic
// Stole a lot from my https://nofuss.co.za/toys/amazer/ project to be honest, just ported to Java,
// so it's pretty messy but it seems to work alright-ish. I have to revisit this, I feel like it's
// gotten way too clunky with all the little nuances added for some units like medics etc. Also
// the yielding code is meh, it works most times but it can also be really dumb.

import wartech.building.Building;
import wartech.map.GameMap;

public final class UnitNavigation {

    public static final class ContactPoint {
        public double x;
        public double y;
        public boolean hit;

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
            this.hit = true;
        }

        public void reset() {
            this.x = 0.0;
            this.y = 0.0;
            this.hit = false;
        }
    }

    public static final ContactPoint sharedContact = new ContactPoint();

    private static final int[] bfsPrev = new int[128 * 128];
    private static final int[] bfsSeen = new int[128 * 128];
    private static final int[] bfsQueue = new int[128 * 128];
    private static int bfsStamp = 0;

    private static final int MAX_PATHS_PER_FRAME = 24;
    private static final int RESERVED_COMBAT_PATHS = 6;
    private static int pathBudget = MAX_PATHS_PER_FRAME;

    public static void resetPathBudget() {
        pathBudget = MAX_PATHS_PER_FRAME;
    }

    private static boolean takePathBudget(boolean highPriority) {
        if (pathBudget <= 0) return false;
        if (!highPriority && pathBudget <= RESERVED_COMBAT_PATHS) return false;
        pathBudget--;
        return true;
    }

    private static final int[] BFS_DX = { 1, -1, 0, 0, 1, 1, -1, -1 };
    private static final int[] BFS_DY = { 0, 0, 1, -1, 1, -1, 1, -1 };

    private UnitNavigation() {}

    public static boolean inWorldBounds(double px, double py, double radius, GameMap map) {
        return px >= radius && px <= map.width * GameMap.TILE_SIZE - radius &&
               py >= radius && py <= map.height * GameMap.TILE_SIZE - radius;
    }

    public static boolean collidesWithObstacle(double px, double py, double clearance, GameMap map, Building ignoredBuilding) {
        int minTx = Math.max(0, (int) ((px - clearance) / GameMap.TILE_SIZE));
        int maxTx = Math.min(map.width - 1, (int) ((px + clearance) / GameMap.TILE_SIZE));
        int minTy = Math.max(0, (int) ((py - clearance) / GameMap.TILE_SIZE));
        int maxTy = Math.min(map.height - 1, (int) ((py + clearance) / GameMap.TILE_SIZE));

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int idx = map.index(tx, ty);
                if (map.terrain[idx] == GameMap.TERRAIN_CLIFF) {
                    double nearX = Math.clamp(px, tx * GameMap.TILE_SIZE, (tx + 1) * GameMap.TILE_SIZE);
                    double nearY = Math.clamp(py, ty * GameMap.TILE_SIZE, (ty + 1) * GameMap.TILE_SIZE);
                    double cdx = px - nearX;
                    double cdy = py - nearY;
                    if (cdx * cdx + cdy * cdy < clearance * clearance) {
                        return true;
                    }
                }
                Building b = map.buildingGrid[idx];
                if (b != null && !b.isDestroyed() && b != ignoredBuilding) {
                    double bMinX = b.pixelX;
                    double bMaxX = b.pixelX + b.type.widthTiles * GameMap.TILE_SIZE;
                    double bMinY = b.pixelY;
                    double bMaxY = b.pixelY + b.type.heightTiles * GameMap.TILE_SIZE;
                    double nearX = Math.clamp(px, bMinX, bMaxX);
                    double nearY = Math.clamp(py, bMinY, bMaxY);
                    double cdx = px - nearX;
                    double cdy = py - nearY;
                    if (cdx * cdx + cdy * cdy < clearance * clearance) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean findClosestObstacle(double px, double py, double maxDist, GameMap map, Building ignoredBuilding, ContactPoint out) {
        int minTx = Math.max(0, (int) ((px - maxDist) / GameMap.TILE_SIZE));
        int maxTx = Math.min(map.width - 1, (int) ((px + maxDist) / GameMap.TILE_SIZE));
        int minTy = Math.max(0, (int) ((py - maxDist) / GameMap.TILE_SIZE));
        int maxTy = Math.min(map.height - 1, (int) ((py + maxDist) / GameMap.TILE_SIZE));

        double bestDistSq = maxDist * maxDist;
        boolean found = false;

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int idx = map.index(tx, ty);
                if (map.terrain[idx] == GameMap.TERRAIN_CLIFF) {
                    double nearX = Math.clamp(px, tx * GameMap.TILE_SIZE, (tx + 1) * GameMap.TILE_SIZE);
                    double nearY = Math.clamp(py, ty * GameMap.TILE_SIZE, (ty + 1) * GameMap.TILE_SIZE);
                    double cdx = px - nearX;
                    double cdy = py - nearY;
                    double dSq = cdx * cdx + cdy * cdy;
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        if (out != null) out.set(nearX, nearY);
                        found = true;
                    }
                }
                Building b = map.buildingGrid[idx];
                if (b != null && !b.isDestroyed() && b != ignoredBuilding) {
                    double bMinX = b.pixelX;
                    double bMaxX = b.pixelX + b.type.widthTiles * GameMap.TILE_SIZE;
                    double bMinY = b.pixelY;
                    double bMaxY = b.pixelY + b.type.heightTiles * GameMap.TILE_SIZE;
                    double nearX = Math.clamp(px, bMinX, bMaxX);
                    double nearY = Math.clamp(py, bMinY, bMaxY);
                    double cdx = px - nearX;
                    double cdy = py - nearY;
                    double dSq = cdx * cdx + cdy * cdy;
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        if (out != null) out.set(nearX, nearY);
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public static void resolveObstacleOverlap(Unit u, GameMap map, double clearance, Building ignoredBuilding) {
        int minTx = Math.max(0, (int) ((u.x - clearance) / GameMap.TILE_SIZE));
        int maxTx = Math.min(map.width - 1, (int) ((u.x + clearance) / GameMap.TILE_SIZE));
        int minTy = Math.max(0, (int) ((u.y - clearance) / GameMap.TILE_SIZE));
        int maxTy = Math.min(map.height - 1, (int) ((u.y + clearance) / GameMap.TILE_SIZE));

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int idx = map.index(tx, ty);
                if (map.terrain[idx] == GameMap.TERRAIN_CLIFF) {
                    double nearX = Math.clamp(u.x, tx * GameMap.TILE_SIZE, (tx + 1) * GameMap.TILE_SIZE);
                    double nearY = Math.clamp(u.y, ty * GameMap.TILE_SIZE, (ty + 1) * GameMap.TILE_SIZE);
                    double cdx = u.x - nearX;
                    double cdy = u.y - nearY;
                    double distSq = cdx * cdx + cdy * cdy;
                    if (distSq < clearance * clearance) {
                        double dist = Math.sqrt(distSq);
                        if (dist > 0.001) {
                            double overlap = clearance - dist;
                            u.x += (cdx / dist) * overlap;
                            u.y += (cdy / dist) * overlap;
                        } else {
                            double cx = (tx + 0.5) * GameMap.TILE_SIZE;
                            double cy = (ty + 0.5) * GameMap.TILE_SIZE;
                            double ex = u.x - cx;
                            double ey = u.y - cy;
                            if (Math.abs(ex) > Math.abs(ey)) {
                                u.x = (ex >= 0) ? (tx + 1) * GameMap.TILE_SIZE + clearance : tx * GameMap.TILE_SIZE - clearance;
                            } else {
                                u.y = (ey >= 0) ? (ty + 1) * GameMap.TILE_SIZE + clearance : ty * GameMap.TILE_SIZE - clearance;
                            }
                        }
                    }
                }
                Building b = map.buildingGrid[idx];
                if (b != null && !b.isDestroyed() && b != ignoredBuilding) {
                    double bMinX = b.pixelX;
                    double bMaxX = b.pixelX + b.type.widthTiles * GameMap.TILE_SIZE;
                    double bMinY = b.pixelY;
                    double bMaxY = b.pixelY + b.type.heightTiles * GameMap.TILE_SIZE;
                    double nearX = Math.clamp(u.x, bMinX, bMaxX);
                    double nearY = Math.clamp(u.y, bMinY, bMaxY);
                    double cdx = u.x - nearX;
                    double cdy = u.y - nearY;
                    double distSq = cdx * cdx + cdy * cdy;
                    if (distSq < clearance * clearance) {
                        double dist = Math.sqrt(distSq);
                        if (dist > 0.001) {
                            double overlap = clearance - dist;
                            u.x += (cdx / dist) * overlap;
                            u.y += (cdy / dist) * overlap;
                        } else {
                            double leftDist = Math.abs(u.x - bMinX);
                            double rightDist = Math.abs(bMaxX - u.x);
                            double topDist = Math.abs(u.y - bMinY);
                            double bottomDist = Math.abs(bMaxY - u.y);
                            double minEdge = Math.min(Math.min(leftDist, rightDist), Math.min(topDist, bottomDist));
                            if (minEdge == leftDist) {
                                u.x = bMinX - clearance;
                            } else if (minEdge == rightDist) {
                                u.x = bMaxX + clearance;
                            } else if (minEdge == topDist) {
                                u.y = bMinY - clearance;
                            } else {
                                u.y = bMaxY + clearance;
                            }
                        }
                    }
                }
            }
        }
        u.x = Math.clamp(u.x, u.type.radius, map.width * GameMap.TILE_SIZE - u.type.radius);
        u.y = Math.clamp(u.y, u.type.radius, map.height * GameMap.TILE_SIZE - u.type.radius);
    }

    public static boolean computePath(Unit u, GameMap map, int goalTX, int goalTY, Building ignoredBuilding) {
        u.pathLen = 0;
        u.pathIdx = 0;
        u.pathGoalTX = goalTX;
        u.pathGoalTY = goalTY;
        int w = map.width;
        int h = map.height;
        if (goalTX < 0 || goalTY < 0 || goalTX >= w || goalTY >= h) return false;
        int sx = Math.clamp((int) (u.x / GameMap.TILE_SIZE), 0, w - 1);
        int sy = Math.clamp((int) (u.y / GameMap.TILE_SIZE), 0, h - 1);
        if (sx == goalTX && sy == goalTY) return true;

        if (map.isTileBlocked(goalTX, goalTY, ignoredBuilding)) {
            int bestAdjX = -1;
            int bestAdjY = -1;
            int bestDistSq = Integer.MAX_VALUE;
            for (int r = 1; r <= 4; r++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                        int atx = goalTX + dx;
                        int aty = goalTY + dy;
                        if (!map.isTileBlocked(atx, aty, ignoredBuilding)) {
                            int dSq = (atx - sx) * (atx - sx) + (aty - sy) * (aty - sy);
                            if (dSq < bestDistSq) {
                                bestDistSq = dSq;
                                bestAdjX = atx;
                                bestAdjY = aty;
                            }
                        }
                    }
                }
                if (bestAdjX != -1) break;
            }
            if (bestAdjX != -1) {
                goalTX = bestAdjX;
                goalTY = bestAdjY;
            } else {
                return false;
            }
        }

        if (sx == goalTX && sy == goalTY) return true;
        boolean wide = u.isWideChassis();
        if (bfsRoute(u, map, sx, sy, goalTX, goalTY, wide, ignoredBuilding)) return true;
        if (wide) return bfsRoute(u, map, sx, sy, goalTX, goalTY, false, ignoredBuilding);
        return false;
    }

    private static boolean bfsRoute(Unit u, GameMap map, int sx, int sy, int gx, int gy, boolean avoidSqueeze, Building ignoredBuilding) {
        int w = map.width;
        int h = map.height;
        bfsStamp++;
        int head = 0;
        int tail = 0;
        int start = sy * w + sx;
        int goal = gy * w + gx;
        bfsQueue[tail++] = start;
        bfsSeen[start] = bfsStamp;
        bfsPrev[start] = -1;
        boolean found = false;
        while (head < tail) {
            int cur = bfsQueue[head++];
            if (cur == goal) {
                found = true;
                break;
            }
            int cx = cur % w;
            int cy = cur / w;
            for (int d = 0; d < 8; d++) {
                int dx = BFS_DX[d];
                int dy = BFS_DY[d];
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int nidx = ny * w + nx;
                if (bfsSeen[nidx] == bfsStamp) continue;
                if (map.isTileBlocked(nx, ny, ignoredBuilding)) continue;
                if (dx != 0 && dy != 0) {
                    if (map.isTileBlocked(nx, cy, ignoredBuilding)) continue;
                    if (map.isTileBlocked(cx, ny, ignoredBuilding)) continue;
                }
                if (avoidSqueeze) {
                    if (nx > 0 && map.isTileBlocked(nx - 1, ny, ignoredBuilding)) continue;
                    if (nx < w - 1 && map.isTileBlocked(nx + 1, ny, ignoredBuilding)) continue;
                    if (ny > 0 && map.isTileBlocked(nx, ny - 1, ignoredBuilding)) continue;
                    if (ny < h - 1 && map.isTileBlocked(nx, ny + 1, ignoredBuilding)) continue;
                }
                bfsSeen[nidx] = bfsStamp;
                bfsPrev[nidx] = cur;
                bfsQueue[tail++] = nidx;
            }
        }
        if (!found) return false;
        int len = 0;
        int cur = goal;
        while (cur != start && cur != -1 && len < Unit.MAX_PATH_TILES) {
            u.pathTX[len] = cur % w;
            u.pathTY[len] = cur / w;
            len++;
            cur = bfsPrev[cur];
        }
        if (cur != start) return false;
        for (int i = 0; i < len / 2; i++) {
            int ttx = u.pathTX[i];
            int tty = u.pathTY[i];
            u.pathTX[i] = u.pathTX[len - 1 - i];
            u.pathTY[i] = u.pathTY[len - 1 - i];
            u.pathTX[len - 1 - i] = ttx;
            u.pathTY[len - 1 - i] = tty;
        }
        u.pathLen = len;
        u.pathIdx = 0;
        return true;
    }

    public static void startDetour(Unit u, double gx, double gy, GameMap map) {
        double hx = gx - u.x;
        double hy = gy - u.y;
        double hl = Math.sqrt(hx * hx + hy * hy);
        if (hl < 1.0) return;
        double pxn = -hy / hl;
        double pyn = hx / hl;
        double reach = GameMap.TILE_SIZE * 3.5;
        double ax = u.x + pxn * reach;
        double ay = u.y + pyn * reach;
        double bx = u.x - pxn * reach;
        double by = u.y - pyn * reach;
        double cl = u.getObstacleClearance();
        boolean aBlocked = collidesWithObstacle(ax, ay, cl, map, null) || !inWorldBounds(ax, ay, u.type.radius, map);
        boolean bBlocked = collidesWithObstacle(bx, by, cl, map, null) || !inWorldBounds(bx, by, u.type.radius, map);
        if (aBlocked && bBlocked) {
            return;
        }
        if (aBlocked) {
            u.detourX = bx;
            u.detourY = by;
        } else if (bBlocked) {
            u.detourX = ax;
            u.detourY = ay;
        } else {
            u.detourSide = -u.detourSide;
            if (u.detourSide > 0) {
                u.detourX = ax;
                u.detourY = ay;
            } else {
                u.detourX = bx;
                u.detourY = by;
            }
        }
        u.detourTimer = Unit.DETOUR_SECONDS;
        u.slideSide = 0;
        u.slideLockTimer = 0.0;
    }

    public static final double TRAFFIC_CONTACT_SLOP = 14.0;
    public static final double YIELD_SECONDS = 2.5;
    public static final double YIELD_CLEAR_RADIUS = 340.0;
    public static final double ARRIVAL_RADIUS = 48.0;
    private static final double[] YIELD_BACKOFF = { 192.0, 144.0, 104.0, 72.0 };

    public static void updateCorridorTraffic(Unit[] units, int unitCount, GameMap map, double dt) {
        updateCorridorTraffic(units, unitCount, map, dt, null);
    }

    public static void updateCorridorTraffic(Unit[] units, int unitCount, GameMap map, double dt, UnitSpatialGrid grid) {
        if (units == null || unitCount < 2) return;

        for (int i = 0; i < unitCount; i++) {
            Unit u = units[i];
            if (u == null || u.isDestroyed() || !u.yielding) continue;
            u.yieldTimer -= dt;
            double rdx = u.yieldRetreatX - u.x;
            double rdy = u.yieldRetreatY - u.y;
            if (rdx * rdx + rdy * rdy <= 1600.0) {
                u.moving = false;
                if (u.yieldTimer <= 0.0 || laneAheadClear(u, units, unitCount)) {
                    u.endYield();
                }
            } else if (u.yieldTimer <= 0.0) {
                u.endYield();
            }
        }

        for (int i = 0; i < unitCount; i++) {
            Unit u = units[i];
            if (u == null || u.isDestroyed() || u.yielding || !u.moving) continue;
            if (grid != null) {
                int cx = grid.getCellX(u.x);
                int cy = grid.getCellY(u.y);
                int minCx = Math.max(0, cx - 1);
                int maxCx = Math.min(grid.cols - 1, cx + 1);
                int minCy = Math.max(0, cy - 1);
                int maxCy = Math.min(grid.rows - 1, cy + 1);

                for (int ncy = minCy; ncy <= maxCy; ncy++) {
                    int rowOffset = ncy * grid.cols;
                    for (int ncx = minCx; ncx <= maxCx; ncx++) {
                        int j = grid.head[rowOffset + ncx];
                        while (j != -1) {
                            if (j > i) {
                                Unit v = units[j];
                                checkCorridorTraffic(u, v, map);
                            }
                            j = grid.next[j];
                        }
                    }
                }
            } else {
                for (int j = i + 1; j < unitCount; j++) {
                    Unit v = units[j];
                    checkCorridorTraffic(u, v, map);
                }
            }
        }
    }

    private static void checkCorridorTraffic(Unit u, Unit v, GameMap map) {
        if (v == null || v.isDestroyed() || v.yielding || !v.moving) return;
        if (v.faction != u.faction) return;

        double dx = u.x - v.x;
        double dy = u.y - v.y;
        double minDist = u.type.radius + v.type.radius + TRAFFIC_CONTACT_SLOP;
        if (dx * dx + dy * dy > minDist * minDist) return;
        if (!inSingleLane(u, map) && !inSingleLane(v, map)) return;

        double adx = u.targetX - u.x;
        double ady = u.targetY - u.y;
        double bdx = v.targetX - v.x;
        double bdy = v.targetY - v.y;
        double la = Math.sqrt(adx * adx + ady * ady);
        double lb = Math.sqrt(bdx * bdx + bdy * bdy);
        if (la < 1.0 || lb < 1.0) return;
        double dot = (adx * bdx + ady * bdy) / (la * lb);
        if (dot > -0.35) return;

        Unit loser = (trafficPriority(u) > trafficPriority(v)) ? u : v;
        double ldx = (loser.targetX - loser.x) / (loser == u ? la : lb);
        double ldy = (loser.targetY - loser.y) / (loser == u ? la : lb);
        double cl = loser.getObstacleClearance();

        for (int k = 0; k < YIELD_BACKOFF.length; k++) {
            double rx = loser.x - ldx * YIELD_BACKOFF[k];
            double ry = loser.y - ldy * YIELD_BACKOFF[k];
            if (!inWorldBounds(rx, ry, loser.type.radius, map)) continue;
            if (collidesWithObstacle(rx, ry, cl, map, null)) continue;
            loser.beginYield(rx, ry, YIELD_SECONDS);
            break;
        }
    }

    private static int trafficPriority(Unit u) {
        return (u.type == UnitType.COLLECTOR ? 0 : 100000) + u.id;
    }

    public static boolean inSingleLane(Unit u, GameMap map) {
        int tx = (int) (u.x / GameMap.TILE_SIZE);
        int ty = (int) (u.y / GameMap.TILE_SIZE);
        if (!map.inBounds(tx, ty)) return false;
        boolean north = map.isTileBlocked(tx, ty - 1, null);
        boolean south = map.isTileBlocked(tx, ty + 1, null);
        boolean west = map.isTileBlocked(tx - 1, ty, null);
        boolean east = map.isTileBlocked(tx + 1, ty, null);
        return (north && south) || (west && east);
    }

    private static boolean laneAheadClear(Unit u, Unit[] units, int unitCount) {
        double dx = u.yieldResumeX - u.x;
        double dy = u.yieldResumeY - u.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0) return true;
        dx /= len;
        dy /= len;
        for (int i = 0; i < unitCount; i++) {
            Unit o = units[i];
            if (o == null || o == u || o.isDestroyed() || o.faction != u.faction) continue;
            if (!o.moving && !o.yielding) continue;
            double ox = o.x - u.x;
            double oy = o.y - u.y;
            double od = Math.sqrt(ox * ox + oy * oy);
            if (od < 1.0 || od > YIELD_CLEAR_RADIUS) continue;
            if ((ox * dx + oy * dy) / od > 0.25) return false;
        }
        return true;
    }

    public static void updateUnitMovement(Unit u, double dt, GameMap map) {
        double destX = u.targetX;
        double destY = u.targetY;
        double aimX = u.getAimX();
        double aimY = u.getAimY();

        if ((u.attackTargetUnit != null || u.attackTargetBuilding != null) && !u.yielding) {
            if (u.stance == Stance.GUARD) {
                double anchorDistSq = (u.guardAnchorX - u.x) * (u.guardAnchorX - u.x) + (u.guardAnchorY - u.y) * (u.guardAnchorY - u.y);
                if (anchorDistSq <= 114.0) {
                    destX = u.x;
                    destY = u.y;
                    u.moving = false;
                }
            } else {
                double dist = Math.sqrt((aimX - u.x) * (aimX - u.x) + (aimY - u.y) * (aimY - u.y));
                if (u.stance == Stance.HUNT && dist > u.type.sightRange * 2.0) {
                    u.attackTargetUnit = null;
                    u.attackTargetBuilding = null;
                    if (u.hasHuntWaypoint) {
                        destX = u.huntWaypointX;
                        destY = u.huntWaypointY;
                        u.targetX = u.huntWaypointX;
                        u.targetY = u.huntWaypointY;
                        u.returningToHuntStation = false;
                    } else if (u.hasHuntStation) {
                        destX = u.huntStationX;
                        destY = u.huntStationY;
                        u.targetX = u.huntStationX;
                        u.targetY = u.huntStationY;
                        u.returningToHuntStation = true;
                    } else {
                        destX = u.x;
                        destY = u.y;
                        u.moving = false;
                    }
                } else if (dist <= u.type.attackRange) {
                    destX = u.x;
                    destY = u.y;
                    u.moving = false;
                } else {
                    destX = aimX;
                    destY = aimY;
                }
            }
        } else if (u.repairTargetUnit != null && !u.yielding) {
            double dist = Math.sqrt((aimX - u.x) * (aimX - u.x) + (aimY - u.y) * (aimY - u.y));
            if (dist <= Unit.REPAIR_UNIT_RANGE) {
                destX = u.x;
                destY = u.y;
                u.moving = false;
                u.isRepairing = true;
            } else {
                destX = aimX;
                destY = aimY;
                u.isRepairing = false;
            }
        } else if (u.repairTargetBuilding != null && !u.yielding) {
            double minBx = (double) u.repairTargetBuilding.pixelX;
            double maxBx = (double) (u.repairTargetBuilding.pixelX + u.repairTargetBuilding.type.widthTiles * GameMap.TILE_SIZE);
            double minBy = (double) u.repairTargetBuilding.pixelY;
            double maxBy = (double) (u.repairTargetBuilding.pixelY + u.repairTargetBuilding.type.heightTiles * GameMap.TILE_SIZE);
            double bx = Math.clamp(u.x, minBx, maxBx);
            double by = Math.clamp(u.y, minBy, maxBy);
            double dist = Math.sqrt((bx - u.x) * (bx - u.x) + (by - u.y) * (by - u.y));
            if (dist <= Unit.REPAIR_BUILDING_RANGE) {
                destX = u.x;
                destY = u.y;
                u.moving = false;
                u.isRepairing = true;
            } else {
                destX = aimX;
                destY = aimY;
                u.isRepairing = false;
            }
        }

        Building ignoredBuilding = (u.repairTargetBuilding != null) ? u.repairTargetBuilding : null;

        if (u.yielding) {
            destX = u.yieldRetreatX;
            destY = u.yieldRetreatY;
        }

        int destTX = (int) (destX / GameMap.TILE_SIZE);
        int destTY = (int) (destY / GameMap.TILE_SIZE);
        boolean mapChanged = u.pathVersion != map.version;
        if (destTX != u.pathGoalTX || destTY != u.pathGoalTY || mapChanged) {
            boolean highPriority = u.attackTargetUnit != null || u.attackTargetBuilding != null ||
                                   u.threatTimer > 0.0 || u.retreatingToRepair || u.yielding;
            if (takePathBudget(highPriority)) {
                boolean routed = computePath(u, map, destTX, destTY, ignoredBuilding);
                u.pathVersion = map.version;
                if (!routed && u.attackTargetUnit == null && u.attackTargetBuilding == null) {
                    u.moving = false;
                }
            } else {
                u.pathLen = 0;
                u.pathIdx = 0;
            }
        }
        if (u.pathLen > 0 && u.pathIdx < u.pathLen) {
            int targetIdx = -1;
            while (u.pathIdx < u.pathLen) {
                double wx = (u.pathTX[u.pathIdx] + 0.5) * GameMap.TILE_SIZE;
                double wy = (u.pathTY[u.pathIdx] + 0.5) * GameMap.TILE_SIZE;
                double wdx = wx - u.x;
                double wdy = wy - u.y;
                double wdistSq = wdx * wdx + wdy * wdy;
                if (wdistSq <= 1156.0) {
                    u.pathIdx++;
                } else if (wdistSq >= 2304.0) {
                    targetIdx = u.pathIdx;
                    break;
                } else {
                    u.pathIdx++;
                }
            }
            if (targetIdx >= 0) {
                destX = (u.pathTX[targetIdx] + 0.5) * GameMap.TILE_SIZE;
                destY = (u.pathTY[targetIdx] + 0.5) * GameMap.TILE_SIZE;
            } else if (u.pathIdx >= u.pathLen) {
                u.pathLen = 0;
                int otx = (int) (u.targetX / GameMap.TILE_SIZE);
                int oty = (int) (u.targetY / GameMap.TILE_SIZE);
                if (map.isTileBlocked(otx, oty, ignoredBuilding)) {
                    u.moving = false;
                    u.returningToHuntStation = false;
                }
            }
        }

        if (u.detourTimer > 0.0) {
            u.detourTimer -= dt;
            if (u.detourTimer <= 0.0) {
                u.detourTimer = 0.0;
                u.pathLen = 0;
                u.pathGoalTX = -9999;
                u.pathGoalTY = -9999;
            }
            destX = u.detourX;
            destY = u.detourY;
        } else {
            u.stuckCheckTimer += dt;
            if (u.stuckCheckTimer >= Unit.STUCK_CHECK_SECONDS) {
                u.stuckCheckTimer = 0.0;
                double psx = u.x - u.stuckCheckX;
                double psy = u.y - u.stuckCheckY;
                u.stuckCheckX = u.x;
                u.stuckCheckY = u.y;
                if (psx * psx + psy * psy < 256.0) {
                    u.stuckCount++;
                    if (u.stuckCount >= 2) {
                        u.stuckCount = 0;
                        startDetour(u, destX, destY, map);
                    }
                } else {
                    u.stuckCount = 0;
                }
            }
        }

        double dx = destX - u.x;
        double dy = destY - u.y;
        double distSq = dx * dx + dy * dy;

        if (distSq > 79.0) {
            double dist = Math.sqrt(distSq);
            double dirX = dx / dist;
            double dirY = dy / dist;

            int tx = (int) (u.x / GameMap.TILE_SIZE);
            int ty = (int) (u.y / GameMap.TILE_SIZE);
            double terrainMult = 1.0;
            if (map.inBounds(tx, ty) && map.terrain[map.index(tx, ty)] == GameMap.TERRAIN_SAND) {
                terrainMult = (u.type == UnitType.TROOPER_RIFLE || u.type == UnitType.SHARD_SKITTERER) ? 0.95 : 0.75;
            }

            double step = u.type.speed * terrainMult * dt;
            if (step > dist) step = dist;

            double orderDx = u.targetX - u.x;
            double orderDy = u.targetY - u.y;
            double orderDist = Math.sqrt(orderDx * orderDx + orderDy * orderDy);
            double turnRate = 4.0;
            if (orderDist < ARRIVAL_RADIUS) {
                double approach = orderDist / ARRIVAL_RADIUS;
                turnRate = 4.0 + (1.0 - approach) * 12.0;
                step *= 0.30 + 0.70 * approach;
            }

            double clearance = u.getObstacleClearance();

            double targetHeading = Math.atan2(dy, dx);
            double testAngle = Unit.rotateToward(u.angle, targetHeading, turnRate * dt);
            double testStepX = Math.cos(testAngle) * step;
            double testStepY = Math.sin(testAngle) * step;
            double nextX = u.x + testStepX;
            double nextY = u.y + testStepY;

            if (!collidesWithObstacle(nextX, nextY, clearance, map, ignoredBuilding) && inWorldBounds(nextX, nextY, u.type.radius, map)) {
                u.x = nextX;
                u.y = nextY;
                u.angle = testAngle;
                u.slideSide = 0;
                u.slideLockTimer = 0.0;
            } else {
                boolean moved = false;
                sharedContact.reset();
                if (findClosestObstacle(nextX, nextY, clearance + step + 11.0, map, ignoredBuilding, sharedContact)) {
                    double cdx = nextX - sharedContact.x;
                    double cdy = nextY - sharedContact.y;
                    double cLen = Math.sqrt(cdx * cdx + cdy * cdy);
                    if (cLen > 0.001) {
                        double nx = cdx / cLen;
                        double ny = cdy / cLen;

                        double t1x = -ny;
                        double t1y = nx;
                        double t2x = ny;
                        double t2y = -nx;

                        double dot1 = t1x * dirX + t1y * dirY;
                        double dot2 = t2x * dirX + t2y * dirY;
                        if (u.slideLockTimer > 0.0 && u.slideSide != 0) {
                            double lockedDot = (u.slideSide == 1) ? dot1 : dot2;
                            double otherDot = (u.slideSide == 1) ? dot2 : dot1;
                            if (otherDot > lockedDot + 0.45) {
                                u.slideSide = -u.slideSide;
                                u.slideLockTimer = Unit.SLIDE_LOCK_SECONDS;
                            } else {
                                u.slideLockTimer -= dt;
                            }
                        } else {
                            u.slideSide = (dot1 >= dot2) ? 1 : -1;
                            u.slideLockTimer = Unit.SLIDE_LOCK_SECONDS;
                        }
                        double bestTx = (u.slideSide == 1) ? t1x : t2x;
                        double bestTy = (u.slideSide == 1) ? t1y : t2y;

                        double sx = bestTx * 0.88 + nx * 0.24;
                        double sy = bestTy * 0.88 + ny * 0.24;
                        double sLen = Math.sqrt(sx * sx + sy * sy);
                        if (sLen > 0.001) {
                            sx /= sLen;
                            sy /= sLen;

                            double slideNextX = u.x + sx * step;
                            double slideNextY = u.y + sy * step;

                            if (!collidesWithObstacle(slideNextX, slideNextY, clearance, map, ignoredBuilding) && inWorldBounds(slideNextX, slideNextY, u.type.radius, map)) {
                                u.x = slideNextX;
                                u.y = slideNextY;
                                u.angle = Unit.rotateToward(u.angle, Math.atan2(sy, sx), 5.0 * dt);
                                moved = true;
                            }
                        }
                    }
                }

                if (!moved) {
                    if (!collidesWithObstacle(u.x + testStepX, u.y, clearance, map, ignoredBuilding) && inWorldBounds(u.x + testStepX, u.y, u.type.radius, map)) {
                        u.x += testStepX;
                        moved = true;
                    }
                    if (!collidesWithObstacle(u.x, u.y + testStepY, clearance, map, ignoredBuilding) && inWorldBounds(u.x, u.y + testStepY, u.type.radius, map)) {
                        u.y += testStepY;
                        moved = true;
                    }
                    if (moved) u.angle = testAngle;
                }

                if (!moved) {
                    double altStepX = (destX >= u.x) ? step : -step;
                    double altStepY = (destY >= u.y) ? step : -step;
                    if (!collidesWithObstacle(u.x + altStepX, u.y, clearance, map, ignoredBuilding) && inWorldBounds(u.x + altStepX, u.y, u.type.radius, map)) {
                        u.x += altStepX * 0.8;
                        moved = true;
                    } else if (!collidesWithObstacle(u.x, u.y + altStepY, clearance, map, ignoredBuilding) && inWorldBounds(u.x, u.y + altStepY, u.type.radius, map)) {
                        u.y += altStepY * 0.8;
                        moved = true;
                    }
                    if (moved) u.angle = testAngle;
                }

                if (!moved) {
                    if (distSq <= (clearance + 14.0) * (clearance + 14.0) || distSq <= 114.0) {
                        if (u.detourTimer > 0.0) {
                            u.detourTimer = 0.0;
                            u.pathLen = 0;
                            u.pathGoalTX = -9999;
                            u.pathGoalTY = -9999;
                        } else {
                            u.moving = false;
                            u.returningToHuntStation = false;
                        }
                    } else {
                        int dtx = (int) (destX / GameMap.TILE_SIZE);
                        int dty = (int) (destY / GameMap.TILE_SIZE);
                        if (map.inBounds(dtx, dty)) {
                            Building db = map.buildingGrid[map.index(dtx, dty)];
                            if (db != null && !db.isDestroyed() && db != ignoredBuilding) {
                                double bMinX = db.pixelX;
                                double bMaxX = db.pixelX + db.type.widthTiles * GameMap.TILE_SIZE;
                                double bMinY = db.pixelY;
                                double bMaxY = db.pixelY + db.type.heightTiles * GameMap.TILE_SIZE;
                                double nearX = Math.clamp(u.x, bMinX, bMaxX);
                                double nearY = Math.clamp(u.y, bMinY, bMaxY);
                                double bDistSq = (u.x - nearX) * (u.x - nearX) + (u.y - nearY) * (u.y - nearY);
                                if (bDistSq <= (clearance + 8.0) * (clearance + 8.0)) {
                                    if (u.detourTimer > 0.0) {
                                        u.detourTimer = 0.0;
                                        u.pathLen = 0;
                                        u.pathGoalTX = -9999;
                                        u.pathGoalTY = -9999;
                                    } else {
                                        u.moving = false;
                                        u.returningToHuntStation = false;
                                    }
                                }
                            }
                        }
                    }
                }

                resolveObstacleOverlap(u, map, clearance, ignoredBuilding);
            }
        } else {
            if (u.detourTimer > 0.0) {
                u.detourTimer = 0.0;
                u.pathLen = 0;
                u.pathGoalTX = -9999;
                u.pathGoalTY = -9999;
            } else {
                u.moving = false;
                u.returningToHuntStation = false;
                if (u.hasHuntWaypoint && u.attackTargetUnit == null && u.attackTargetBuilding == null) {
                    u.hasHuntWaypoint = false;
                }
            }
        }
    }
}
