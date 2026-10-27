package wartech.ai;

// Pulls damaged enemy vehicles back to the Repair Facility (Veteran and Commander)

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public final class EnemyRetreat {
    private final EnemyAI ai;

    public EnemyRetreat(EnemyAI ai) {
        this.ai = ai;
    }

    public void update(AiCensus census, WarTech game) {
        if (!ai.difficulty.atLeast(Difficulty.MEDIUM) || census.repairFacilities <= 0) return;
        Building fac = EnemyAI.findEnemyBuilding(BuildingType.REPAIR_FACILITY, game);
        if (fac == null) return;

        double retreatThreshold = ai.tuning().retreatFraction();
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY || !u.type.isVehicle()) continue;
            if (u.type == UnitType.COLLECTOR) continue;
            processVehicle(u, fac, retreatThreshold);
        }
    }

    private void processVehicle(Unit u, Building fac, double retreatThreshold) {
        if (u.retreatingToRepair) {
            if (u.hp > (int)(u.maxHp * (retreatThreshold + AiTuning.RETREAT_HYSTERESIS))) {
                u.retreatingToRepair = false;
                u.retreatRetries = 0;
            }
            if (!u.retreatingToRepair) return;
        } else if (u.hp >= (int)(u.maxHp * retreatThreshold) || u.isRepairing()) {
            return;
        }
        double dToFac = Math.sqrt((fac.getCenterX() - u.x) * (fac.getCenterX() - u.x) + (fac.getCenterY() - u.y) * (fac.getCenterY() - u.y));
        double depotX = fac.getCenterX();
        double depotY = fac.pixelY + fac.type.heightTiles * GameMap.TILE_SIZE + AiTuning.RETREAT_DEPOT_MARGIN;
        if (u.retreatingToRepair) {
            if (!u.moving && dToFac > AiTuning.RETREAT_ARRIVED_RADIUS) {
                u.retreatRetries++;
                if (u.retreatRetries > AiTuning.MAX_RETREAT_RETRIES) {
                    u.retreatingToRepair = false;
                    u.retreatRetries = 0;
                } else {
                    u.moveTo(depotX, depotY);
                }
            }
            return;
        }
        u.retreatingToRepair = true;
        u.retreatRetries = 0;
        u.attackTargetUnit = null;
        u.attackTargetBuilding = null;
        if (dToFac <= AiTuning.RETREAT_PARKED_RADIUS) {
            u.moving = false;
        } else {
            u.moveTo(depotX, depotY);
        }
    }
}
