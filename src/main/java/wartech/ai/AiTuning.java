package wartech.ai;

// Balance and behaviour constants for the enemy commander AI (also see EnemyAI for how it's used).

public final class AiTuning {

    private AiTuning() {}

    // Difficulty tier
    public record Tier(
            // Economy
            int startCredits,
            int creditsPerThink,
            int maxCollectors,
            int maxCollectorsExpanded,
            // Wave pacing
            double firstWaveDelay,
            double waveInterval,
            int assaultRecheckSeconds,
            // Assault force
            int requiredDefenders,
            int minAssaultForce,
            int assaultBase,
            int assaultGrowthCap,
            // Base building
            int maxTurrets,
            int maxRefineries,
            int powerThreshold,
            int turretReserve,
            // Production
            int maxInfantry,
            int maxVehicles,
            int maxMechanicsPerFacility,
            int maxMedics,
            double vandalShare,
            double dreadnoughtShare,
            // Repair
            double retreatFraction) {

        /**
         * Units one wave may send after {@code waveCount} waves. 
         * Commander is uncapped, it is brutal.
         */
        public int assaultCap(int waveCount) {
            return assaultBase + Math.min(assaultGrowthCap, waveCount);
        }
    }

    // Recruit: slow waves, small raids, no repairs
    public static final Tier TIER_EASY = new Tier(
            1100,   // startCredits
            1,      // creditsPerThink
            10,     // maxCollectors
            10,     // maxCollectorsExpanded
            55.0,   // firstWaveDelay
            84.0,   // waveInterval
            30,     // assaultRecheckSeconds
            3,      // requiredDefenders
            3,      // minAssaultForce
            3,      // assaultBase
            4,      // assaultGrowthCap
            3,      // maxTurrets
            1,      // maxRefineries
            15,     // powerThreshold
            150,    // turretReserve
            8,      // maxInfantry
            6,      // maxVehicles
            1,      // maxMechanicsPerFacility
            1,      // maxMedics
            0.35,   // vandalShare (35% Vandal; was RAIDER_SHARE_EASY 0.65)
            0.0,    // dreadnoughtShare
            0.0);   // retreatFraction — Easy never retreats; EnemyRetreat gates on MEDIUM

    // Veteran: regular waves, combined forces, repairs enabled
    public static final Tier TIER_MEDIUM = new Tier(
            2000,   // startCredits
            3,      // creditsPerThink
            14,     // maxCollectors
            14,     // maxCollectorsExpanded
            30.0,   // firstWaveDelay
            54.0,   // waveInterval
            20,     // assaultRecheckSeconds
            6,      // requiredDefenders
            6,      // minAssaultForce
            6,      // assaultBase
            8,      // assaultGrowthCap
            5,      // maxTurrets
            2,      // maxRefineries
            25,     // powerThreshold
            100,    // turretReserve
            16,     // maxInfantry
            12,     // maxVehicles
            1,      // maxMechanicsPerFacility
            1,      // maxMedics
            0.50,   // vandalShare
            0.20,   // dreadnoughtShare
            0.25);  // retreatFraction

    // Commander: heavy blitz, dual refineries, trickier with repairs and assaults
    public static final Tier TIER_HARD = new Tier(
            3500,   // startCredits
            5,      // creditsPerThink
            14,     // maxCollectors
            18,     // maxCollectorsExpanded (second refinery raises the fleet cap)
            15.0,   // firstWaveDelay
            34.0,   // waveInterval
            10,     // assaultRecheckSeconds
            12,     // requiredDefenders
            9,      // minAssaultForce
            99,     // assaultBase
            0,      // assaultGrowthCap
            7,      // maxTurrets
            3,      // maxRefineries
            35,     // powerThreshold
            100,    // turretReserve
            26,     // maxInfantry
            20,     // maxVehicles
            2,      // maxMechanicsPerFacility
            2,      // maxMedics
            0.65,   // vandalShare
            0.55,   // dreadnoughtShare
            0.40);  // retreatFraction

    public static Tier tier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> TIER_EASY;
            case MEDIUM -> TIER_MEDIUM;
            case HARD -> TIER_HARD;
        };
    }

    // Starting Defaults
    public static final int DEFAULT_CREDITS = 1500;
    public static final int DEFAULT_POWER_NET = 10;
    public static final double DEFAULT_ATTACK_GRACE_SECONDS = 40.0;
    public static final double DEFAULT_WAVE_INTERVAL_SECONDS = 35.0;

    // How often should the AI pause between "think" occurences
    public static final double THINK_INTERVAL_SECONDS = 1.0;

    // How does low power affect AI, for now, same as player
    public static final double LOW_POWER_PRODUCTION_FACTOR = 0.5;

    // Construction and spawing and rally point jitter
    public static final double FOUNDATION_MIN_HP_FRACTION = 0.1;
    public static final int FOUNDATION_MIN_HP = 10;
    public static final int FOUNDATION_HP_DIVISOR = 10;
    public static final double SPAWN_EFFECT_SECONDS = 0.25;
    public static final double SPAWN_EFFECT_SIZE = 21.0;
    public static final double RALLY_JITTER = 12.0;
    public static final double SPAWN_SPREAD_X = 40.0;
    public static final double SPAWN_OFFSET_Y = 24.0;
    public static final double SPAWN_SPREAD_Y = 32.0;

    /// Guides on where to build. For now, assumes enemy is top right, this needs a lot of work
    public static final int BUILD_AREA_FROM_EAST_TILES = 22;
    public static final int BUILD_AREA_TOP_ROW = 2;
    public static final int BUILD_AREA_WIDTH_TILES = 20;
    public static final int BUILD_AREA_HEIGHT_TILES = 16;

    // Where to put a con yard when the old one is gone. This is meh, needs a bit of work 
    // as in it needs to be more dynamic. I need a better building management system eventually.
    public static final double FALLBACK_BASE_FROM_EAST_TILES = 12.0;
    public static final double FALLBACK_BASE_ROW_TILES = 8.0;

    // Economy 
    public static final int NO_REFINERY_RELIEF_CREDITS = 30; // help the AI out when it's in real trouble
    public static final double REFINERY_DEPOT_OFFSET_Y = 53.0;
    public static final int CRYSTAL_SEARCH_RADIUS_TILES = 24;
    public static final double COLLECTOR_GRANT_COOLDOWN_SECONDS = 20.0; // How often is a free collector given if the enemy has none

    // If you are rich, build more turrets
    public static final int SURPLUS_TURRETS = 8;
    public static final int SURPLUS_TURRET_BANK = 1000;

    // Unit creation probabilities
    public static final double MEDIC_QUEUE_CHANCE = 0.15;

    // Damaged vehicles are held back, maybe they get repaired
    public static final double WOUNDED_VEHICLE_FRACTION = 0.40;

    // Once a unit is retreating, it should continue until it's near the repair depot
    public static final double RETREAT_HYSTERESIS = 0.20;
    public static final double RETREAT_DEPOT_MARGIN = 32.0;
    public static final double RETREAT_ARRIVED_RADIUS = 120.0;
    public static final double RETREAT_PARKED_RADIUS = 90.0;
    public static final int MAX_RETREAT_RETRIES = 5;

    // Base defense parameters
    public static final double DEFENDER_DRIFT_RADIUS = 20.0;
    public static final double DEFENDER_CONSCRIPT_RADIUS = 1000.0;
    public static final double BASE_THREAT_RADIUS = 480.0;
    public static final double STATION_EDGE_MARGIN_TILES = 2.0;
    
    // The next bit is hard-coded, I haven't created truly dynamic maps yet
    // Guard station positions around the base
    public static final int DEF_STATION_SLOTS = 12;
    public static final int DEF_SLOT_REFINERY_GUARD = 3;
    public static final int DEF_SLOT_CON_YARD_GUARD = 6;
    public static final double[] DEF_STATION_DX = { -220.0, -280.0,  -60.0, -180.0,  -340.0, 
                                                    -160.0, -80.0, 50.0, -280.0, -380.0, -100.0, 
                                                    40.0 };
    public static final double[] DEF_STATION_DY = { 180.0, 40.0, 260.0, 120.0, 120.0, 340.0, 80.0, 
                                                    220.0, 240.0, 60.0, 320.0, 140.0 };

    // Offsets from the building's centre for the guard slots.
    public static final double DEF_REFINERY_DX = -110.0;
    public static final double DEF_REFINERY_DY = 60.0;
    public static final double DEF_CON_YARD_DX = -70.0;
    public static final double DEF_CON_YARD_DY = 70.0;

    // Assualt waves spread
    public static final double DEFAULT_ASSAULT_MARGIN_TILES = 10.0;
    public static final double ASSAULT_SPREAD = 80.0;
}
