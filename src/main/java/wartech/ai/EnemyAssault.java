package wartech.ai;

// Wave timing, target selection and attack-move orders for enemy assaults

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public final class EnemyAssault {
    private final EnemyAI ai;

    public EnemyAssault(EnemyAI ai) {
        this.ai = ai;
    }

    // Called once attackTimer expires and then either launches the wave or hold until enough units are available
    public void tickWave(WarTech game) {
        int availableAssault = countAvailableAssaultUnits(game);
        int minForce = ai.getMinAssaultForce();

        if (availableAssault >= minForce) {
            ai.waveCount++;
            ai.attackTimer = ai.waveInterval;
            launchAttack(game);
        } else {
            ai.attackTimer = getRecheckSeconds();
        }
    }

    public int getRecheckSeconds() {
        return ai.tuning().assaultRecheckSeconds();
    }

    // Check if a vehicle should be repaired rather than sent
    private boolean isHeldBackForRepair(Unit u) {
        return ai.difficulty.atLeast(Difficulty.MEDIUM) && u.type.isVehicle()
            && u.hp < (int)(u.maxHp * AiTuning.WOUNDED_VEHICLE_FRACTION);
    }

    public int countAvailableAssaultUnits(WarTech game) {
        int count = 0;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY) continue;
            if (u.type == UnitType.COLLECTOR || u.type == UnitType.MECHANIC) continue;
            if (u.isBaseDefender) continue;
            if (isHeldBackForRepair(u)) continue;
            count++;
        }
        return count;
    }

    private static boolean joinsAssault(UnitType t) {
        return t == UnitType.RAVAGER_TANK || t == UnitType.DREADNOUGHT || t == UnitType.REAVER_BUGGY
            || t == UnitType.RAIDER || t == UnitType.VANDAL || t == UnitType.MEDIC;
    }

    private void launchAttack(WarTech game) {
        Building home = EnemyAI.findEnemyBuilding(BuildingType.CON_YARD, game);
        double ox = DefenseStations.baseX(home, game.map);
        double oy = DefenseStations.baseY(home);

        double targetX = AiTuning.DEFAULT_ASSAULT_MARGIN_TILES * GameMap.TILE_SIZE;
        double targetY = (game.map.height - AiTuning.DEFAULT_ASSAULT_MARGIN_TILES) * GameMap.TILE_SIZE;

        // Units go after Collectors on Veteran and Commander difficulty
        Unit raidTarget = ai.difficulty.atLeast(Difficulty.MEDIUM) ? nearestPlayerCollector(game, ox, oy) : null;

        Unit pCol = null;
        Building pBld;
        if (ai.difficulty == Difficulty.HARD) {
            // Commander targets Collectors, then power, then the core
            pCol = raidTarget;
            pBld = nearestPlayerBuilding(game, ox, oy, BuildingType.WIND_TURBINE, null, null);
            if (pBld == null) pBld = nearestPlayerBuilding(game, ox, oy, BuildingType.REFINERY, BuildingType.CON_YARD, null);
        } else {
            // Recruit and Veteran target economic and production structures first
            pBld = nearestPlayerBuilding(game, ox, oy, BuildingType.REFINERY, null, null);
            if (pBld == null) pBld = nearestPlayerBuilding(game, ox, oy, BuildingType.HEAVY_FACTORY, BuildingType.BARRACKS, null);
        }
        if (pBld == null) pBld = nearestPlayerBuilding(game, ox, oy, null, null, null);

        // The army aims at structures; only the fast raiders peel off for Collectors 
        if (pBld != null) {
            targetX = pBld.getCenterX();
            targetY = pBld.getCenterY();
        } else if (pCol != null) {
            targetX = pCol.x;
            targetY = pCol.y;
        } else {
            // Fallback: any live player unit
            for (int i = 0; i < game.unitCount; i++) {
                var u = game.units[i];
                if (!u.isDestroyed() && u.faction == Faction.PLAYER) {
                    targetX = u.x;
                    targetY = u.y;
                    break;
                }
            }
        }

        int maxAssault = ai.tuning().assaultCap(ai.waveCount);
        int sent = 0;

        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY) continue;
            // Never send Mechanics or Collectors to fight
            if (u.type == UnitType.MECHANIC || u.type == UnitType.COLLECTOR) continue;
            // Never send dedicated Base Defenders to assault, they must remain on station
            if (u.isBaseDefender) continue;
            // In Medium and Hard, don't send damaged tanks that are undergoing repairs
            if (isHeldBackForRepair(u)) continue;

            if (joinsAssault(u.type)) {
                double dx = targetX;
                double dy = targetY;
                if (u.type == UnitType.REAVER_BUGGY && raidTarget != null) {
                    dx = raidTarget.x;
                    dy = raidTarget.y;
                }
                orderAssault(u, dx + game.simRng.range(-AiTuning.ASSAULT_SPREAD, AiTuning.ASSAULT_SPREAD),
                        dy + game.simRng.range(-AiTuning.ASSAULT_SPREAD, AiTuning.ASSAULT_SPREAD));
                sent++;
                if (sent >= maxAssault) break;
            }
        }
    }

    // Moves and engages any target encountered en-route
    private void orderAssault(Unit u, double x, double y) {
        u.stance = Stance.HUNT;
        u.hasHuntStation = false;
        u.returningToHuntStation = false;
        u.moveTo(x, y);
        u.huntWaypointX = x;
        u.huntWaypointY = y;
        u.hasHuntWaypoint = true;
    }

    private Unit nearestPlayerCollector(WarTech game, double fromX, double fromY) {
        Unit best = null;
        double bestSq = Double.MAX_VALUE;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER || u.type != UnitType.COLLECTOR) continue;
            double dSq = (u.x - fromX) * (u.x - fromX) + (u.y - fromY) * (u.y - fromY);
            if (dSq < bestSq) {
                bestSq = dSq;
                best = u;
            }
        }
        return best;
    }

    // Nearest live player building of type {@code a}, {@code b} or {@code c}
    private Building nearestPlayerBuilding(WarTech game, double fromX, double fromY,
                                           BuildingType a, BuildingType b, BuildingType c) {
        Building best = null;
        double bestSq = Double.MAX_VALUE;
        boolean any = a == null && b == null && c == null;
        for (int i = 0; i < game.buildingCount; i++) {
            var bl = game.buildings[i];
            if (bl.isDestroyed() || bl.faction != Faction.PLAYER) continue;
            if (!any && bl.type != a && bl.type != b && bl.type != c) continue;
            double dSq = (bl.getCenterX() - fromX) * (bl.getCenterX() - fromX) + (bl.getCenterY() - fromY) * (bl.getCenterY() - fromY);
            if (dSq < bestSq) {
                bestSq = dSq;
                best = bl;
            }
        }
        return best;
    }
}
