package wartech.building;

// Building instance, construction state, and automated defense logic if applicable

import wartech.core.Faction;
import wartech.map.GameMap;
import wartech.unit.Unit;
import wartech.unit.UnitNavigation;
import wartech.unit.UnitType;

public final class Building {
    private static int nextId = 1;

    public final int id;
    public final BuildingType type;
    public final Faction faction;
    public final int tileX;
    public final int tileY;
    public final int pixelX;
    public final int pixelY;

    public int hp;
    public final int maxHp;
    public double constructionProgress; // 0.0 to 1.0 
    public boolean constructing = false;
    public int buildSpent = 0;
    public boolean active = true;

    // Defense turret logic
    public double turretAngle = 0.0;
    public double reloadCooldown = 0.0;

    // Rally point for unit-producing structures
    public boolean hasRallyPoint = false;
    public double rallyX = 0.0;
    public double rallyY = 0.0;
    public boolean isPrimary = false;

    public void setRallyPoint(double x, double y) {
        this.rallyX = x;
        this.rallyY = y;
        this.hasRallyPoint = true;
    }

    public void clearRallyPoint() {
        this.hasRallyPoint = false;
    }

    public Building(BuildingType type, Faction faction, int tileX, int tileY) {
        this.id = nextId++;
        this.type = type;
        this.faction = faction;
        this.tileX = tileX;
        this.tileY = tileY;
        this.pixelX = tileX * GameMap.TILE_SIZE;
        this.pixelY = tileY * GameMap.TILE_SIZE;
        this.maxHp = type.maxHp;
        this.hp = type.maxHp;
        this.constructionProgress = 1.0;
    }

    public boolean isDestroyed() {
        return hp <= 0 || !active;
    }

    // Faction that last damaged this structure; null when unknown. 
    public Faction lastDamagerFaction = null;

    public void takeDamage(int dmg) {
        takeDamage(dmg, null);
    }

    public void takeDamage(int dmg, Faction from) {
        lastDamagerFaction = from;
        hp = Math.clamp(hp - dmg, 0, maxHp);
        if (hp == 0) {
            active = false;
        }
    }

    public boolean containsWorldPoint(double wx, double wy) {
        return wx >= pixelX && wx <= pixelX + (type.widthTiles * GameMap.TILE_SIZE) &&
               wy >= pixelY && wy <= pixelY + (type.heightTiles * GameMap.TILE_SIZE);
    }

    public double getCenterX() {
        return pixelX + (type.widthTiles * GameMap.TILE_SIZE / 2.0);
    }

    public double getCenterY() {
        return pixelY + (type.heightTiles * GameMap.TILE_SIZE / 2.0);
    }

    /** Tile column at the horizontal centre of the footprint. */
    public int centerTileX() {
        return tileX + type.widthTiles / 2;
    }

    /** Tile row at the vertical centre of the footprint. */
    public int centerTileY() {
        return tileY + type.heightTiles / 2;
    }

    public double exitSpawnX() {
        return getCenterX();
    }

    public double exitSpawnY() {
        return pixelY + (type.heightTiles * GameMap.TILE_SIZE) + (GameMap.TILE_SIZE / 2.0);
    }

    public Unit spawnUnit(UnitType unitType, GameMap map) {
        double sx = exitSpawnX();
        double sy = exitSpawnY();
        if (!spawnPointClear(sx, sy, unitType, map)) {
            double w = type.widthTiles * GameMap.TILE_SIZE;
            double h = type.heightTiles * GameMap.TILE_SIZE;
            double cx = pixelX + w / 2.0;
            double cy = pixelY + h / 2.0;
            double half = GameMap.TILE_SIZE / 2.0;
            if (spawnPointClear(pixelX + w + half, cy, unitType, map)) {
                sx = pixelX + w + half;
                sy = cy;
            } else if (spawnPointClear(pixelX - half, cy, unitType, map)) {
                sx = pixelX - half;
                sy = cy;
            } else if (spawnPointClear(cx, pixelY - half, unitType, map)) {
                sx = cx;
                sy = pixelY - half;
            } else if (spawnPointClear(cx, pixelY + h + half * 3.0, unitType, map)) {
                sx = cx;
                sy = pixelY + h + half * 3.0;
            }
        }
        return new Unit(unitType, faction, sx, sy);
    }

    private boolean spawnPointClear(double px, double py, UnitType unitType, GameMap map) {
        if (!UnitNavigation.inWorldBounds(px, py, unitType.radius, map)) return false;
        return !UnitNavigation.collidesWithObstacle(px, py, Unit.obstacleClearanceFor(unitType), map, this);
    }
}
