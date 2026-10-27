package wartech.unit;

import wartech.combat.Wreckage;

public final class ScavengerRole {
    public static final int MAX_SCRAP = 100;

    public int scrapCarried = 0;
    public Wreckage salvageTarget = null;
    public int salvageTargetId = 0;
    public double salvageTimer = 0.0;
    public boolean returningScrap = false;

    public boolean isFull() {
        return scrapCarried >= MAX_SCRAP;
    }

    public boolean hasCargo() {
        return scrapCarried > 0;
    }

    public void clearTarget() {
        salvageTarget = null;
        salvageTargetId = 0;
    }

    public void reset() {
        clearTarget();
        salvageTimer = 0.0;
        returningScrap = false;
    }
}
