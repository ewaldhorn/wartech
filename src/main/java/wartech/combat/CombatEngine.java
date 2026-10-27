package wartech.combat;

// Ballistics, targeting, splash damage, and support unit functionality

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.RepairRole;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public class CombatEngine {
    private final WarTech game;

    public CombatEngine(WarTech game) {
        this.game = game;
    }

    public boolean isDangerousTarget(Faction myFaction, Unit target) {
        if (target == null || target.isDestroyed() || !target.active) return false;
        if (target.faction == myFaction) return false;

        if (target.faction != Faction.NEUTRAL) {
            return true;
        }

        if (target.type == UnitType.SHARD_SKITTERER) {
            return true;
        }

        if (target.attackTargetUnit != null && !target.attackTargetUnit.isDestroyed() && target.attackTargetUnit.faction == myFaction) {
            return true;
        }

        if (target.attackTargetBuilding != null && !target.attackTargetBuilding.isDestroyed() && target.attackTargetBuilding.faction == myFaction) {
            return true;
        }
        
        if (target.threatTimer > 0.0) {
            return true;
        }

        return false;
    }

    public boolean isRivalBuilding(Faction myFaction, Building b) {
        if (b == null || b.isDestroyed()) return false;
        if (b.faction == myFaction || b.faction == Faction.NEUTRAL) return false;
        return true;
    }

    public Unit findDangerousUnitInRange(Unit u, double range) {
        Unit best = null;
        double bestDist = range;
        int count = game.spatialGrid.queryRange(u.x, u.y, range);
        for (int k = 0; k < count; k++) {
            int i = game.spatialGrid.queryResults[k];
            var other = game.units[i];
            if (!isDangerousTarget(u.faction, other)) continue;
            if (u.faction == Faction.PLAYER) {
                int tx = (int) (other.x / GameMap.TILE_SIZE);
                int ty = (int) (other.y / GameMap.TILE_SIZE);
                if (game.map.inBounds(tx, ty) && !game.map.visible[game.map.index(tx, ty)]) continue;
            }
            double dx = other.x - u.x;
            double dy = other.y - u.y;
            double distSq = dx * dx + dy * dy;
            if (distSq > bestDist * bestDist) continue;
            if (!game.map.hasLineOfSight(u.x, u.y, other.x, other.y, null)) continue;
            bestDist = Math.sqrt(distSq);
            best = other;
        }
        return best;
    }

    public void findNearestHostile(Unit u) {
        if (u.type.attackDamage <= 0) return;
        if (u.retreatingToRepair) return;

        Unit threat = findDangerousUnitInRange(u, u.type.attackRange);
        if (threat != null) {
            u.attackUnit(threat);
            return;
        }

        Building bestBuilding = null;
        double bestBldDist = u.type.attackRange;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (!isRivalBuilding(u.faction, b)) continue;
            if (u.faction == Faction.PLAYER) {
                int bidx = game.map.index(b.tileX, b.tileY);
                if (!game.map.visible[bidx]) continue;
            }
            double bdx = b.getCenterX() - u.x;
            double bdy = b.getCenterY() - u.y;
            double bDistSq = bdx * bdx + bdy * bdy;
            if (bDistSq > bestBldDist * bestBldDist) continue;
            if (!game.map.hasLineOfSight(u.x, u.y, b.getCenterX(), b.getCenterY(), b)) continue;
            bestBldDist = Math.sqrt(bDistSq);
            bestBuilding = b;
        }
        if (bestBuilding != null) {
            u.attackBuilding(bestBuilding);
        }
    }

    public void acquireHuntTarget(Unit u) {
        if (u.isDestroyed() || u.type.attackDamage <= 0) return;
        if (u.retreatingToRepair) return;

        Unit bestUnit = null;
        double bestDist = u.type.sightRange * 2.0;
        int count = game.spatialGrid.queryRange(u.x, u.y, bestDist);

        for (int k = 0; k < count; k++) {
            int i = game.spatialGrid.queryResults[k];
            var other = game.units[i];
            if (!isDangerousTarget(u.faction, other)) continue;
            int tx = (int) (other.x / GameMap.TILE_SIZE);
            int ty = (int) (other.y / GameMap.TILE_SIZE);
            if (!game.map.inBounds(tx, ty)) continue;
            int idx = game.map.index(tx, ty);
            if (u.faction == Faction.PLAYER && !game.map.visible[idx]) continue;

            double hdx = other.x - u.x;
            double hdy = other.y - u.y;
            double hDistSq = hdx * hdx + hdy * hdy;
            if (hDistSq > bestDist * bestDist) continue;
            if (!game.map.hasLineOfSight(u.x, u.y, other.x, other.y, null)) continue;
            bestDist = Math.sqrt(hDistSq);
            bestUnit = other;
        }

        if (bestUnit != null) {
            u.returningToHuntStation = false;
            u.attackUnit(bestUnit);
            return;
        }

        Building bestBuilding = null;
        double bestBldDist = u.type.sightRange * 2.0;
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (!isRivalBuilding(u.faction, b)) continue;
            int bidx = game.map.index(b.tileX, b.tileY);
            if (u.faction == Faction.PLAYER && !game.map.visible[bidx]) continue;

            double hbx = b.getCenterX() - u.x;
            double hby = b.getCenterY() - u.y;
            double hbDistSq = hbx * hbx + hby * hby;
            if (hbDistSq > bestBldDist * bestBldDist) continue;
            if (!game.map.hasLineOfSight(u.x, u.y, b.getCenterX(), b.getCenterY(), b)) continue;
            bestBldDist = Math.sqrt(hbDistSq);
            bestBuilding = b;
        }

        if (bestBuilding != null) {
            u.returningToHuntStation = false;
            u.attackBuilding(bestBuilding);
            return;
        }

        if (u.hasHuntWaypoint && !u.moving && u.attackTargetUnit == null && u.attackTargetBuilding == null) {
            u.targetX = u.huntWaypointX;
            u.targetY = u.huntWaypointY;
            u.moving = true;
            u.returningToHuntStation = false;
            return;
        }

        if (u.hasHuntStation && u.attackTargetUnit == null && u.attackTargetBuilding == null) {
            double stationDistSq = (u.huntStationX - u.x) * (u.huntStationX - u.x) + (u.huntStationY - u.y) * (u.huntStationY - u.y);
            if (stationDistSq > 144.0) {
                if (!u.moving || Math.abs(u.targetX - u.huntStationX) > 4.0 || Math.abs(u.targetY - u.huntStationY) > 4.0) {
                    u.targetX = u.huntStationX;
                    u.targetY = u.huntStationY;
                    u.moving = true;
                    u.returningToHuntStation = true;
                }
            } else if (u.returningToHuntStation) {
                u.moving = false;
                u.returningToHuntStation = false;
            }
        }
    }

    public void fireProjectile(Unit u, double tx, double ty) {
        boolean rocket = u.type.rocket;
        double spd = rocket ? 693.0 : 1013.0;
        if (!game.spawnProjectile(u.x, u.y, tx, ty, spd, u.type.attackDamage,
                u.faction, rocket, u.type.splashRadius, null)) {
            return;
        }
        u.reloadCooldown = u.type.attackCooldown;
        if (u.faction == Faction.NEUTRAL) {
            u.threatTimer = 10.0;
        }
        game.spawnEffect(Effect.TYPE_MUZZLE, u.x, u.y, 0.1, 11.0);
    }

    public void updateMechanic(Unit u, double dt) {
        if (u.repairer == null || u.isDestroyed()) return;
        var r = u.repairer;

        if (r.isRepairing) {
            boolean targetValid = false;
            int missingHp = 0;
            double targetCenterX = u.x;
            double targetCenterY = u.y;

            if (r.repairTargetUnit != null) {
                if (!r.repairTargetUnit.isDestroyed() && r.repairTargetUnit.type.isVehicle() && r.repairTargetUnit.hp < r.repairTargetUnit.maxHp) {
                    targetValid = true;
                    missingHp = r.repairTargetUnit.maxHp - r.repairTargetUnit.hp;
                    targetCenterX = r.repairTargetUnit.x;
                    targetCenterY = r.repairTargetUnit.y;
                } else {
                    r.repairTargetUnit = null;
                }
            } else if (r.repairTargetBuilding != null) {
                if (!r.repairTargetBuilding.isDestroyed() && !r.repairTargetBuilding.constructing && r.repairTargetBuilding.hp < r.repairTargetBuilding.maxHp) {
                    targetValid = true;
                    missingHp = r.repairTargetBuilding.maxHp - r.repairTargetBuilding.hp;
                    targetCenterX = r.repairTargetBuilding.getCenterX();
                    targetCenterY = r.repairTargetBuilding.getCenterY();
                } else {
                    r.repairTargetBuilding = null;
                }
            }

            if (targetValid) {
                if (r.repairTargetUnit != null) {
                    double rdx = r.repairTargetUnit.x - u.x;
                    double rdy = r.repairTargetUnit.y - u.y;
                    if (Math.sqrt(rdx * rdx + rdy * rdy) > RepairRole.REPAIR_UNIT_RANGE) {
                        r.isRepairing = false;
                        u.moving = true;
                        targetValid = false;
                    }
                } else if (r.repairTargetBuilding != null) {
                    double minBx = (double) r.repairTargetBuilding.pixelX;
                    double maxBx = (double) (r.repairTargetBuilding.pixelX + r.repairTargetBuilding.type.widthTiles * GameMap.TILE_SIZE);
                    double minBy = (double) r.repairTargetBuilding.pixelY;
                    double maxBy = (double) (r.repairTargetBuilding.pixelY + r.repairTargetBuilding.type.heightTiles * GameMap.TILE_SIZE);
                    double ebdx = Math.clamp(u.x, minBx, maxBx) - u.x;
                    double ebdy = Math.clamp(u.y, minBy, maxBy) - u.y;
                    if (Math.sqrt(ebdx * ebdx + ebdy * ebdy) > RepairRole.REPAIR_BUILDING_RANGE) {
                        r.isRepairing = false;
                        u.moving = true;
                        targetValid = false;
                    }
                }
            }

            if (!targetValid) {
                r.isRepairing = false;
                r.repairHpCarry = 0.0;
            } else {
                int availableCredits = (u.faction == Faction.PLAYER) ? game.playerCredits : game.enemyAI.credits;

                if (r.repairCreditAccumulator < 1.0) {
                    if (availableCredits >= 1) {
                        if (u.faction == Faction.PLAYER) {
                            game.playerCredits -= 1;
                        } else {
                            game.enemyAI.credits -= 1;
                        }
                        r.repairCreditAccumulator += 10.0;
                    } else {
                        if (u.selected && u.faction == Faction.PLAYER) {
                            game.statusToast = "REPAIRS ON HOLD - INSUFFICIENT CREDITS";
                        }
                    }
                }

                if (r.repairCreditAccumulator >= 1.0) {
                    double hpToHeal = Math.min((double) missingHp,
                        Math.min(r.repairCreditAccumulator, 60.0 * dt + r.repairHpCarry));
                    int healInt = (int) hpToHeal;
                    r.repairHpCarry = hpToHeal - healInt;

                    if (healInt > 0) {
                        if (r.repairTargetUnit != null) {
                            r.repairTargetUnit.hp = Math.min(r.repairTargetUnit.maxHp, r.repairTargetUnit.hp + healInt);
                            if (r.repairTargetUnit.hp >= r.repairTargetUnit.maxHp) {
                                if (u.faction == Faction.PLAYER && game.campaignManager != null) {
                                    game.campaignManager.onVehicleRepaired(r.repairTargetUnit.type);
                                }
                                r.repairTargetUnit = null;
                                r.isRepairing = false;
                                r.repairHpCarry = 0.0;
                            }
                        } else if (r.repairTargetBuilding != null) {
                            r.repairTargetBuilding.hp = Math.min(r.repairTargetBuilding.maxHp, r.repairTargetBuilding.hp + healInt);
                            if (r.repairTargetBuilding.hp >= r.repairTargetBuilding.maxHp) {
                                r.repairTargetBuilding = null;
                                r.isRepairing = false;
                                r.repairHpCarry = 0.0;
                            }
                        }
                        r.repairCreditAccumulator = Math.max(0.0, r.repairCreditAccumulator - healInt);
                        if (game.fxRng.chance(0.3)) {
                            game.spawnEffect(Effect.TYPE_SPARK,
                                targetCenterX + game.fxRng.range(-8.0, 8.0),
                                targetCenterY + game.fxRng.range(-8.0, 8.0), 0.2, 10.0);
                        }
                    }
                }
            }
        }

        if (r.autoRepair && !r.isRepairing && !u.moving && r.repairTargetUnit == null && r.repairTargetBuilding == null) {
            Unit bestUnit = null;
            double bestDistSq = Double.MAX_VALUE;

            for (int vi = 0; vi < game.unitCount; vi++) {
                var other = game.units[vi];
                if (other.isDestroyed() || other.faction != u.faction) continue;
                if (other.type.isVehicle() && other.hp < other.maxHp
                        && game.withinBaseLeash(other.x, other.y, u.faction)) {
                    double dSq = (other.x - u.x) * (other.x - u.x) + (other.y - u.y) * (other.y - u.y);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        bestUnit = other;
                    }
                }
            }

            Building bestBuilding = null;
            for (int bi = 0; bi < game.buildingCount; bi++) {
                var b = game.buildings[bi];
                if (b.isDestroyed() || b.faction != u.faction || b.constructing) continue;
                if (b.hp < b.maxHp && game.withinBaseLeash(b.getCenterX(), b.getCenterY(), u.faction)) {
                    double dSq = (b.getCenterX() - u.x) * (b.getCenterX() - u.x) + (b.getCenterY() - u.y) * (b.getCenterY() - u.y);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        bestBuilding = b;
                        bestUnit = null;
                    }
                }
            }

            if (bestUnit != null) {
                u.repairUnit(bestUnit);
                r.autoRepairJob = true;
            } else if (bestBuilding != null) {
                u.repairBuilding(bestBuilding);
                r.autoRepairJob = true;
            }
        }

        if (r.autoRepair && r.autoRepairJob && !r.isRepairing) {
            boolean jobValid = false;
            if (r.repairTargetUnit != null && !r.repairTargetUnit.isDestroyed()) {
                jobValid = game.withinBaseLeash(r.repairTargetUnit.x, r.repairTargetUnit.y, u.faction);
            } else if (r.repairTargetBuilding != null && !r.repairTargetBuilding.isDestroyed()) {
                jobValid = game.withinBaseLeash(r.repairTargetBuilding.getCenterX(), r.repairTargetBuilding.getCenterY(), u.faction);
            }
            if (!jobValid) {
                r.repairTargetUnit = null;
                r.repairTargetBuilding = null;
                r.autoRepairJob = false;
                u.moving = false;
            }
        }
        if (r.autoRepair && !r.isRepairing && !u.moving
                && r.repairTargetUnit == null && r.repairTargetBuilding == null
                && !game.withinBaseLeash(u.x, u.y, u.faction)) {
            Building home = game.findNearestBuilding(u.x, u.y, u.faction);
            if (home != null) {
                u.moveTo(home.getCenterX(), home.getCenterY() + 140.0);
            }
        }
    }

    public void updateMedic(Unit u, double dt) {
        if (u.type != UnitType.MEDIC || u.isDestroyed()) return;

        u.healTimer += dt;
        if (u.healTimer >= 30.0 / 60.0 - 1e-9) {
            u.healTimer -= 30.0 / 60.0;
            double healRadiusSq = 160.0 * 160.0;
            for (int vi = 0; vi < game.unitCount; vi++) {
                var other = game.units[vi];
                if (other.isDestroyed() || other.faction != u.faction) continue;
                if (other.type.isInfantry() && other.hp < other.maxHp) {
                    double dSq = (other.x - u.x) * (other.x - u.x) + (other.y - u.y) * (other.y - u.y);
                    if (dSq <= healRadiusSq) {
                        other.hp = Math.min(other.maxHp, other.hp + 5);
                        game.spawnEffect(Effect.TYPE_HEAL, other.x, other.y, 0.4, 12.0);
                    }
                }
            }
        }

        if (!u.moving && u.attackTargetUnit == null && u.attackTargetBuilding == null) {
            Unit injuredTroop = null;
            double bestDistSq = u.type.sightRange * u.type.sightRange;
            for (int vi = 0; vi < game.unitCount; vi++) {
                var other = game.units[vi];
                if (other.isDestroyed() || other.faction != u.faction || other == u) continue;
                if (other.type.isInfantry() && other.hp < other.maxHp) {
                    double dSq = (other.x - u.x) * (other.x - u.x) + (other.y - u.y) * (other.y - u.y);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        injuredTroop = other;
                    }
                }
            }
            if (injuredTroop != null && bestDistSq > 90.0 * 90.0) {
                u.moveTo(injuredTroop.x + game.simRng.range(-15.0, 15.0),
                    injuredTroop.y + game.simRng.range(-15.0, 15.0));
            }
        }
    }

    public void updateDefenseTurrets(double dt) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.constructing) continue;
            if (b.type == BuildingType.TURRET) {
                if (b.reloadCooldown > 0) b.reloadCooldown -= dt;
                if (b.reloadCooldown <= 0) {
                    Unit target = findTurretTarget(b, 320.0);
                    if (target != null) {
                        b.turretAngle = Math.atan2(target.y - b.getCenterY(), target.x - b.getCenterX());
                        if (game.spawnProjectile(b.getCenterX(), b.getCenterY(),
                                target.x, target.y, 933.0, 30, b.faction, false, 0.0, b)) {
                            b.reloadCooldown = 0.9;
                        }
                    }
                }
            }
        }
    }

    public Unit findTurretTarget(Building b, double range) {
        Unit best = null;
        double bestDistSq = range * range;
        int count = game.spatialGrid.queryRange(b.getCenterX(), b.getCenterY(), range);
        for (int k = 0; k < count; k++) {
            int uIdx = game.spatialGrid.queryResults[k];
            var u = game.units[uIdx];
            if (u.isDestroyed() || !isDangerousTarget(b.faction, u)) continue;
            double dx = u.x - b.getCenterX();
            double dy = u.y - b.getCenterY();
            double distSq = dx * dx + dy * dy;
            if (distSq > bestDistSq) continue;
            if (b.faction == Faction.PLAYER) {
                int tx = (int) (u.x / GameMap.TILE_SIZE);
                int ty = (int) (u.y / GameMap.TILE_SIZE);
                if (game.map.inBounds(tx, ty) && !game.map.visible[game.map.index(tx, ty)]) continue;
            }
            if (!game.map.hasLineOfSight(b.getCenterX(), b.getCenterY(), u.x, u.y, b)) continue;
            bestDistSq = distSq;
            best = u;
        }
        return best;
    }

    public void updateProjectiles(double dt) {
        for (int i = game.projectileCount - 1; i >= 0; i--) {
            var p = game.projectiles[i];
            boolean hit = p.update(dt, game.map);
            if (hit) {
                game.spawnEffect(Effect.TYPE_EXPLOSION, p.x, p.y, 0.25, 16.0);
                applyImpactDamage(p);
                game.projectileCount--;
                game.projectiles[i] = game.projectiles[game.projectileCount];
                game.projectiles[game.projectileCount] = p;
            }
        }
    }

    public void applyImpactDamage(Projectile p) {
        Unit directVictim = null;
        double bestDistSq = Double.MAX_VALUE;
        int count = game.spatialGrid.queryRange(p.x, p.y, 64.0);
        for (int k = 0; k < count; k++) {
            int i = game.spatialGrid.queryResults[k];
            var u = game.units[i];
            if (u.isDestroyed() || u.faction == p.faction) continue;
            double distSq = (u.x - p.x) * (u.x - p.x) + (u.y - p.y) * (u.y - p.y);
            double reach = u.type.radius + 16.0;
            if (distSq > reach * reach || distSq >= bestDistSq) continue;
            if (!game.map.hasLineOfSight(p.x, p.y, u.x, u.y, null)) continue;
            bestDistSq = distSq;
            directVictim = u;
        }
        if (directVictim != null) {
            damageUnit(directVictim, p.damage, p.faction);
        } else {
            for (int i = 0; i < game.buildingCount; i++) {
                var b = game.buildings[i];
                if (b.isDestroyed() || b.faction == p.faction) continue;
                if (b.containsWorldPoint(p.x, p.y)) {
                    game.damageBuilding(b, p.damage, p.faction);
                    break;
                }
            }
        }
        if (p.splash > 0.0) {
            applySplash(p.x, p.y, p.splash, p.damage / 2, p.faction, directVictim);
        }
    }

    private void damageUnit(Unit u, int dmg, Faction from) {
        u.takeDamage(dmg, from);
        if ((u.faction == Faction.NEUTRAL || u.type == UnitType.MEDIC) && !u.isDestroyed()) {
            u.threatTimer = Math.max(u.threatTimer, 10.0);
        }
    }

    public void applySplash(double x, double y, double radius, int dmg, Faction fromFaction, Unit directVictim) {
        if (dmg < 1) dmg = 1;

        int count = game.spatialGrid.queryRange(x, y, radius + 40.0);
        for (int k = 0; k < count; k++) {
            int i = game.spatialGrid.queryResults[k];
            var u = game.units[i];
            if (u.isDestroyed() || u.faction == fromFaction || u == directVictim) continue;
            double distSq = (u.x - x) * (u.x - x) + (u.y - y) * (u.y - y);
            double reach = radius + u.type.radius;
            if (distSq > reach * reach) continue;
            if (!game.map.hasLineOfSight(x, y, u.x, u.y, null)) continue;
            damageUnit(u, dmg, fromFaction);
        }
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.faction == fromFaction) continue;
            double cx = Math.clamp(x, b.pixelX, b.pixelX + b.type.widthTiles * GameMap.TILE_SIZE);
            double cy = Math.clamp(y, b.pixelY, b.pixelY + b.type.heightTiles * GameMap.TILE_SIZE);
            double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy);
            if (distSq > radius * radius) continue;
            if (!game.map.hasLineOfSight(x, y, cx, cy, b)) continue;
            game.damageBuilding(b, dmg, fromFaction);
        }

        if (game.threatSystem != null) {
            game.threatSystem.breachGeodesInRadius(x, y, radius);
        }
    }
}
