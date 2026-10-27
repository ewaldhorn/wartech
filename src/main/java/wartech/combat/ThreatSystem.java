package wartech.combat;

// Autonomous fauna, Precursor ruins defense, and Shard Skitterers

import wartech.ai.Difficulty;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.ScavengerRole;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public class ThreatSystem {
    private final WarTech game;

    public static final int MAX_REINFORCEMENTS = 2;
    public static final double SALVAGE_SEARCH_RADIUS = 1200.0;
    public int reinforcementsSpawned = 0;

    public ThreatSystem(WarTech game) {
        this.game = game;
    }

    public void updateScavengers(double dt, GameMap map) {
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.NEUTRAL || u.scavenger == null) continue;
            var s = u.scavenger;

            if (!u.hasHuntStation) {
                u.huntStationX = u.x;
                u.huntStationY = u.y;
                u.hasHuntStation = true;
            }

            Wreckage wreck = s.salvageTarget;
            if (wreck != null && (!wreck.active || wreck.scrapValue <= 0 || wreck.id != s.salvageTargetId)) {
                wreck = null;
                s.clearTarget();
            }

            boolean hauling = s.isFull();
            if (!hauling && wreck == null) {
                wreck = findWreck(u);
                s.salvageTarget = wreck;
                s.salvageTargetId = wreck != null ? wreck.id : 0;
                s.salvageTimer = 0.0;
                if (wreck == null && s.hasCargo()) hauling = true;
            }

            if (hauling) {
                s.clearTarget();
                double hdx = u.huntStationX - u.x;
                double hdy = u.huntStationY - u.y;
                if (hdx * hdx + hdy * hdy <= 64.0 * 64.0) {
                    deliverScrap(u);
                } else if (!u.moving) {
                    u.moveTo(u.huntStationX, u.huntStationY);
                    s.returningScrap = true;
                }
                continue;
            }

            if (wreck == null) continue;

            double dx = wreck.x - u.x;
            double dy = wreck.y - u.y;
            if (dx * dx + dy * dy > 44.0 * 44.0) {
                if (!u.moving) u.moveTo(wreck.x, wreck.y);
                continue;
            }

            u.moving = false;
            s.salvageTimer += dt;
            while (s.salvageTimer >= 0.5 && wreck != null) {
                s.salvageTimer -= 0.5;
                int take = Math.min(20, Math.min(wreck.scrapValue, ScavengerRole.MAX_SCRAP - s.scrapCarried));
                wreck.scrapValue -= take;
                s.scrapCarried += take;
                game.spawnEffect(Effect.TYPE_SPARK, wreck.x, wreck.y, 0.25, 12.0);
                if (wreck.scrapValue <= 0) {
                    wreck.active = false;
                    s.clearTarget();
                    wreck = null;
                }
            }
        }
    }

    private Wreckage findWreck(Unit u) {
        Wreckage best = null;
        double bestDistSq = SALVAGE_SEARCH_RADIUS * SALVAGE_SEARCH_RADIUS;
        for (int i = 0; i < game.wreckCount; i++) {
            var w = game.wrecks[i];
            if (!w.active || w.scrapValue <= 0) continue;
            double dx = w.x - u.x;
            double dy = w.y - u.y;
            double dSq = dx * dx + dy * dy;
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                best = w;
            }
        }
        return best;
    }

    private void deliverScrap(Unit u) {
        if (u.scavenger != null) {
            u.scavenger.scrapCarried = 0;
            u.scavenger.returningScrap = false;
        }
        u.moving = false;
        if (game.selectedDifficulty.atLeast(Difficulty.MEDIUM)
                && reinforcementsSpawned < MAX_REINFORCEMENTS
                && game.unitCount < WarTech.MAX_UNITS) {
            var clone = new Unit(UnitType.SCAVENGER_DRONE, Faction.NEUTRAL, u.huntStationX, u.huntStationY);
            clone.hasHuntStation = true;
            clone.huntStationX = u.huntStationX;
            clone.huntStationY = u.huntStationY;
            if (game.addUnit(clone)) {
                reinforcementsSpawned++;
                game.spawnEffect(Effect.TYPE_MUZZLE, u.huntStationX, u.huntStationY, 0.35, 18.0);
            }
        }
    }

    public void spawnRuinsThreats(GameMap map) {
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (map.terrain[map.index(tx, ty)] == GameMap.TERRAIN_RUIN && (tx % 3 == 0) && (ty % 3 == 0)) {
                    if (game.unitCount >= WarTech.MAX_UNITS) return;
                    game.addUnit(new Unit(UnitType.SCAVENGER_DRONE, Faction.NEUTRAL, tx * GameMap.TILE_SIZE, ty * GameMap.TILE_SIZE));
                    if (game.simRng.chance(0.3) && game.unitCount < WarTech.MAX_UNITS) {
                        game.addUnit(new Unit(UnitType.PRECURSOR_WALKER, Faction.NEUTRAL, (tx + 1) * GameMap.TILE_SIZE, (ty + 1) * GameMap.TILE_SIZE));
                    }
                }
            }
        }
    }

    public void checkGeodeDisturbance(int utx, int uty, Unit triggerUnit) {
        if (utx < 0 || uty < 0 || utx >= game.map.width || uty >= game.map.height) return;
        breachGeode(utx, uty, triggerUnit);
    }

    public void breachGeodesInRadius(double x, double y, double radius) {
        if (radius <= 0.0) return;
        double r2 = radius * radius;
        int minTx = Math.max(0, (int) ((x - radius) / GameMap.TILE_SIZE));
        int maxTx = Math.min(game.map.width - 1, (int) ((x + radius) / GameMap.TILE_SIZE));
        int minTy = Math.max(0, (int) ((y - radius) / GameMap.TILE_SIZE));
        int maxTy = Math.min(game.map.height - 1, (int) ((y + radius) / GameMap.TILE_SIZE));

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int idx = game.map.index(tx, ty);
                if (game.map.geode[idx] != GameMap.GEODE_DORMANT) continue;
                double cx = (tx + 0.5) * GameMap.TILE_SIZE;
                double cy = (ty + 0.5) * GameMap.TILE_SIZE;
                double dx = cx - x;
                double dy = cy - y;
                if (dx * dx + dy * dy > r2) continue;
                breachGeode(tx, ty, null);
            }
        }
    }

    private void breachGeode(int tx, int ty, Unit triggerUnit) {
        int idx = game.map.index(tx, ty);
        if (game.map.geode[idx] != GameMap.GEODE_DORMANT) return;
        if (game.unitCount + 3 > WarTech.MAX_UNITS) return;
        game.map.geode[idx] = GameMap.GEODE_BREACHED;
        for (int s = 0; s < 3; s++) {
            var sk = new Unit(UnitType.SHARD_SKITTERER, Faction.NEUTRAL,
                (tx + game.simRng.nextDouble() - 0.5) * GameMap.TILE_SIZE,
                (ty + game.simRng.nextDouble() - 0.5) * GameMap.TILE_SIZE);
            sk.threatTimer = 30.0;
            if (triggerUnit != null) {
                sk.attackUnit(triggerUnit);
            }
            game.addUnit(sk);
        }
    }
}
