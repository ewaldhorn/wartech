package wartech.unit;

public final class HarvesterRole {
    public static final int MAX_LOAD = 300;
    public static final int HARVEST_YIELD = 30;
    public static final double HARVEST_RETRY_INTERVAL = 0.5;

    public int crystalLoad = 0;
    public double harvestTimer = 0.0;
    public boolean returningToRefinery = false;
    public boolean autoCollect = true;
    public double harvestTargetX = -1.0;
    public double harvestTargetY = -1.0;
    public double harvestRetryTimer = 0.0;

    public boolean isFull() {
        return crystalLoad >= MAX_LOAD;
    }

    public boolean hasCargo() {
        return crystalLoad > 0;
    }

    public void clearTarget() {
        harvestTargetX = -1.0;
        harvestTargetY = -1.0;
    }

    public void reset() {
        autoCollect = false;
        returningToRefinery = false;
        harvestTimer = 0.0;
        harvestRetryTimer = 0.0;
    }
}
