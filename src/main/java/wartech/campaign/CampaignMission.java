package wartech.campaign;

// Mission definitions, tech restrictions, and objectives

import java.util.EnumSet;
import java.util.Objects;
import wartech.ai.Difficulty;
import wartech.building.BuildingType;
import wartech.unit.UnitType;

public class CampaignMission {
    // Mission ids
    // Future work: This needs to be more dynamic, perhaps moved to config files?
    public static final int MISSION_DESERT_DAWN = 0;
    public static final int MISSION_IRON_BULWARK = 1;
    public static final int MISSION_VIPERS_NEST = 2;
    public static final int MISSION_FORWARD_REDOUBT = 3;
    public static final int MISSION_ANCIENT_ECHOES = 4;
    public static final int MISSION_SCAVENGER_RUN = 5;
    public static final int MISSION_CRIMSON_FORTRESS = 6;
    public static final int MISSION_CONVOY_EXTRACTION = 7;
    public static final int MISSION_DREAD_HARVEST = 8;
    public static final int MISSION_TITAN_AWAKENING = 9;
    public static final int MISSION_APEX_CATACLYSM = 10;

    public final int id;
    public final String codename;
    public final String title;
    public final String titleUpper; // the briefing header title can be pre-uppercased once
    public final String sector;
    public final String[] briefingText;
    public final String[] primaryObjectives;
    public final String[] secondaryObjectives;
    public final String[] primaryDirectives;   // briefing primary, to avoid rebuilding per frame
    public final String[] secondaryDirectives; // briefing secondary, same reason
    public final String[] unlockedStructures;
    public final String[] unlockedUnits;

    public final int startingCredits;
    public final int mapSize; // 0 = 64x64, 1 = 128x128, should be an enum if I add more sizes
    public final long mapSeed;
    public final Difficulty enemyDifficulty;
    public final boolean hasSyndicateEnemy;

    public final EnumSet<BuildingType> allowedBuildings;
    public final EnumSet<UnitType> allowedUnits;

    // Spawns
    public final int spawnGeodesNearPlayer;
    public final BuildingType[] spawnEnemyStructures;
    public final int spawnRuinGuardiansWalkers;
    public final int spawnRuinGuardiansDrones;
    public final int spawnAwakenedWalkers;

    // Enemy base defenders
    public final UnitType[] enemyDefendersEasy;
    public final UnitType[] enemyDefendersMedium;
    public final UnitType[] enemyDefendersHard;

    public final int targetCredits;
    public final int targetKills;
    public final int targetBonusCount;
    public final double targetTimeLimit;
    public final String targetCreditsSuffix;
    public final String targetKillsSuffix;
    public final String targetBonusCountSuffix;

    public static final class Builder {
        private final int id;
        private String codename;
        private String title;
        private String sector;
        private String[] briefingText;
        private String[] primaryObjectives;
        private String[] secondaryObjectives;
        private final EnumSet<BuildingType> allowedBuildings = EnumSet.of(
            BuildingType.CON_YARD, BuildingType.WIND_TURBINE, BuildingType.REFINERY);
        private final EnumSet<UnitType> allowedUnits = EnumSet.of(
            UnitType.COLLECTOR, UnitType.SCOUT_TRIKE);
        private int startingCredits;
        private int mapSize;
        private long mapSeed;
        private Difficulty enemyDifficulty = Difficulty.EASY;
        private boolean hasSyndicateEnemy;
        private int spawnGeodesNearPlayer;
        private BuildingType[] spawnEnemyStructures;
        private int spawnRuinGuardiansWalkers;
        private int spawnRuinGuardiansDrones;
        private int spawnAwakenedWalkers;
        private UnitType[] enemyDefendersEasy;
        private UnitType[] enemyDefendersMedium;
        private UnitType[] enemyDefendersHard;
        private int targetCredits;
        private int targetKills;
        private int targetBonusCount;
        private double targetTimeLimit;

        public Builder(int id) {
            this.id = id;
        }

        public Builder codename(String v) { codename = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder sector(String v) { sector = v; return this; }
        public Builder briefingText(String[] v) { briefingText = v; return this; }
        public Builder primaryObjectives(String[] v) { primaryObjectives = v; return this; }
        public Builder secondaryObjectives(String[] v) { secondaryObjectives = v; return this; }
        public Builder startingCredits(int v) { startingCredits = v; return this; }
        public Builder mapSize(int v) { mapSize = v; return this; }
        public Builder mapSeed(long v) { mapSeed = v; return this; }
        public Builder enemyDifficulty(Difficulty v) { enemyDifficulty = v; return this; }
        public Builder hasSyndicateEnemy(boolean v) { hasSyndicateEnemy = v; return this; }

        public Builder allow(BuildingType first, BuildingType... rest) {
            allowedBuildings.add(first);
            for (BuildingType t : rest) allowedBuildings.add(t);
            return this;
        }

        public Builder allow(UnitType first, UnitType... rest) {
            allowedUnits.add(first);
            for (UnitType t : rest) allowedUnits.add(t);
            return this;
        }

        public Builder spawnGeodesNearPlayer(int v) { spawnGeodesNearPlayer = v; return this; }
        public Builder spawnEnemyStructures(BuildingType[] v) { spawnEnemyStructures = v; return this; }
        public Builder spawnRuinGuardiansWalkers(int v) { spawnRuinGuardiansWalkers = v; return this; }
        public Builder spawnRuinGuardiansDrones(int v) { spawnRuinGuardiansDrones = v; return this; }
        public Builder spawnAwakenedWalkers(int v) { spawnAwakenedWalkers = v; return this; }
        public Builder enemyDefendersEasy(UnitType[] v) { enemyDefendersEasy = v; return this; }
        public Builder enemyDefendersMedium(UnitType[] v) { enemyDefendersMedium = v; return this; }
        public Builder enemyDefendersHard(UnitType[] v) { enemyDefendersHard = v; return this; }
        public Builder targetCredits(int v) { targetCredits = v; return this; }
        public Builder targetKills(int v) { targetKills = v; return this; }
        public Builder targetBonusCount(int v) { targetBonusCount = v; return this; }
        public Builder targetTimeLimit(double v) { targetTimeLimit = v; return this; }

        public CampaignMission build() {
            return new CampaignMission(this);
        }
    }

    private CampaignMission(Builder b) {
        this.id = b.id;
        this.codename = b.codename;
        this.title = b.title;
        this.titleUpper = Objects.requireNonNull(b.title, "title").toUpperCase();
        this.sector = b.sector;
        this.briefingText = b.briefingText;
        this.primaryObjectives = b.primaryObjectives;
        this.secondaryObjectives = b.secondaryObjectives;
        this.primaryDirectives = prefixAll(b.primaryObjectives, "[!] ");
        this.secondaryDirectives = prefixAll(b.secondaryObjectives, "[*] ");
        this.startingCredits = b.startingCredits;
        this.mapSize = b.mapSize;
        this.mapSeed = b.mapSeed;
        this.enemyDifficulty = b.enemyDifficulty;
        this.hasSyndicateEnemy = b.hasSyndicateEnemy;

        // Base Tier 1 is always unlocked in campaign; scenarios add to it.
        this.allowedBuildings = EnumSet.copyOf(b.allowedBuildings);
        this.allowedUnits = EnumSet.copyOf(b.allowedUnits);
        this.unlockedStructures = structureNames(this.allowedBuildings);
        this.unlockedUnits = unitNames(this.allowedUnits);
        requireProducible(this.allowedUnits, this.allowedBuildings);

        this.spawnGeodesNearPlayer = b.spawnGeodesNearPlayer;
        this.spawnEnemyStructures = b.spawnEnemyStructures;
        this.spawnRuinGuardiansWalkers = b.spawnRuinGuardiansWalkers;
        this.spawnRuinGuardiansDrones = b.spawnRuinGuardiansDrones;
        this.spawnAwakenedWalkers = b.spawnAwakenedWalkers;

        this.enemyDefendersEasy = b.enemyDefendersEasy;
        this.enemyDefendersMedium = b.enemyDefendersMedium;
        this.enemyDefendersHard = b.enemyDefendersHard;

        this.targetCredits = b.targetCredits;
        this.targetKills = b.targetKills;
        this.targetBonusCount = b.targetBonusCount;
        this.targetTimeLimit = b.targetTimeLimit;
        this.targetCreditsSuffix = (b.targetCredits > 0) ? ("/" + b.targetCredits) : "";
        this.targetKillsSuffix = (b.targetKills > 0) ? ("/" + b.targetKills) : "";
        this.targetBonusCountSuffix = (b.targetBonusCount > 0) ? ("/" + b.targetBonusCount) : "";
    }

    private static String[] prefixAll(String[] lines, String marker) {
        if (lines == null) return new String[0];
        String[] out = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            out[i] = (lines[i] == null) ? marker : marker + lines[i];
        }
        return out;
    }

    private static String[] structureNames(EnumSet<BuildingType> types) {
        String[] out = new String[types.size()];
        int i = 0;
        for (BuildingType bt : types) out[i++] = bt.shortName;
        return out;
    }

    private static String[] unitNames(EnumSet<UnitType> types) {
        String[] out = new String[types.size()];
        int i = 0;
        for (UnitType ut : types) out[i++] = ut.shortName;
        return out;
    }

    private static void requireProducible(EnumSet<UnitType> units, EnumSet<BuildingType> buildings) {
        for (UnitType ut : units) {
            BuildingType factory = ut.requiredBuilding;
            if (factory != null && !buildings.contains(factory)) {
                throw new IllegalStateException(
                    "mission authorizes " + ut + " but not its " + factory);
            }
        }
    }

    public UnitType[] getEnemyDefenders(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> enemyDefendersEasy;
            case MEDIUM -> enemyDefendersMedium;
            case HARD -> enemyDefendersHard;
        };
    }

    public boolean isBuildingAllowed(BuildingType bt) {
        return bt != null && allowedBuildings.contains(bt);
    }

    public boolean isUnitAllowed(UnitType ut) {
        return ut != null && allowedUnits.contains(ut);
    }
}
