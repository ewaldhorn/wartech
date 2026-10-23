package wartech.building;

// Economy, construction queues, building placement, and refund logic

import wartech.combat.Effect;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.UnitType;

public class ProductionSystem {
    private final WarTech game;

    public ProductionSystem(WarTech game) {
        this.game = game;
    }

    public boolean canProduce(BuildingType bt) {
        if (bt == null) return false;
        if (game.campaignManager != null && !game.campaignManager.isBuildingAllowed(bt)) {
            return false;
        }

        if (bt != BuildingType.CON_YARD && findBuilding(BuildingType.CON_YARD, Faction.PLAYER) == null) {
            return false;
        }
        BuildingType req = bt.getRequiredBuilding();
        if (req != null) {
            return findBuilding(req, Faction.PLAYER) != null;
        }
        return true;
    }

    public boolean canProduce(UnitType ut) {
        if (ut == null) return false;
        if (!ut.isAvailableTo(Faction.PLAYER)) return false;
        if (game.campaignManager != null && !game.campaignManager.isUnitAllowed(ut)) {
            return false;
        }
        if (ut == UnitType.COLLECTOR) {
            return findBuilding(BuildingType.REFINERY, Faction.PLAYER) != null ||
                   findBuilding(BuildingType.HEAVY_FACTORY, Faction.PLAYER) != null;
        }
        if (ut.requiredBuilding == null) return false;
        return findBuilding(ut.requiredBuilding, Faction.PLAYER) != null;
    }

    public BuildingType getAvailableBuilding(int targetSlot) {
        int currentSlot = 0;
        for (int i = 0; i < WarTech.BUILD_ORDER.length; i++) {
            var bt = WarTech.BUILD_ORDER[i];
            if (canProduce(bt)) {
                if (currentSlot == targetSlot) {
                    return bt;
                }
                currentSlot++;
            }
        }
        return null;
    }

    public UnitType getAvailableUnit(int targetSlot) {
        int currentSlot = 0;
        for (int i = 0; i < WarTech.UNIT_ORDER.length; i++) {
            var ut = WarTech.UNIT_ORDER[i];
            if (canProduce(ut)) {
                if (currentSlot == targetSlot) {
                    return ut;
                }
                currentSlot++;
            }
        }
        return null;
    }

    public void trainUnit(UnitType ut) {
        if (!canProduce(ut)) {
            game.statusToast = (ut != null && ut.requiredBuilding != null)
                ? "REQUIRES " + ut.requiredBuilding.displayNameUpper
                : "CANNOT TRAIN";
            return;
        }

        if (ut == UnitType.MECHANIC) {
            int currentMechanics = countUnits(UnitType.MECHANIC, Faction.PLAYER) + countQueuedUnits(UnitType.MECHANIC, Faction.PLAYER);
            int maxAllowed = countBuildings(BuildingType.REPAIR_FACILITY, Faction.PLAYER) * 2;
            if (currentMechanics >= maxAllowed) {
                game.statusToast = "MAX MECHANICS REACHED (2 PER FACILITY)";
                return;
            }
        }

        if (game.unitQueueCount >= WarTech.MAX_UNIT_QUEUE) {
            game.statusToast = "PRODUCTION QUEUE FULL";
            return;
        }

        game.unitQueue[game.unitQueueCount++] = ut;
        if (game.unitQueueCount == 1) {
            game.unitBuildSpent = 0;
            game.statusToast = "BUILDING " + ut.displayNameUpper;
        } else {
            game.statusToast = ut.displayNameUpper + " QUEUED (" + game.unitQueueCount + ")";
        }
    }

    public void cancelUnit(UnitType ut) {
        if (game.unitQueueCount == 0) return;
        int foundIdx = -1;
        for (int i = game.unitQueueCount - 1; i >= 0; i--) {
            if (game.unitQueue[i] == ut) {
                foundIdx = i;
                break;
            }
        }
        if (foundIdx == -1) return;

        if (foundIdx == 0) {
            game.playerCredits += game.unitBuildSpent;
            game.unitBuildSpent = 0;
        }

        for (int i = foundIdx; i < game.unitQueueCount - 1; i++) {
            game.unitQueue[i] = game.unitQueue[i + 1];
        }
        game.unitQueueCount--;
        game.statusToast = ut.displayNameUpper + " CANCELLED";
    }

    public boolean spawnPlayerUnit(UnitType ut) {
        Building factory = findProductionBuilding(ut.requiredBuilding, Faction.PLAYER);
        if (ut == UnitType.COLLECTOR && factory == null) {
            factory = findProductionBuilding(BuildingType.HEAVY_FACTORY, Faction.PLAYER);
        }
        if (factory == null) {
            game.playerCredits += game.unitBuildSpent;
            game.unitBuildSpent = 0;
            game.statusToast = ut.displayNameUpper + " LOST - NO PRODUCTION FACILITY (REFUNDED)";
            return false;
        }

        var trained = factory.spawnUnit(ut, game.map);
        double spawnX = trained.x;
        double spawnY = trained.y;
        switch (ut) {
            case COLLECTOR -> trained.autoCollect = true;
            case MECHANIC -> trained.autoRepair = true;
            case null, default -> { }
        }
        if (!game.addUnit(trained)) {
            game.playerCredits += game.unitBuildSpent;
            game.unitBuildSpent = 0;
            game.statusToast = ut.displayNameUpper + " LOST - UNIT CAPACITY FULL (REFUNDED)";
            return false;
        }
        game.spawnEffect(Effect.TYPE_MUZZLE, spawnX, spawnY, 0.25, 21.0);

        if (factory.hasRallyPoint) {
            double scatterX = factory.rallyX + game.simRng.range(-12.0, 12.0);
            double scatterY = factory.rallyY + game.simRng.range(-12.0, 12.0);
            scatterX = Math.clamp(scatterX, 16.0, game.map.width * GameMap.TILE_SIZE - 16.0);
            scatterY = Math.clamp(scatterY, 16.0, game.map.height * GameMap.TILE_SIZE - 16.0);

            if (ut == UnitType.COLLECTOR) {
                int rx = (int) (factory.rallyX / GameMap.TILE_SIZE);
                int ry = (int) (factory.rallyY / GameMap.TILE_SIZE);
                if (game.map.inBounds(rx, ry) && game.map.crystals[game.map.index(rx, ry)] > 0) {
                    trained.harvestTargetX = factory.rallyX;
                    trained.harvestTargetY = factory.rallyY;
                }
            }
            trained.moveTo(scatterX, scatterY);
        }
        return true;
    }

    public void updateConstruction(int ticks) {
        if (ticks <= 0) return;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.faction == Faction.PLAYER && b.constructing && !b.isDestroyed()) {
                int needed = b.type.cost - b.buildSpent;
                int spend = Math.min(ticks, Math.min(game.playerCredits, needed));
                if (spend > 0) {
                    game.playerCredits -= spend;
                    b.buildSpent += spend;
                    b.constructionProgress = (double) b.buildSpent / b.type.cost;
                    b.hp = (int) (b.maxHp * (0.1 + 0.9 * b.constructionProgress));
                }

                if (b.buildSpent >= b.type.cost) {
                    b.constructing = false;
                    b.constructionProgress = 1.0;
                    b.hp = b.maxHp;
                    game.statusToast = b.type.displayNameUpper + " READY";
                    game.map.updateInfluence(game.buildings, game.buildingCount);

                    if (b.type == BuildingType.REFINERY) {
                        if (game.unitCount < WarTech.MAX_UNITS) {
                            var col = b.spawnUnit(UnitType.COLLECTOR, game.map);
                            col.autoCollect = true;
                            game.addUnit(col);
                        } else {
                            game.statusToast = "UNIT CAPACITY FULL - COLLECTOR NOT DEPLOYED";
                            game.capacityNotice = WarTech.NOTICE_COLLECTOR_SKIPPED;
                        }
                    }
                }
                break; 
            }
        }

        if (game.unitQueueCount > 0) {
            var current = game.unitQueue[0];
            if (!canProduce(current)) {
                game.playerCredits += game.unitBuildSpent; 
                game.unitBuildSpent = 0;
                game.statusToast = current.displayNameUpper + " CANCELLED - FACILITY LOST";
                for (int q = 0; q < game.unitQueueCount - 1; q++) {
                    game.unitQueue[q] = game.unitQueue[q + 1];
                }
                game.unitQueueCount--;
            } else if (game.unitCount >= WarTech.MAX_UNITS) {
                game.statusToast = "UNIT CAPACITY FULL - PRODUCTION HOLD";
                game.capacityNotice = WarTech.NOTICE_UNIT_CAP;
            } else {
                int needed = current.cost - game.unitBuildSpent;
                int spend = Math.min(ticks, Math.min(game.playerCredits, needed));
                if (spend > 0) {
                    game.playerCredits -= spend;
                    game.unitBuildSpent += spend;
                }

                if (game.unitBuildSpent >= current.cost) {
                    if (spawnPlayerUnit(current)) {
                        game.statusToast = current.displayNameUpper + " READY";
                    }
                    for (int q = 0; q < game.unitQueueCount - 1; q++) {
                        game.unitQueue[q] = game.unitQueue[q + 1];
                    }
                    game.unitQueueCount--;
                    game.unitBuildSpent = 0;
                }
            }
        }
    }

    public void sellSelectedBuilding() {
        if (game.selectedBuilding == null || game.selectedBuilding.faction != Faction.PLAYER) return;
        int refund = game.selectedBuilding.constructing ? (game.selectedBuilding.buildSpent / 2) : (game.selectedBuilding.type.cost / 2);
        if (game.playerCredits + refund > WarTech.MAX_CREDITS) {
            game.statusToast = "STORE FULL - CANNOT ACCEPT SALE";
            return;
        }
        game.addPlayerCredits(refund);
        game.selectedBuilding.hp = 0;
        game.selectedBuilding.active = false;
        game.spawnEffect(Effect.TYPE_EXPLOSION, game.selectedBuilding.getCenterX(), game.selectedBuilding.getCenterY(), 0.35, 18.0);
        game.statusToast = "SOLD " + game.selectedBuilding.type.displayNameUpper + " (+$" + refund + ")";
        game.selectedBuilding = null;
        game.compactBuildings();
    }

    public void repairSelectedBuilding() {
        if (game.selectedBuilding == null || game.selectedBuilding.faction != Faction.PLAYER) return;
        if (game.selectedBuilding.constructing) {
            game.statusToast = "STRUCTURE UNDER CONSTRUCTION";
            return;
        }
        if (game.selectedBuilding.hp >= game.selectedBuilding.maxHp) {
            game.statusToast = "STRUCTURE AT FULL INTEGRITY";
            return;
        }
        int cost = 25;
        if (game.playerCredits < cost) {
            game.statusToast = "INSUFFICIENT CREDITS ($" + cost + ")";
            return;
        }
        game.playerCredits -= cost;
        game.selectedBuilding.hp = Math.min(game.selectedBuilding.maxHp, game.selectedBuilding.hp + 120);
        game.spawnEffect(Effect.TYPE_SPARK, game.selectedBuilding.getCenterX(), game.selectedBuilding.getCenterY(), 0.15, 8.0);
        game.statusToast = "REPAIRED (+120 HP)";
    }

    public Building findProductionBuilding(BuildingType type, Faction faction) {
        if (type == null) return null;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.type == type && b.faction == faction && !b.isDestroyed() && !b.constructing && b.isPrimary) {
                return b;
            }
        }
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.type == type && b.faction == faction && !b.isDestroyed() && !b.constructing && b.hasRallyPoint) {
                return b;
            }
        }
        return findBuilding(type, faction);
    }

    public void setPrimaryBuilding(Building target) {
        if (target == null) return;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.type == target.type && b.faction == target.faction) {
                b.isPrimary = (b == target);
            }
        }
    }

    public Building findBuilding(BuildingType type, Faction faction) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.type == type && b.faction == faction && !b.isDestroyed() && !b.constructing) {
                return b;
            }
        }
        return null;
    }

    public int countBuildings(BuildingType type, Faction faction) {
        int count = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.type == type && b.faction == faction && !b.isDestroyed() && !b.constructing) {
                count++;
            }
        }
        return count;
    }

    public int countUnits(UnitType type, Faction faction) {
        int count = 0;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.type == type && u.faction == faction && !u.isDestroyed()) {
                count++;
            }
        }
        return count;
    }

    public int countQueuedUnits(UnitType type, Faction faction) {
        if (faction != Faction.PLAYER) return 0;
        int count = 0;
        for (int i = 0; i < game.unitQueueCount; i++) {
            if (game.unitQueue[i] == type) {
                count++;
            }
        }
        return count;
    }
}
