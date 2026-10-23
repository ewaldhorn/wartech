package wartech.ai;

// Base defender stationing and the perimeter alerts

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.UnitType;

public final class EnemyDefense {
    private final EnemyAI ai;

    public EnemyDefense(EnemyAI ai) {
        this.ai = ai;
    }

    public void update(GameMap map, WarTech game) {
        Building con = EnemyAI.findEnemyBuilding(BuildingType.CON_YARD, game);
        Building ref = EnemyAI.findEnemyBuilding(BuildingType.REFINERY, game);
        double bx = DefenseStations.baseX(con, map);
        double by = DefenseStations.baseY(con);

        int activeDef = reanchorDefenders(game);
        activeDef = conscriptDefenders(activeDef, bx, by, ref, con, map, game);

        ai.baseDefenderCount = activeDef;
        ai.assaultForceCount = ai.assault.countAvailableAssaultUnits(game);

        if (isBaseThreatened(bx, by, game)) {
            alertDefenders(game);
        }
    }

    // Counts base defenders and sends idle strays back to their station. 
    // Returns the active defender count.
    private int reanchorDefenders(WarTech game) {
        int activeDef = 0;
        double driftSq = AiTuning.DEFENDER_DRIFT_RADIUS * AiTuning.DEFENDER_DRIFT_RADIUS;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY) continue;
            if (!u.isBaseDefender) continue;
            activeDef++;
            // If not retreating for repairs, check if this unit has drifted from its post and send it back
            if (!u.retreatingToRepair && !u.moving && u.attackTargetUnit == null
                    && u.attackTargetBuilding == null && u.hasHuntStation) {
                double dSq = (u.x - u.huntStationX) * (u.x - u.huntStationX) + (u.y - u.huntStationY) * (u.y - u.huntStationY);
                if (dSq > driftSq) {
                    u.moveTo(u.huntStationX, u.huntStationY);
                }
            }
        }
        return activeDef;
    }

    // If we have run out of defenders for whatever reason, check if there's someone standing around
    // doing nothing. If they are not tasked with work, assign them to guard duty.
    private int conscriptDefenders(int activeDef, double bx, double by, Building ref, Building con,
                                   GameMap map, WarTech game) {
        int reqDefenders = ai.getRequiredDefenders();
        if (activeDef >= reqDefenders) return activeDef;

        double baseRadiusSq = AiTuning.DEFENDER_CONSCRIPT_RADIUS * AiTuning.DEFENDER_CONSCRIPT_RADIUS;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY) continue;
            if (u.type == UnitType.COLLECTOR || u.type == UnitType.MECHANIC || u.type == UnitType.MEDIC) continue;
            if (u.isBaseDefender) continue;
            if (u.hasHuntWaypoint) continue;
            if (u.attackTargetUnit != null || u.attackTargetBuilding != null) continue;
            if (u.retreatingToRepair) continue;
            double bdx = u.x - bx;
            double bdy = u.y - by;
            if (bdx * bdx + bdy * bdy > baseRadiusSq) continue;

            double rawSx = DefenseStations.stationX(activeDef, bx, by, ref, con, map);
            double rawSy = DefenseStations.stationY(activeDef, bx, by, ref, con, map);
            double sx = DefenseStations.resolveClearX(rawSx, rawSy, map);
            double sy = DefenseStations.resolveClearY(rawSx, rawSy, map);

            u.isBaseDefender = true;
            u.stance = Stance.HUNT;
            u.huntStationX = sx;
            u.huntStationY = sy;
            u.hasHuntStation = true;
            u.returningToHuntStation = false;
            u.hasHuntWaypoint = false;
            u.moveTo(sx, sy);

            activeDef++;
            if (activeDef >= reqDefenders) break;
        }
        return activeDef;
    }

    // Check if we are under attack, buildings under construction should not alert, unless they
    // are actually attacked. This logic needs a bit of cleaning up still.
    private boolean isBaseThreatened(double bx, double by, WarTech game) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.faction != Faction.ENEMY || b.hp >= b.maxHp) continue;
            if (b.constructing && b.hp >= expectedFoundationHp(b)) continue;
            return true;
        }
        double threatSq = AiTuning.BASE_THREAT_RADIUS * AiTuning.BASE_THREAT_RADIUS;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (!u.isDestroyed() && u.faction == Faction.PLAYER) {
                double dSq = (u.x - bx) * (u.x - bx) + (u.y - by) * (u.y - by);
                if (dSq < threatSq) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int expectedFoundationHp(Building b) {
        double f = AiTuning.FOUNDATION_MIN_HP_FRACTION;
        return (int) (b.maxHp * (f + (1.0 - f) * b.constructionProgress));
    }

    private void alertDefenders(WarTech game) {
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (!u.isDestroyed() && u.faction == Faction.ENEMY && u.isBaseDefender) {
                if (u.retreatingToRepair) continue;
                if (u.attackTargetUnit == null && u.attackTargetBuilding == null) {
                    game.acquireHuntTarget(u);
                }
            }
        }
    }
}
