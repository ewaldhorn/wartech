package wartech.unit;

// Mobile unit entity with a basic state machines, stances and lifecycle 
// All angles are in radians

import wartech.building.Building;
import wartech.combat.Wreckage;
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

    public double targetX;
    public double targetY;
    public boolean moving = false;

    public static final double SLIDE_LOCK_SECONDS = 40.0 / 60.0;
    public static final double STUCK_CHECK_SECONDS = 45.0 / 60.0;
    public static final double DETOUR_SECONDS = 110.0 / 60.0;

    public boolean yielding = false;
    public double yieldTimer = 0.0;
    public double yieldResumeX = 0.0;
    public double yieldResumeY = 0.0;
    public double yieldRetreatX = 0.0;
    public double yieldRetreatY = 0.0;
    public int slideSide = 0;
    public double slideLockTimer = 0.0;
    public double stuckCheckX = 0.0;
    public double stuckCheckY = 0.0;
    public double stuckCheckTimer = 0.0;
    public int stuckCount = 0;
    public double detourX = 0.0;
    public double detourY = 0.0;
    public double detourTimer = 0.0;
    public int detourSide = 1;

    public static final int MAX_PATH_TILES = 512;
    public int pathLen = 0;
    public int pathIdx = 0;
    public int pathGoalTX = -9999;
    public int pathGoalTY = -9999;
    public int pathVersion = -1;
    public final int[] pathTX = new int[MAX_PATH_TILES];
    public final int[] pathTY = new int[MAX_PATH_TILES];

    public Unit attackTargetUnit = null;
    public Building attackTargetBuilding = null;
    public double reloadCooldown = 0.0;
    public double threatTimer = 0.0;

    public int crystalLoad = 0;
    public static final int MAX_LOAD = 300;
    public static final int HARVEST_YIELD = 30;
    public static final double HARVEST_RETRY_INTERVAL = 0.5;
    public double harvestTimer = 0.0;
    public boolean returningToRefinery = false;
    public boolean autoCollect = true;
    public double harvestTargetX = -1.0;
    public double harvestTargetY = -1.0;
    public double harvestRetryTimer = 0.0;

    public int scrapCarried = 0;
    public static final int MAX_SCRAP = 100;
    public Wreckage salvageTarget = null;
    public double salvageTimer = 0.0;
    public boolean returningScrap = false;

    public Unit repairTargetUnit = null;
    public Building repairTargetBuilding = null;
    public double repairCreditAccumulator = 0.0;
    public boolean isRepairing = false;
    public boolean retreatingToRepair = false;
    public int retreatRetries = 0;
    public boolean autoRepair = true;
    public boolean autoRepairJob = false;

    public static final double MECHANIC_LEASH_RANGE = 256.0;
    public static final double REPAIR_UNIT_RANGE = 54.0;
    public static final double REPAIR_BUILDING_RANGE = 44.0;

    public double regenTimer = 0.0;
    public double healTimer = 0.0;
    public double repairHpCarry = 0.0;

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
        if (type.isMilitary()) {
            this.stance = Stance.GUARD;
            this.guardAnchorX = x;
            this.guardAnchorY = y;
        }
        resetNavState();
    }

    public void resetNavState() {
        slideSide = 0;
        slideLockTimer = 0.0;
        stuckCheckX = x;
        stuckCheckY = y;
        stuckCheckTimer = 0.0;
        stuckCount = 0;
        detourTimer = 0.0;
        pathLen = 0;
        pathIdx = 0;
        pathGoalTX = -9999;
        pathGoalTY = -9999;
        pathVersion = -1;
        yielding = false;
        yieldTimer = 0.0;
    }

    public void beginYield(double retreatX, double retreatY, double seconds) {
        if (!yielding) {
            yieldResumeX = targetX;
            yieldResumeY = targetY;
        }
        resetNavState();
        yielding = true;
        yieldTimer = seconds;
        yieldRetreatX = retreatX;
        yieldRetreatY = retreatY;
        moving = true;
    }

    public void endYield() {
        if (!yielding) return;
        yielding = false;
        yieldTimer = 0.0;
        resetNavState();
        moving = true;
    }

    public boolean isDestroyed() {
        return hp <= 0 || !active;
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
        this.repairTargetUnit = null;
        this.repairTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepairJob = false;
        resetNavState();
    }

    public void stop() {
        this.moving = false;
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.autoCollect = false;
        this.returningToRefinery = false;
        this.hasHuntWaypoint = false;
        this.hasHuntStation = false;
        this.returningToHuntStation = false;
        this.guardAnchorX = this.x;
        this.guardAnchorY = this.y;
        this.repairTargetUnit = null;
        this.repairTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepair = false;
        this.autoRepairJob = false;
        resetNavState();
    }

    public void attackUnit(Unit target) {
        this.attackTargetUnit = target;
        this.attackTargetBuilding = null;
        this.repairTargetUnit = null;
        this.repairTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepairJob = false;
        this.returningToHuntStation = false;
        if (stance != Stance.GUARD) {
            this.moving = true;
        }
        resetNavState();
    }

    public void attackBuilding(Building target) {
        this.attackTargetBuilding = target;
        this.attackTargetUnit = null;
        this.repairTargetUnit = null;
        this.repairTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepairJob = false;
        this.returningToHuntStation = false;
        if (stance != Stance.GUARD) {
            this.moving = true;
        }
        resetNavState();
    }

    public void repairUnit(Unit target) {
        this.repairTargetUnit = target;
        this.repairTargetBuilding = null;
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepair = true;
        this.autoRepairJob = false;
        this.moving = true;
        this.repairHpCarry = 0.0;
        resetNavState();
    }

    public void repairBuilding(Building target) {
        this.repairTargetBuilding = target;
        this.repairTargetUnit = null;
        this.attackTargetUnit = null;
        this.attackTargetBuilding = null;
        this.isRepairing = false;
        this.autoRepair = true;
        this.autoRepairJob = false;
        this.moving = true;
        this.repairHpCarry = 0.0;
        resetNavState();
    }

    public double getAimX() {
        if (attackTargetUnit != null) return attackTargetUnit.x;
        if (attackTargetBuilding != null) return attackTargetBuilding.getCenterX();
        if (repairTargetUnit != null) return repairTargetUnit.x;
        if (repairTargetBuilding != null) return repairTargetBuilding.getCenterX();
        return targetX;
    }

    public double getAimY() {
        if (attackTargetUnit != null) return attackTargetUnit.y;
        if (attackTargetBuilding != null) return attackTargetBuilding.getCenterY();
        if (repairTargetUnit != null) return repairTargetUnit.y;
        if (repairTargetBuilding != null) return repairTargetBuilding.getCenterY();
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
        if (repairTargetUnit != null && (repairTargetUnit.isDestroyed() || repairTargetUnit.hp >= repairTargetUnit.maxHp)) {
            repairTargetUnit = null;
            isRepairing = false;
        }
        if (repairTargetBuilding != null && (repairTargetBuilding.isDestroyed() || repairTargetBuilding.hp >= repairTargetBuilding.maxHp)) {
            repairTargetBuilding = null;
            isRepairing = false;
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

        if (yielding) {
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

        if (stance == Stance.GUARD && !moving && attackTargetUnit == null && attackTargetBuilding == null && repairTargetUnit == null && repairTargetBuilding == null) {
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
        Building ignoredBuilding = (repairTargetBuilding != null) ? repairTargetBuilding : null;
        if (UnitNavigation.collidesWithObstacle(x, y, cl, map, ignoredBuilding)) {
            UnitNavigation.resolveObstacleOverlap(this, map, cl, ignoredBuilding);
        }
    }
}
