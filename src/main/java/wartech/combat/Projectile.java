package wartech.combat;

// Ballistic tracer, missile, or plasma projectile in flight
// Need to split this out so I can support more types

import wartech.building.Building;
import wartech.core.Faction;
import wartech.map.GameMap;

public final class Projectile {
    public double x;
    public double y;
    public double targetX;
    public double targetY;
    public double speed;
    public int damage;
    public Faction faction;
    public boolean isRocket;
    public double splash;
    public Building ignoreBuilding;
    public boolean active = true;

    public Projectile(double startX, double startY, double targetX, double targetY,
                      double speed, int damage, Faction faction, boolean isRocket, double splash,
                      Building ignoreBuilding) {
        init(startX, startY, targetX, targetY, speed, damage, faction, isRocket, splash, ignoreBuilding);
    }

    public void init(double startX, double startY, double targetX, double targetY,
                     double speed, int damage, Faction faction, boolean isRocket, double splash,
                     Building ignoreBuilding) {
        this.active = true;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.damage = damage;
        this.faction = faction;
        this.isRocket = isRocket;
        this.splash = splash;
        this.ignoreBuilding = ignoreBuilding;
    }

    public boolean update(double dt, GameMap map) {
        if (!active) return false;

        double dx = targetX - x;
        double dy = targetY - y;
        double distSq = dx * dx + dy * dy;

        double step = speed * dt;
        if (distSq <= step * step || distSq <= 114.0) {
            x = targetX;
            y = targetY;
            active = false;
            return true; // Impact!
        }

        double angle = Math.atan2(dy, dx);
        double dirX = Math.cos(angle);
        double dirY = Math.sin(angle);

        double remaining = step;
        while (remaining > 0.0001) {
            double chunk = Math.min(remaining, 16.0);
            double nx = x + dirX * chunk;
            double ny = y + dirY * chunk;
            int tx = (int) Math.floor(nx / GameMap.TILE_SIZE);
            int ty = (int) Math.floor(ny / GameMap.TILE_SIZE);
            if (map.blocksShot(tx, ty, ignoreBuilding)) {
                x = nx;
                y = ny;
                active = false;
                return true;
            }
            x = nx;
            y = ny;
            remaining -= chunk;
        }
        return false;
    }
}
