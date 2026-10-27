package wartech.unit;

// Mobile unit entity with a basic state machines, stances and lifecycle
// All angles are in radians

import wartech.building.Building;
import wartech.core.Faction;
import wartech.map.GameMap;

public final class Unit {
    private static int nextId = 1;

    public final int id;
    public final UnitType type;
    public final Faction faction;

    public double x;
    public double y;
    public double angle;
    public double turretAngle;

    public int hp;
    public final int maxHp;
    public boolean active = true;
    public boolean selected = false;

    public static final int GROUP_NONE = 0;
    public static final int GROUP_MAX = 9;

    public int group = GROUP_NONE;

    public static final double CLIFF_BARRIER_MARGIN = 3.5;
    public static final double MAX_OBSTACLE_CLEARANCE = 28.0;

    public double getObstacleClearance() {
        return obstacleClearanceFor(type);
    }

    public static double obstacleClearanceFor(UnitType unitType) {
        return Math.min(unitType.radius + CLIFF_BARRIER_MARGIN, MAX_OBSTACLE_CLEARANCE);
    }

    public boolean isWideChassis() {
        return type.radius * 2.0 > GameMap.TILE_SIZE - 8.0;
    }

    public Stance stance = Stance.DEFAULT;
    public double guardAnchorX = 0.0;
    public double guardAnchorY = 0.0;
    public double huntWaypointX = 0.0;
    public double huntWaypointY = 0.0;
    public boolean hasHuntWaypoint = false;
    public double huntStationX = 0.0;
    public double huntStationY = 0.0;
    public boolean hasHuntStation = false;
    public boolean returningToHuntStation = false;
    public boolean isBaseDefender = false;
    public boolean retreatingToRepair = false;
    public int retreatRetries = 0;

    public boolean isRepairing() {
        return repairer != null && repairer.isRepairing;
    }

    public double targetX;
    public double targetY;
    public boolean moving = false;

    public final NavigationState nav = new NavigationState();

    public boolean isYielding() {
        return nav.yielding;
    }

    public Unit attackTargetUnit = null;
    public Building attackTargetBuilding = null;
    public double reloadCooldown = 0.0;
    public double threatTimer = 0.0;

    public final HarvesterRole harvester;
    public final ScavengerRole scavenger;
    public final RepairRole repairer;

    public boolean isHarvester() {
        return harvester != null;
    }

    public boolean isScavenger() {
        return scavenger != null;
    }

    public boolean isRepairer() {
        return repairer != null;
    }

    public double regenTimer = 0.0;
    public double healTimer = 0.0;

    public Unit(UnitType type, Faction faction, double x, double y) {
        this.id = nextId++;
        this.type = type;
        this.faction = faction;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.angle = 0.0;
        this.turretAngle = 0.0;
        this.maxHp = type.maxHp;
        this.hp = type.maxHp;
        this.harvester = (type == UnitType.COLLECTOR) ? new HarvesterRole() : null;
        this.scavenger = (type == UnitType.SCAVENGER_DRONE) ? new ScavengerRole() : null;
        this.repairer = (type == UnitType.MECHANIC) ? new RepairRole() : null;
        if (type.isMilitary()) {
            this.stance = Stance.GUARD;
            this.guardAnchorX = x;
            this.guardAnchorY = y;
        }
        resetNavState();
    }

    public void resetNavState() {
        nav.reset(x, y);
    }

    public void beginYield(double retreatX, double retreatY, double seconds) {
        nav.beginYield(retreatX, retreatY, seconds, targetX, targetY);
        moving = true;
    }

    public void endYield() {
        if (!nav.yielding) return;
        nav.endYield(x, y);
        moving = true;
    }

    public boolean isDestroyed() {
        return hp <= 0 || !active;
    }

    public int getScrapValue() {
        return 1 + (maxHp / 3);
    }

    public Faction lastDamagerFaction = null;

    public void takeDamage(int dmg) {
        takeDamage(dmg, null);
    }

    public void takeDamage(int dmg, Faction from) {
        if (dmg <= 0) return;
        lastDamagerFaction = from;
        int eff = Math.max(1, dmg - type.armor);
        hp = Math.clamp(hp - eff, 0, maxHp);
        if (hp <= 0) {
            active = false;
        }
    }

    public void moveTo(double tx, double ty) {
        this.targetX = tx;
        this.targetY = ty;
        if (stance == Stance.GUARD) {
            this.guardAnchorX = tx;
            this.guardAnchorY = ty;
        }
        this.moving = true;
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        if (repairer != null) {
            repairer.clearTarget();
        }
        resetNavState();
    }

    public void stop() {
        this.moving = false;
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.hasHuntWaypoint = false;
        this.hasHuntStation = false;
        this.returningToHuntStation = false;
        this.guardAnchorX = this.x;
        this.guardAnchorY = this.y;

        // I had a switch here but realised I have some units planned that might have multiple roles,
        // like the mobile repair station I'm working on that can gather scrap and repair stuff.
        // So for now I'm leaving it as three if's and will revisit it sometime later.
        if (harvester != null) harvester.reset();
        if (scavenger != null) scavenger.reset();
        if (repairer != null) repairer.reset();

        resetNavState();
    }

    public void attackUnit(Unit target) {
        this.attackTargetUnit = target;
        this.attackTargetBuilding = null;
        if (repairer != null) {
            repairer.clearTarget();
        }
        this.returningToHuntStation = false;
        if (stance != Stance.GUARD) {
            this.moving = true;
        }
        resetNavState();
    }

    public void attackBuilding(Building target) {
        this.attackTargetBuilding = target;
        this.attackTargetUnit = null;
        if (repairer != null) {
            repairer.clearTarget();
        }
        this.returningToHuntStation = false;
        if (stance != Stance.GUARD) {
            this.moving = true;
        }
        resetNavState();
    }

    public void repairUnit(Unit target) {
        if (repairer != null) {
            repairer.repairTargetUnit = target;
            repairer.repairTargetBuilding = null;
            repairer.isRepairing = false;
            repairer.autoRepair = true;
            repairer.autoRepairJob = false;
            repairer.repairHpCarry = 0.0;
        }
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.moving = true;
        resetNavState();
    }

    public void repairBuilding(Building target) {
        if (repairer != null) {
            repairer.repairTargetBuilding = target;
            repairer.repairTargetUnit = null;
            repairer.isRepairing = false;
            repairer.autoRepair = true;
            repairer.autoRepairJob = false;
            repairer.repairHpCarry = 0.0;
        }
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.moving = true;
        resetNavState();
    }

    public double getAimX() {
        if (attackTargetUnit != null) return attackTargetUnit.x;
        if (attackTargetBuilding != null) return attackTargetBuilding.getCenterX();
        if (repairer != null) {
            if (repairer.repairTargetUnit != null) return repairer.repairTargetUnit.x;
            if (repairer.repairTargetBuilding != null) return repairer.repairTargetBuilding.getCenterX();
        }
        return targetX;
    }

    public double getAimY() {
        if (attackTargetUnit != null) return attackTargetUnit.y;
        if (attackTargetBuilding != null) return attackTargetBuilding.getCenterY();
        if (repairer != null) {
            if (repairer.repairTargetUnit != null) return repairer.repairTargetUnit.y;
            if (repairer.repairTargetBuilding != null) return repairer.repairTargetBuilding.getCenterY();
        }
        return targetY;
    }

    public static double rotateToward(double current, double target, double maxStep) {
        double diff = (target - current) % (Math.PI * 2);
        if (diff < -Math.PI) diff += Math.PI * 2;
        if (diff > Math.PI) diff -= Math.PI * 2;

        if (Math.abs(diff) <= maxStep) {
            return target;
        }
        return current + Math.signum(diff) * maxStep;
    }

    public void update(double dt, GameMap map) {
        if (!active) return;

        updateTimersAndPassiveRegen(dt);
        validateTargets();
        updateAimAndTurret(dt);
        updateStanceEngagements();

        if (moving) {
            UnitNavigation.updateUnitMovement(this, dt, map);
        } else {
            resolveIdleObstacleOverlap(map);
        }
    }

    private void updateTimersAndPassiveRegen(double dt) {
        if (type.isInfantry() && hp < maxHp && active) {
            regenTimer += dt;
            if (regenTimer >= 100.0 / 60.0 - 1e-9) {
                regenTimer -= 100.0 / 60.0;
                hp = Math.min(maxHp, hp + 1);
            }
        } else {
            regenTimer = 0.0;
        }

        if (reloadCooldown > 0.0) {
            reloadCooldown = Math.max(0.0, reloadCooldown - dt);
        }
        if (threatTimer > 0.0) {
            threatTimer = Math.max(0.0, threatTimer - dt);
        }
    }

    private void validateTargets() {
        if (attackTargetUnit != null && attackTargetUnit.isDestroyed()) {
            attackTargetUnit = null;
            if (stance == Stance.HUNT) {
                moving = false;
                resetNavState();
            }
        }
        if (attackTargetBuilding != null && attackTargetBuilding.isDestroyed()) {
            attackTargetBuilding = null;
            if (stance == Stance.HUNT) {
                moving = false;
                resetNavState();
            }
        }
        if (repairer != null) {
            if (repairer.repairTargetUnit != null && (repairer.repairTargetUnit.isDestroyed() || repairer.repairTargetUnit.hp >= repairer.repairTargetUnit.maxHp)) {
                repairer.repairTargetUnit = null;
                repairer.isRepairing = false;
            }
            if (repairer.repairTargetBuilding != null && (repairer.repairTargetBuilding.isDestroyed() || repairer.repairTargetBuilding.hp >= repairer.repairTargetBuilding.maxHp)) {
                repairer.repairTargetBuilding = null;
                repairer.isRepairing = false;
            }
        }
    }

    private void updateAimAndTurret(double dt) {
        double aimX = getAimX();
        double aimY = getAimY();
        double desiredTurretAngle = Math.atan2(aimY - y, aimX - x);
        turretAngle = rotateToward(turretAngle, desiredTurretAngle, 4.0 * dt);
    }

    private void updateStanceEngagements() {
        double aimX = getAimX();
        double aimY = getAimY();

        if (retreatingToRepair) return;

        if (nav.yielding) {
            boolean inContact = (attackTargetUnit != null || attackTargetBuilding != null)
                && Math.sqrt((aimX - x) * (aimX - x) + (aimY - y) * (aimY - y)) <= type.attackRange;
            if (!inContact) return;
            endYield();
        }

        if (!moving && (attackTargetUnit != null || attackTargetBuilding != null)) {
            double chaseDist = Math.sqrt((aimX - x) * (aimX - x) + (aimY - y) * (aimY - y));
            if (type == UnitType.MEDIC && stance == Stance.DEFAULT) {
                if (chaseDist > type.attackRange) {
                    attackTargetUnit = null;
                    attackTargetBuilding = null;
                }
            } else switch (stance) {
                case GUARD -> {
                if (chaseDist > type.attackRange) {
                    attackTargetUnit = null;
                    attackTargetBuilding = null;
                }
                }
                case HUNT -> {
                if (chaseDist > type.sightRange * 2.0) {
                    attackTargetUnit = null;
                    attackTargetBuilding = null;
                    if (hasHuntWaypoint) {
                        targetX = huntWaypointX;
                        targetY = huntWaypointY;
                        moving = true;
                        returningToHuntStation = false;
                    } else if (hasHuntStation) {
                        double stationDistSq = (huntStationX - x) * (huntStationX - x) + (huntStationY - y) * (huntStationY - y);
                        if (stationDistSq > 144.0) {
                            targetX = huntStationX;
                            targetY = huntStationY;
                            moving = true;
                            returningToHuntStation = true;
                        }
                    }
                } else if (chaseDist > type.attackRange) {
                    moving = true;
                }
                }
                case DEFAULT -> {
                if (chaseDist > type.attackRange) {
                    moving = true;
                }
                }
            }
        }

        boolean hasRepairTarget = repairer != null && repairer.hasTarget();
        if (stance == Stance.GUARD && !moving && attackTargetUnit == null && attackTargetBuilding == null && !hasRepairTarget) {
            double anchorDistSq = (guardAnchorX - x) * (guardAnchorX - x) + (guardAnchorY - y) * (guardAnchorY - y);
            if (anchorDistSq > 316.0) {
                targetX = guardAnchorX;
                targetY = guardAnchorY;
                moving = true;
            }
        }

        if (stance == Stance.HUNT && attackTargetUnit == null && attackTargetBuilding == null && hasHuntStation) {
            double stationDistSq = (huntStationX - x) * (huntStationX - x) + (huntStationY - y) * (huntStationY - y);
            if (stationDistSq > 144.0) {
                if (!moving || Math.abs(targetX - huntStationX) > 4.0 || Math.abs(targetY - huntStationY) > 4.0) {
                    targetX = huntStationX;
                    targetY = huntStationY;
                    moving = true;
                    returningToHuntStation = true;
                    resetNavState();
                }
            } else if (returningToHuntStation) {
                moving = false;
                returningToHuntStation = false;
            }
        }
    }

    private void resolveIdleObstacleOverlap(GameMap map) {
        double cl = getObstacleClearance();
        Building ignoredBuilding = (repairer != null && repairer.repairTargetBuilding != null) ? repairer.repairTargetBuilding : null;
        if (UnitNavigation.collidesWithObstacle(x, y, cl, map, ignoredBuilding)) {
            UnitNavigation.resolveObstacleOverlap(this, map, cl, ignoredBuilding);
        }
    }
}
