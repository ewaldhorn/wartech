package wartech.combat;

// Destroyed vehicle chassis debris, salvageable by Scavenger Drones for scrap
// This can be expanded to have different kinds of scrap, maybe mechanics can salvage it?

public final class Wreckage {
    public final double x;
    public final double y;
    public int scrapValue;
    public double decayTimer; // disappears after 45 seconds if not salvaged
    public boolean active = true;

    public Wreckage(double x, double y, int scrapValue) {
        this.x = x;
        this.y = y;
        this.scrapValue = scrapValue;
        this.decayTimer = 45.0;
    }

    public void update(double dt) {
        decayTimer -= dt;
        if (decayTimer <= 0 || scrapValue <= 0) {
            active = false;
        }
    }
}
