package wartech.ai;

// Crimson Syndicate AI for base building and assault waves

// This class containts the AI's state and update loop.
//  I broke it into more manageable pieces:
// 
//   AiTuning         balance constants
//   AiCensus         per-think status of the enemy's buildings and units
//   EnemyEconomy     income, Collector upkeep and harvesting orders
//   EnemyBuildOrder  which structure to build next, and where
//   EnemyRoster      which units to queue
//   EnemyProduction  foundation and FIFO unit-queue payment and deployment
//   EnemyRetreat     damaged vehicles falling back for repair
//   EnemyDefense     base defenders and perimeter alerts
//   EnemyAssault     wave timing, targeting and attack orders
//   DefenseStations  guard station or base-position geometry

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.map.GameMap;

public final class EnemyAI {
    public Difficulty difficulty = Difficulty.MEDIUM;
    public int credits = AiTuning.DEFAULT_CREDITS;
    public int powerNet = AiTuning.DEFAULT_POWER_NET;
    public double updateTimer = 0.0;
    public double attackTimer = AiTuning.DEFAULT_ATTACK_GRACE_SECONDS; // initial attack grace period
    public double waveInterval = AiTuning.DEFAULT_WAVE_INTERVAL_SECONDS;
    public int waveCount = 0;
    // Counts down to zero every frame. Collector gift fires only once it expired.
    public double collectorGrantCooldown = 0.0;
    public int baseDefenderCount = 0;
    public int assaultForceCount = 0;

    public final EnemyProduction production = new EnemyProduction(this);
    public final EnemyEconomy economy = new EnemyEconomy(this);
    public final EnemyBuildOrder buildOrder = new EnemyBuildOrder(this);
    public final EnemyRoster roster = new EnemyRoster(this);
    public final EnemyRetreat retreat = new EnemyRetreat(this);
    public final EnemyDefense defense = new EnemyDefense(this);
    public final EnemyAssault assault = new EnemyAssault(this);
    private final AiCensus census = new AiCensus();

    public AiTuning.Tier tuning() {
        return AiTuning.tier(difficulty);
    }

    public int addCredits(int amount) {
        int room = WarTech.MAX_CREDITS - credits;
        int accepted = Math.max(0, Math.min(amount, room));
        credits += accepted;
        return accepted;
    }

    public int getRequiredDefenders() {
        return tuning().requiredDefenders();
    }

    public int getMinAssaultForce() {
        return tuning().minAssaultForce();
    }

    public int countAvailableAssaultUnits(WarTech game) {
        return assault.countAvailableAssaultUnits(game);
    }

    public void initDifficulty(Difficulty diff) {
        this.difficulty = diff;
        this.waveCount = 0;
        this.baseDefenderCount = 0;
        this.assaultForceCount = 0;
        production.reset();
        this.collectorGrantCooldown = 0.0;
        this.updateTimer = 0.0;
        AiTuning.Tier t = tuning();
        this.credits = t.startCredits();
        this.attackTimer = t.firstWaveDelay();
        this.waveInterval = t.waveInterval();
    }

    public void update(double dt, GameMap map, WarTech game) {
        updateTimer += dt;
        attackTimer -= dt;
        if (collectorGrantCooldown > 0.0) {
            collectorGrantCooldown = Math.max(0.0, collectorGrantCooldown - dt);
        }

        production.update(production.consumeTicks(dt), game);

        if (updateTimer >= AiTuning.THINK_INTERVAL_SECONDS) {
            updateTimer = 0.0;
            think(map, game);
        }

        if (attackTimer <= 0.0) {
            assault.tickWave(game);
        }
    }

    // The thinking steps for the AI. This happens in sequence, one step can affect the next one.
    private void think(GameMap map, WarTech game) {
        census.refresh(game);
        powerNet = census.powerNet;

        economy.applyIncome(census, game);
        economy.queueReplacementCollectors(census, game);
        buildOrder.update(census, map, game);
        roster.update(census, game);
        economy.directCollectors(map, game);
        retreat.update(census, game);
        defense.update(map, game);
    }

    // Find an active building of the type to ensure an action is legit
    public static Building findEnemyBuilding(BuildingType type, WarTech game) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (!b.isDestroyed() && !b.constructing && b.faction == Faction.ENEMY && b.type == type) {
                return b;
            }
        }
        return null;
    }

    // Defender-station geometry lives in DefenseStations; these keep the public API stable while I fine-tune things
    public double getDefStationX(int slot, double bx, double by, Building ref, Building con, GameMap map) {
        return DefenseStations.stationX(slot, bx, by, ref, con, map);
    }

    public double getDefStationY(int slot, double bx, double by, Building ref, Building con, GameMap map) {
        return DefenseStations.stationY(slot, bx, by, ref, con, map);
    }

    public double resolveClearX(double sx, double sy, GameMap map) {
        return DefenseStations.resolveClearX(sx, sy, map);
    }

    public double resolveClearY(double sx, double sy, GameMap map) {
        return DefenseStations.resolveClearY(sx, sy, map);
    }
}
