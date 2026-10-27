package wartech.unit;

public final class NavigationState {

    public static final double SLIDE_LOCK_SECONDS = 40.0 / 60.0;
    public static final double STUCK_CHECK_SECONDS = 45.0 / 60.0;
    public static final double DETOUR_SECONDS = 110.0 / 60.0;
    public static final int MAX_PATH_TILES = 512;

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

    public int pathLen = 0;
    public int pathIdx = 0;
    public int pathGoalTX = -9999;
    public int pathGoalTY = -9999;
    public int pathVersion = -1;
    public final int[] pathTX = new int[MAX_PATH_TILES];
    public final int[] pathTY = new int[MAX_PATH_TILES];

    public void reset(double currentX, double currentY) {
        slideSide = 0;
        slideLockTimer = 0.0;
        stuckCheckX = currentX;
        stuckCheckY = currentY;
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

    public void clearPath() {
        pathLen = 0;
        pathIdx = 0;
        pathGoalTX = -9999;
        pathGoalTY = -9999;
    }

    public void beginYield(
        double retreatX,
        double retreatY,
        double seconds,
        double currentTargetX,
        double currentTargetY
    ) {
        if (!yielding) {
            yieldResumeX = currentTargetX;
            yieldResumeY = currentTargetY;
        }
        reset(retreatX, retreatY);
        yielding = true;
        yieldTimer = seconds;
        yieldRetreatX = retreatX;
        yieldRetreatY = retreatY;
    }

    public void endYield(double currentX, double currentY) {
        if (!yielding) return;
        yielding = false;
        yieldTimer = 0.0;
        reset(currentX, currentY);
    }
}
