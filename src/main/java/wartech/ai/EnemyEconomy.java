package wartech.ai;

// Enemy income and Collector fleet upkeep and mineral collecting orders.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.combat.Effect;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public final class EnemyEconomy {
    private final EnemyAI ai;

    public EnemyEconomy(EnemyAI ai) {
        this.ai = ai;
    }

    /**
     * To make games a bit more interesting, the enemy gets some free credits and when it's lost
     * all collectors, it is given a new one, for free, if they still have a refinery. It's set
     * so they don't get a collecter more than once per 20 seconds.
     */
    public void applyIncome(AiCensus census, WarTech game) {
        ai.addCredits(ai.tuning().creditsPerThink());

        if (census.collectors != 0) return;
        Building grantRef = EnemyAI.findEnemyBuilding(BuildingType.REFINERY, game);
        if (ai.collectorGrantCooldown <= 0.0 && grantRef != null && game.unitCount < WarTech.MAX_UNITS) {
            var col = grantRef.spawnUnit(UnitType.COLLECTOR, game.map);
            if (col.harvester != null) {
                col.harvester.autoCollect = true;
            }
            game.addUnit(col);
            game.spawnEffect(Effect.TYPE_MUZZLE, col.x, col.y, AiTuning.SPAWN_EFFECT_SECONDS, AiTuning.SPAWN_EFFECT_SIZE);
            ai.collectorGrantCooldown = AiTuning.COLLECTOR_GRANT_COOLDOWN_SECONDS;
            census.collectors++;
        } else if (grantRef == null && ai.credits < UnitType.COLLECTOR.cost) {
            ai.addCredits(AiTuning.NO_REFINERY_RELIEF_CREDITS); // no refinery to deliver from so save toward rebuilding
        }
    }

    // Top economic priority is to keep the Collector fleet maxed
    public void queueReplacementCollectors(AiCensus census, WarTech game) {
        AiTuning.Tier tier = ai.tuning();
        int maxCollectors = census.refineries > 1 ? tier.maxCollectorsExpanded() : tier.maxCollectors();
        var production = ai.production;
        if (census.collectors + production.countQueued(UnitType.COLLECTOR) < maxCollectors
                && EnemyAI.findEnemyBuilding(BuildingType.REFINERY, game) != null
                && production.hasRoom()) {
            production.enqueue(UnitType.COLLECTOR);
        }
    }

    // Send full Collectors to a Refinery and idle ones to the nearest crystal field
    public void directCollectors(GameMap map, WarTech game) {
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY || u.harvester == null) continue;

            if (u.harvester.isFull()) {
                returnToRefinery(u, game);
            } else if (!u.moving && !u.harvester.returningToRefinery) {
                seekCrystals(u, map);
            }
        }
    }

    private void returnToRefinery(Unit u, WarTech game) {
        var ref = game.findNearestRefinery(u.x, u.y, Faction.ENEMY);
        if (ref == null) return;
        double gx = ref.getCenterX();
        double gy = ref.getCenterY() + AiTuning.REFINERY_DEPOT_OFFSET_Y;
        double dx = gx - u.x;
        double dy = gy - u.y;
        double depositRange = WarTech.REFINERY_DEPOSIT_RADIUS;
        if (dx * dx + dy * dy > depositRange * depositRange && (u.harvester == null || !u.harvester.returningToRefinery || !u.moving)) {
            u.moveTo(gx, gy);
            if (u.harvester != null) {
                u.harvester.returningToRefinery = true;
            }
        }
    }

    // Orders the unit to the nearest crystal tile within the search radius
    private void seekCrystals(Unit u, GameMap map) {
        int bestTx = -1;
        int bestTy = -1;
        double bestDist = Double.MAX_VALUE;

        int utx = (int) (u.x / GameMap.TILE_SIZE);
        int uty = (int) (u.y / GameMap.TILE_SIZE);

        for (int r = 1; r < AiTuning.CRYSTAL_SEARCH_RADIUS_TILES; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int tx = utx + dx;
                    int ty = uty + dy;
                    if (map.inBounds(tx, ty) && map.crystals[map.index(tx, ty)] > 0) {
                        double d = dx * dx + dy * dy;
                        if (d < bestDist) {
                            bestDist = d;
                            bestTx = tx;
                            bestTy = ty;
                        }
                    }
                }
            }
            if (bestTx != -1) break;
        }

        if (bestTx != -1) {
            u.moveTo((bestTx + 0.5) * GameMap.TILE_SIZE, (bestTy + 0.5) * GameMap.TILE_SIZE);
        }
    }
}
