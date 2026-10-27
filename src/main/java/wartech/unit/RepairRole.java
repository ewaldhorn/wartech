package wartech.unit;

import wartech.building.Building;

public final class RepairRole {
    public static final double MECHANIC_LEASH_RANGE = 256.0;
    public static final double REPAIR_UNIT_RANGE = 54.0;
    public static final double REPAIR_BUILDING_RANGE = 44.0;

    public Unit repairTargetUnit = null;
    public Building repairTargetBuilding = null;
    public double repairCreditAccumulator = 0.0;
    public boolean isRepairing = false;
    public boolean autoRepair = true;
    public boolean autoRepairJob = false;
    public double repairHpCarry = 0.0;

    public void clearTarget() {
        repairTargetUnit = null;
        repairTargetBuilding = null;
        isRepairing = false;
        autoRepairJob = false;
    }

    public void reset() {
        clearTarget();
        autoRepair = false;
        repairHpCarry = 0.0;
        repairCreditAccumulator = 0.0;
    }

    public boolean hasTarget() {
        return repairTargetUnit != null || repairTargetBuilding != null;
    }
}
