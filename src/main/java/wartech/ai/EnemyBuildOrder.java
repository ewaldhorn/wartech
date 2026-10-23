package wartech.ai;

// Decides which base structure the enemy builds next and where to put it

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;

public final class EnemyBuildOrder {
    private final EnemyAI ai;

    public EnemyBuildOrder(EnemyAI ai) {
        this.ai = ai;
    }

    /**
     * Starts building one building per think cycle, provided there's room for it. If there's no
     * room, the next one in the list is picked etc. etc.
     * 
     * It's possible for buildings to be on the backlog and only get completed when the credits
     * for them are there, so the AI could have two or three structures under construction.
     */
    public void update(AiCensus c, GameMap map, WarTech game) {
        if (EnemyAI.findEnemyBuilding(BuildingType.CON_YARD, game) == null) {
            if (c.conYards == 0 && ai.credits > 0) tryBuild(BuildingType.CON_YARD, map, game);
            return;
        }
        if (c.foundations > 0) return;

        Difficulty diff = ai.difficulty;
        AiTuning.Tier tier = ai.tuning();
        int credits = ai.credits;
        int maxTurrets = tier.maxTurrets();
        int maxRefineries = tier.maxRefineries();
        int powerThreshold = tier.powerThreshold();
        int turretReserve = tier.turretReserve();
        boolean refineryOnline = EnemyAI.findEnemyBuilding(BuildingType.REFINERY, game) != null;
        boolean barracksOnline = EnemyAI.findEnemyBuilding(BuildingType.BARRACKS, game) != null;
        boolean factoryOnline = EnemyAI.findEnemyBuilding(BuildingType.HEAVY_FACTORY, game) != null;
        boolean veteranPlus = diff.atLeast(Difficulty.MEDIUM);

        boolean placed = false;
        // Check for power need first, without it, the economy is halved
        if (!placed && c.powerNet + c.pendingTurbinePower < powerThreshold && credits > 0) {
            placed = tryBuild(BuildingType.WIND_TURBINE, map, game);
        }
        // Need refinery?
        if (!placed && c.refineries < maxRefineries && credits > 0) {
            placed = tryBuild(BuildingType.REFINERY, map, game);
        }
        // Need barracks?
        if (!placed && c.barracks == 0 && refineryOnline && credits > 0) {
            placed = tryBuild(BuildingType.BARRACKS, map, game);
        }
        // Need heavy war factory?
        if (!placed && c.heavyFactories == 0 && refineryOnline && credits > 0) {
            placed = tryBuild(BuildingType.HEAVY_FACTORY, map, game);
        }
        // Need light vehicle factory? (Medium & Hard)
        if (!placed && veteranPlus && c.lightFactories == 0 && refineryOnline && credits > 0) {
            placed = tryBuild(BuildingType.LIGHT_FACTORY, map, game);
        }
        // Need vehicle repair facility? (Medium & Hard: 1 facility)
        if (!placed && veteranPlus && c.repairFacilities == 0 && refineryOnline && factoryOnline && credits > 0) {
            placed = tryBuild(BuildingType.REPAIR_FACILITY, map, game);
        }
        // Defense turrets (requires Barracks prerequisite)
        if (!placed && c.turrets < maxTurrets && barracksOnline && credits >= BuildingType.TURRET.cost + turretReserve) {
            placed = tryBuild(BuildingType.TURRET, map, game);
        }
        // With cash in the bank and power to cover one more turret without browning out, keep building past the baseline.
        if (!placed && c.turrets < maxTurrets + AiTuning.SURPLUS_TURRETS && barracksOnline
                && credits >= AiTuning.SURPLUS_TURRET_BANK
                && c.powerNet + c.pendingTurbinePower + BuildingType.TURRET.powerNet >= 0) {
            placed = tryBuild(BuildingType.TURRET, map, game);
        }
    }

    // Tries to lay the foundations for a new structure, can return false when it couldn't find enough room.
    private boolean tryBuild(BuildingType type, GameMap map, WarTech game) {
        if (game.buildingCount >= WarTech.MAX_BUILDINGS) return false;
        int startX = map.width - AiTuning.BUILD_AREA_FROM_EAST_TILES;
        int startY = AiTuning.BUILD_AREA_TOP_ROW;
        for (int ty = startY; ty < startY + AiTuning.BUILD_AREA_HEIGHT_TILES; ty++) {
            for (int tx = startX; tx < startX + AiTuning.BUILD_AREA_WIDTH_TILES; tx++) {
                if (map.canBuildAt(tx, ty, type, false, game.buildings, game.buildingCount, game.units, game.unitCount)) {
                    var b = new Building(type, Faction.ENEMY, tx, ty);
                    b.constructing = true;
                    b.buildSpent = 0;
                    b.constructionProgress = 0.0;
                    b.hp = Math.max(AiTuning.FOUNDATION_MIN_HP, b.maxHp / AiTuning.FOUNDATION_HP_DIVISOR);
                    if (game.addBuilding(b)) {
                        map.updateInfluence(game.buildings, game.buildingCount);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
