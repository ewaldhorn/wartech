package wartech.core;

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.combat.Effect;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public final class UnitOrders {
    private final WarTech game;

    public UnitOrders(WarTech game) {
        this.game = game;
    }

    private final Unit[] moveGroupUnits = new Unit[WarTech.MAX_UNITS];
    private final double[] slotXBuf = new double[WarTech.MAX_UNITS];
    private final double[] slotYBuf = new double[WarTech.MAX_UNITS];
    private final double[] unitDestXBuf = new double[WarTech.MAX_UNITS];
    private final double[] unitDestYBuf = new double[WarTech.MAX_UNITS];
    private final boolean[] unitAssigned = new boolean[WarTech.MAX_UNITS];

    private double orderHaloSq(double unitRadius) {
        double reach = unitRadius + 32.0 / game.camera.zoom;
        return reach * reach;
    }

    private record ResolvedTargets(Unit targetUnit, Building targetBuilding,
            Unit friendlyVehicle, Unit friendlyTroop, Building friendlyBuilding,
            Building friendlyRefinery, boolean clickedCrystals,
            double crystalTargetX, double crystalTargetY) {
    }

    public void issueRightClickOrder(double worldX, double worldY) {
        if (handlePlacementOrRally(worldX, worldY)) return;
        ResolvedTargets targets = resolveOrderTargets(worldX, worldY);
        int groupMoveCount = planFormationMove(targets, worldX, worldY);
        boolean anyCollectorOrdered = applySpecialOrders(targets, worldX, worldY);
        applyFormationMoves(groupMoveCount, anyCollectorOrdered);
    }

    private boolean handlePlacementOrRally(double worldX, double worldY) {
        if (game.placingBuilding != null) {
            game.placingBuilding = null;
            return true;
        }

        if (game.selectedBuilding != null && game.selectedBuilding.faction == Faction.PLAYER && game.selectedBuilding.type.canProduceUnits()) {
            if (game.selectedBuilding.containsWorldPoint(worldX, worldY)) {
                game.selectedBuilding.clearRallyPoint();
                game.statusToast = game.selectedBuilding.type.displayNameUpper + " RALLY POINT CLEARED";
                game.spawnEffect(Effect.TYPE_EXPLOSION, game.selectedBuilding.getCenterX(), game.selectedBuilding.getCenterY(), 0.15, 8.0);
            } else {
                double rx = Math.clamp(worldX, 16.0, game.map.width * GameMap.TILE_SIZE - 16.0);
                double ry = Math.clamp(worldY, 16.0, game.map.height * GameMap.TILE_SIZE - 16.0);
                game.selectedBuilding.setRallyPoint(rx, ry);
                game.setPrimaryBuilding(game.selectedBuilding);
                game.statusToast = game.selectedBuilding.type.displayNameUpper + " RALLY POINT SET";
                game.spawnEffect(Effect.TYPE_MUZZLE, rx, ry, 0.35, 16.0);
            }
            return true;
        }
        return false;
    }

    private ResolvedTargets resolveOrderTargets(double worldX, double worldY) {
        Unit targetUnit = findAttackTargetUnit(worldX, worldY);
        Building targetBuilding = targetUnit == null ? findAttackTargetBuilding(worldX, worldY) : null;
        Unit friendlyVehicle = findFriendlyVehicle(worldX, worldY);
        Unit friendlyTroop = findFriendlyTroop(worldX, worldY);
        Building friendlyBuilding = findFriendlyBuilding(worldX, worldY);
        Building friendlyRefinery = (targetUnit == null && targetBuilding == null)
            ? findFriendlyRefinery(worldX, worldY) : null;

        int clickTx = (int) (worldX / GameMap.TILE_SIZE);
        int clickTy = (int) (worldY / GameMap.TILE_SIZE);
        boolean clickedCrystals = false;
        double crystalTargetX = worldX;
        double crystalTargetY = worldY;

        for (int dy = -1; dy <= 1 && !clickedCrystals; dy++) {
            for (int dx = -1; dx <= 1 && !clickedCrystals; dx++) {
                int ntx = clickTx + dx;
                int nty = clickTy + dy;
                if (game.map.inBounds(ntx, nty) && game.map.crystals[game.map.index(ntx, nty)] > 0) {
                    clickedCrystals = true;
                    crystalTargetX = (ntx + 0.5) * GameMap.TILE_SIZE;
                    crystalTargetY = (nty + 0.5) * GameMap.TILE_SIZE;
                }
            }
        }

        return new ResolvedTargets(targetUnit, targetBuilding, friendlyVehicle,
            friendlyTroop, friendlyBuilding, friendlyRefinery, clickedCrystals,
            crystalTargetX, crystalTargetY);
    }

    private Unit findAttackTargetUnit(double worldX, double worldY) {
        Unit targetUnit = null;
        double bestTargetDistSq = Double.MAX_VALUE;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction == Faction.PLAYER) continue;
            if (!game.map.visibleAt(u.x, u.y)) continue;
            double distSq = (u.x - worldX) * (u.x - worldX) + (u.y - worldY) * (u.y - worldY);
            if (distSq > orderHaloSq(u.type.radius) || distSq >= bestTargetDistSq) continue;
            if (u.faction == Faction.NEUTRAL && !game.isDangerousTarget(Faction.PLAYER, u)) continue;
            bestTargetDistSq = distSq;
            targetUnit = u;
        }
        return targetUnit;
    }

    private Building findAttackTargetBuilding(double worldX, double worldY) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || !game.isRivalBuilding(Faction.PLAYER, b)) continue;
            if (!game.map.exploredAt(b.getCenterX(), b.getCenterY())) continue;
            if (b.containsWorldPoint(worldX, worldY)) {
                return b;
            }
        }
        return null;
    }

    private Unit findFriendlyVehicle(double worldX, double worldY) {
        Unit friendlyVehicle = null;
        double bestVehicleDistSq = Double.MAX_VALUE;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER || !u.type.isVehicle()) continue;
            double distSq = (u.x - worldX) * (u.x - worldX) + (u.y - worldY) * (u.y - worldY);
            if (distSq > orderHaloSq(u.type.radius) || distSq >= bestVehicleDistSq) continue;
            bestVehicleDistSq = distSq;
            friendlyVehicle = u;
        }
        return friendlyVehicle;
    }

    private Unit findFriendlyTroop(double worldX, double worldY) {
        Unit friendlyTroop = null;
        double bestTroopDistSq = Double.MAX_VALUE;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER || !u.type.isInfantry()) continue;
            double distSq = (u.x - worldX) * (u.x - worldX) + (u.y - worldY) * (u.y - worldY);
            if (distSq > orderHaloSq(u.type.radius) || distSq >= bestTroopDistSq) continue;
            bestTroopDistSq = distSq;
            friendlyTroop = u;
        }
        return friendlyTroop;
    }

    private Building findFriendlyBuilding(double worldX, double worldY) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.faction != Faction.PLAYER) continue;
            if (b.containsWorldPoint(worldX, worldY)) {
                return b;
            }
        }
        return null;
    }

    private Building findFriendlyRefinery(double worldX, double worldY) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.faction != Faction.PLAYER || b.type != BuildingType.REFINERY) continue;
            if (b.containsWorldPoint(worldX, worldY)) {
                return b;
            }
        }
        return null;
    }

    private int planFormationMove(ResolvedTargets targets, double worldX, double worldY) {
        int groupMoveCount = 0;
        double sumX = 0;
        double sumY = 0;
        double maxRadius = 0;
        for (int i = 0; i < game.unitCount && groupMoveCount < WarTech.MAX_UNITS; i++) {
            var u = game.units[i];
            if (u == null || u.isDestroyed() || !u.selected || u.faction != Faction.PLAYER) continue;

            if (u.type == UnitType.MEDIC && targets.friendlyTroop() != null && targets.friendlyTroop() != u) continue;
            if (u.type == UnitType.MECHANIC && (targets.friendlyVehicle() != null || targets.friendlyBuilding() != null)) continue;
            if (targets.targetUnit() != null && u.type != UnitType.COLLECTOR) continue;
            if (targets.targetBuilding() != null && u.type != UnitType.COLLECTOR) continue;
            if (targets.friendlyRefinery() != null && u.type == UnitType.COLLECTOR) continue;
            if (targets.clickedCrystals() && u.type == UnitType.COLLECTOR) continue;

            moveGroupUnits[groupMoveCount] = u;
            sumX += u.x;
            sumY += u.y;
            if (u.type.radius > maxRadius) maxRadius = u.type.radius;
            groupMoveCount++;
        }

        if (groupMoveCount == 1) {
            unitDestXBuf[0] = worldX;
            unitDestYBuf[0] = worldY;
        } else if (groupMoveCount > 1) {
            double avgX = sumX / groupMoveCount;
            double avgY = sumY / groupMoveCount;
            double dirX = worldX - avgX;
            double dirY = worldY - avgY;
            double dirDist = Math.sqrt(dirX * dirX + dirY * dirY);
            double fwdX = 0.0;
            double fwdY = 1.0;
            if (dirDist > 1.0) {
                fwdX = dirX / dirDist;
                fwdY = dirY / dirDist;
            }
            double rightX = -fwdY;
            double rightY = fwdX;

            double spacing = Math.max(52.0, maxRadius * 1.8);
            int cols = Math.min(groupMoveCount, Math.max(3, (int) Math.ceil(Math.sqrt(groupMoveCount * 2.0))));
            int rows = (groupMoveCount + cols - 1) / cols;

            for (int s = 0; s < groupMoveCount; s++) {
                int r = s / cols;
                int c = s % cols;
                int unitsInRow = Math.min(cols, groupMoveCount - r * cols);
                double colOffset = (c - (unitsInRow - 1) * 0.5) * spacing;
                double rowOffset = r * spacing;

                double sx = worldX + rightX * colOffset - fwdX * rowOffset;
                double sy = worldY + rightY * colOffset - fwdY * rowOffset;

                sx = Math.clamp(sx, 32.0, game.map.width * GameMap.TILE_SIZE - 32.0);
                sy = Math.clamp(sy, 32.0, game.map.height * GameMap.TILE_SIZE - 32.0);

                int stx = (int) (sx / GameMap.TILE_SIZE);
                int sty = (int) (sy / GameMap.TILE_SIZE);
                if (game.map.inBounds(stx, sty) && game.map.isTileBlocked(stx, sty, null)) {
                    sx = worldX;
                    sy = worldY;
                }

                slotXBuf[s] = sx;
                slotYBuf[s] = sy;
            }

            for (int u = 0; u < groupMoveCount; u++) {
                unitAssigned[u] = false;
            }

            for (int s = 0; s < groupMoveCount; s++) {
                double sx = slotXBuf[s];
                double sy = slotYBuf[s];
                int bestUnitIdx = -1;
                double bestDistSq = Double.MAX_VALUE;
                for (int u = 0; u < groupMoveCount; u++) {
                    if (unitAssigned[u]) continue;
                    var unit = moveGroupUnits[u];
                    double ddx = unit.x - sx;
                    double ddy = unit.y - sy;
                    double dsq = ddx * ddx + ddy * ddy;
                    if (dsq < bestDistSq) {
                        bestDistSq = dsq;
                        bestUnitIdx = u;
                    }
                }
                if (bestUnitIdx >= 0) {
                    unitAssigned[bestUnitIdx] = true;
                    unitDestXBuf[bestUnitIdx] = sx;
                    unitDestYBuf[bestUnitIdx] = sy;
                }
            }
        }
        return groupMoveCount;
    }

    private boolean applySpecialOrders(ResolvedTargets targets, double worldX, double worldY) {
        boolean anyCollectorOrdered = false;
        boolean attackPinged = false;
        boolean orderPinged = false;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u == null || u.isDestroyed() || !u.selected || u.faction != Faction.PLAYER) continue;

            if (u.type == UnitType.MEDIC && targets.friendlyTroop() != null && targets.friendlyTroop() != u) {
                u.hasHuntWaypoint = false;
                u.moveTo(targets.friendlyTroop().x, targets.friendlyTroop().y);
                game.statusToast = "MEDIC: ATTENDING INJURED TROOP";
                if (!orderPinged) {
                    orderPinged = true;
                    game.spawnEffect(Effect.TYPE_ORDER_MOVE, targets.friendlyTroop().x, targets.friendlyTroop().y, 0.5, 18.0);
                }
            } else if (u.type == UnitType.MECHANIC && (targets.friendlyVehicle() != null || targets.friendlyBuilding() != null)) {
                u.hasHuntWaypoint = false;
                if (targets.friendlyVehicle() != null && targets.friendlyVehicle().hp < targets.friendlyVehicle().maxHp) {
                    u.repairUnit(targets.friendlyVehicle());
                    u.autoRepair = true;
                    game.statusToast = "MECHANIC: REPAIRING VEHICLE";
                    if (!orderPinged) {
                        orderPinged = true;
                        game.spawnEffect(Effect.TYPE_ORDER_MOVE, targets.friendlyVehicle().x, targets.friendlyVehicle().y, 0.5, 18.0);
                    }
                } else if (targets.friendlyBuilding() != null && targets.friendlyBuilding().hp < targets.friendlyBuilding().maxHp && !targets.friendlyBuilding().constructing) {
                    u.repairBuilding(targets.friendlyBuilding());
                    u.autoRepair = true;
                    game.statusToast = "MECHANIC: REPAIRING STRUCTURE";
                    if (!orderPinged) {
                        orderPinged = true;
                        game.spawnEffect(Effect.TYPE_ORDER_MOVE, targets.friendlyBuilding().getCenterX(), targets.friendlyBuilding().getCenterY(), 0.5, 18.0);
                    }
                } else {
                    u.moveTo(worldX, worldY);
                    game.statusToast = "TARGET AT FULL INTEGRITY";
                    if (!orderPinged) {
                        orderPinged = true;
                        game.spawnEffect(Effect.TYPE_ORDER_MOVE, worldX, worldY, 0.5, 18.0);
                    }
                }
            } else if (targets.targetUnit() != null && u.type != UnitType.COLLECTOR) {
                u.hasHuntWaypoint = false;
                u.attackUnit(targets.targetUnit());
                if (u.stance == Stance.HUNT && !u.hasHuntStation) {
                    u.huntStationX = u.x;
                    u.huntStationY = u.y;
                    u.hasHuntStation = true;
                }
                game.statusToast = (u.stance == Stance.HUNT) ? "HUNTING: ENGAGING HOSTILE UNIT" : "ATTACKING HOSTILE UNIT";
                if (!attackPinged) {
                    attackPinged = true;
                    game.spawnEffect(Effect.TYPE_ORDER_ATTACK, targets.targetUnit().x, targets.targetUnit().y, 0.6, 22.0);
                }
            } else if (targets.targetBuilding() != null && u.type != UnitType.COLLECTOR) {
                u.hasHuntWaypoint = false;
                u.attackBuilding(targets.targetBuilding());
                if (u.stance == Stance.HUNT && !u.hasHuntStation) {
                    u.huntStationX = u.x;
                    u.huntStationY = u.y;
                    u.hasHuntStation = true;
                }
                game.statusToast = (u.stance == Stance.HUNT) ? "HUNTING: ENGAGING ENEMY STRUCTURE" : "ATTACKING ENEMY STRUCTURE";
                if (!attackPinged) {
                    attackPinged = true;
                    game.spawnEffect(Effect.TYPE_ORDER_ATTACK, targets.targetBuilding().getCenterX(), targets.targetBuilding().getCenterY(), 0.6, 22.0);
                }
            } else if (targets.friendlyRefinery() != null && u.type == UnitType.COLLECTOR) {
                u.hasHuntWaypoint = false;
                u.returningToRefinery = true;
                u.autoCollect = true;
                u.moveTo(targets.friendlyRefinery().getCenterX(), targets.friendlyRefinery().getCenterY() + 53.0);
                game.statusToast = "COLLECTOR RETURNING TO REFINERY";
                anyCollectorOrdered = true;
                if (!orderPinged) {
                    orderPinged = true;
                    game.spawnEffect(Effect.TYPE_ORDER_MOVE, targets.friendlyRefinery().getCenterX(), targets.friendlyRefinery().getCenterY() + 53.0, 0.5, 18.0);
                }
            } else if (targets.clickedCrystals() && u.type == UnitType.COLLECTOR) {
                u.hasHuntWaypoint = false;
                u.autoCollect = true;
                u.returningToRefinery = false;
                u.harvestTargetX = targets.crystalTargetX();
                u.harvestTargetY = targets.crystalTargetY();
                u.moveTo(targets.crystalTargetX(), targets.crystalTargetY());
                game.statusToast = "COLLECTOR HARVESTING CRYSTALS";
                anyCollectorOrdered = true;
                if (!orderPinged) {
                    orderPinged = true;
                    game.spawnEffect(Effect.TYPE_ORDER_MOVE, targets.crystalTargetX(), targets.crystalTargetY(), 0.5, 18.0);
                }
            }
        }
        return anyCollectorOrdered;
    }

    private void applyFormationMoves(int groupMoveCount, boolean anyCollectorOrdered) {
        int guardCount = 0;
        int huntCount = 0;
        for (int k = 0; k < groupMoveCount; k++) {
            var u = moveGroupUnits[k];
            if (u.type == UnitType.COLLECTOR) {
                u.autoCollect = false;
                u.returningToRefinery = false;
            } else if (u.type == UnitType.MECHANIC) {
                u.repairTargetUnit = null;
                u.repairTargetBuilding = null;
                u.isRepairing = false;
            }
            double targetCoordX = unitDestXBuf[k];
            double targetCoordY = unitDestYBuf[k];
            u.moveTo(targetCoordX, targetCoordY);
            game.spawnEffect(Effect.TYPE_ORDER_MOVE, targetCoordX, targetCoordY, 0.5, 18.0);
            if (u.stance == Stance.GUARD) {
                u.guardAnchorX = targetCoordX;
                u.guardAnchorY = targetCoordY;
                u.hasHuntWaypoint = false;
                guardCount++;
            } else if (u.stance == Stance.HUNT) {
                u.huntStationX = targetCoordX;
                u.huntStationY = targetCoordY;
                u.hasHuntStation = true;
                u.returningToHuntStation = false;
                u.huntWaypointX = targetCoordX;
                u.huntWaypointY = targetCoordY;
                u.hasHuntWaypoint = true;
                huntCount++;
            } else {
                u.hasHuntWaypoint = false;
            }
            moveGroupUnits[k] = null; 
        }

        if (!anyCollectorOrdered && groupMoveCount > 0) {
            if (guardCount == groupMoveCount) {
                game.statusToast = "MOVING TO GUARD POST";
            } else if (huntCount == groupMoveCount) {
                game.statusToast = "HUNTING [MOVE & SEARCH]";
            } else {
                game.statusToast = (groupMoveCount > 1) ? "MOVING IN FORMATION" : "MOVING TO COORDINATES";
            }
        }
    }

    public void stopSelectedUnits() {
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || !u.selected || u.faction != Faction.PLAYER) continue;
            u.stance = Stance.DEFAULT;
            u.stop();
        }
        game.statusToast = "ORDER: HALT / STOP";
    }

    public void guardSelectedUnits() {
        if (game.selectedUnit != null && game.selectedUnit.stance == Stance.GUARD) {
            stopSelectedUnits();
            return;
        }
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || !u.selected || u.faction != Faction.PLAYER) continue;
            u.stance = Stance.GUARD;
            u.guardAnchorX = u.x;
            u.guardAnchorY = u.y;
            u.hasHuntWaypoint = false;
            u.hasHuntStation = false;
            u.returningToHuntStation = false;
            u.moving = false;
            u.returningToRefinery = false;
            u.attackTargetUnit = null;
            u.attackTargetBuilding = null;
            if (u.type == UnitType.COLLECTOR) {
                u.autoCollect = false;
            }
        }
        game.statusToast = "ORDER: HOLD & GUARD [DEFENSIVE]";
    }

    public void huntSelectedUnits() {
        if (game.selectedUnit != null && game.selectedUnit.stance == Stance.HUNT) {
            stopSelectedUnits();
            return;
        }
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || !u.selected || u.faction != Faction.PLAYER) continue;
            if (u.type == UnitType.COLLECTOR) continue;
            u.stance = Stance.HUNT;
            u.huntStationX = u.x;
            u.huntStationY = u.y;
            u.hasHuntStation = true;
            u.returningToHuntStation = false;
            u.hasHuntWaypoint = false;
            game.acquireHuntTarget(u);
        }
        game.statusToast = "ORDER: HUNT [SEEK & DESTROY]";
    }

    public void sendCollectorToHarvest(Unit u) {
        if (u.isDestroyed() || u.type != UnitType.COLLECTOR) return;
        if (u.harvestRetryTimer > 0.0) return;
        u.autoCollect = true;
        u.returningToRefinery = false;

        if (u.harvestTargetX > 0 && u.harvestTargetY > 0) {
            int htx = (int) (u.harvestTargetX / GameMap.TILE_SIZE);
            int hty = (int) (u.harvestTargetY / GameMap.TILE_SIZE);
            if (game.map.inBounds(htx, hty) && game.map.crystals[game.map.index(htx, hty)] > 0) {
                u.moveTo(u.harvestTargetX, u.harvestTargetY);
                u.harvestRetryTimer = Unit.HARVEST_RETRY_INTERVAL;
                return;
            }
        }

        int bestTx = -1;
        int bestTy = -1;
        double bestDistSq = Double.MAX_VALUE;

        for (int ty = 0; ty < game.map.height; ty++) {
            for (int tx = 0; tx < game.map.width; tx++) {
                int idx = game.map.index(tx, ty);
                if (game.map.crystals[idx] > 0) {
                    double cx = (tx + 0.5) * GameMap.TILE_SIZE;
                    double cy = (ty + 0.5) * GameMap.TILE_SIZE;
                    double dSq = (cx - u.x) * (cx - u.x) + (cy - u.y) * (cy - u.y);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        bestTx = tx;
                        bestTy = ty;
                    }
                }
            }
        }

        if (bestTx >= 0) {
            u.harvestTargetX = (bestTx + 0.5) * GameMap.TILE_SIZE;
            u.harvestTargetY = (bestTy + 0.5) * GameMap.TILE_SIZE;
            u.moveTo(u.harvestTargetX, u.harvestTargetY);
            u.harvestRetryTimer = Unit.HARVEST_RETRY_INTERVAL;
            if (u.selected) game.statusToast = "COLLECTOR HARVESTING CRYSTALS";
        } else {
            if (u.crystalLoad > 0) {
                u.harvestRetryTimer = Unit.HARVEST_RETRY_INTERVAL;
                Building ref = game.findNearestRefinery(u.x, u.y, u.faction);
                if (ref != null) {
                    u.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                    u.returningToRefinery = true;
                }
            } else {
                u.autoCollect = false;
                if (u.selected) game.statusToast = "CRYSTAL FIELDS DEPLETED";
            }
        }
    }

    public void toggleCollectSelectedCollector() {
        if (game.selectedUnit == null || game.selectedUnit.type != UnitType.COLLECTOR || game.selectedUnit.faction != Faction.PLAYER) return;
        game.selectedUnit.autoCollect = true;
        if (game.selectedUnit.crystalLoad > 0) {
            Building ref = game.findNearestRefinery(game.selectedUnit.x, game.selectedUnit.y, Faction.PLAYER);
            if (ref != null) {
                game.selectedUnit.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                game.selectedUnit.returningToRefinery = true;
                game.statusToast = "COLLECTOR RETURNING TO UNLOAD";
            } else {
                game.statusToast = "NO OPERATIONAL REFINERY";
            }
        } else {
            game.selectedUnit.harvestRetryTimer = 0.0;
            sendCollectorToHarvest(game.selectedUnit);
        }
    }

    public void unloadSelectedCollector() {
        toggleCollectSelectedCollector();
    }

    public void toggleRepairSelectedMechanic() {
        if (game.selectedUnit == null || game.selectedUnit.type != UnitType.MECHANIC || game.selectedUnit.faction != Faction.PLAYER) return;
        game.selectedUnit.autoRepair = !game.selectedUnit.autoRepair;
        if (!game.selectedUnit.autoRepair) {
            game.selectedUnit.stop();
            game.selectedUnit.repairTargetUnit = null;
            game.selectedUnit.repairTargetBuilding = null;
            game.selectedUnit.isRepairing = false;
            game.statusToast = "MECHANIC: AUTO-REPAIR OFF";
        } else {
            game.statusToast = "MECHANIC: AUTO-REPAIR ON";
        }
    }
}
