package wartech.campaign;

// Campaign operations, scripted objectives, briefings, and tech progression

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.MatchState;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitType;

public class CampaignManager {
    public static final int THEATER_COUNT = CampaignData.THEATER_COUNT;
    private static final int[] THEATER_START = CampaignData.THEATER_START;
    private static final int[] THEATER_SIZE = CampaignData.THEATER_SIZE;

    public static final int DEFEAT_NONE = 0;
    public static final int DEFEAT_CON_YARD = 1;
    public static final int DEFEAT_WIPED_OUT = 2;

    public static final int SCAVENGER_RUN_DRONE_TARGET = 8;

    public final CampaignMission[] missions;
    public int currentMissionIndex = 0;
    public int highestUnlockedMission = 0;
    public int currentTheater = 0; // 0 = Act I (Recruit), 1 = Act II (Veteran), 2 = Act III (Commander)
    public int defeatReasonCode = DEFEAT_NONE;
    public boolean campaignActive = false;
    public boolean briefingActive = false;
    public double briefingTimer = 0.0;

    public boolean primary1Done = false;
    public boolean primary2Done = false;
    public boolean secondaryDone = false;
    public BonusState bonusStatus = BonusState.PENDING;
    public boolean bonusIsSurvival = false;
    public boolean primary1IsSurvival = false;
    public boolean missionAccomplished = false;
    public boolean missionFailed = false;

    public int skitterersKilled = 0;
    public int dronesKilled = 0;
    public int walkersKilled = 0;
    public int collectorsLost = 0;
    public int repairsPerformed = 0;
    public int syndicateUnitsKilled = 0;
    public int trikesLost = 0;
    public int tanksLost = 0;

    public final ObjectiveEvaluator objectives = new ObjectiveEvaluator(this);

    public CampaignManager() {
        this.missions = new CampaignMission[missionCount()];
        initMissions();
    }

    public static int missionCount() {
        return CampaignData.MISSION_COUNT;
    }

    private void initMissions() {
        CampaignMission[] baked = CampaignData.createMissions();
        for (int i = 0; i < baked.length && i < missions.length; i++) {
            missions[i] = baked[i];
        }
    }

    public CampaignMission getCurrentMission() {
        if (missions.length == 0) return null;
        int idx = Math.clamp(currentMissionIndex, 0, missions.length - 1);
        if (missions[idx] != null) return missions[idx];
        for (CampaignMission baked : missions) {
            if (baked != null) return baked;
        }
        return null;
    }

    public boolean isBuildingAllowed(BuildingType bt) {
        if (!campaignActive) return true;
        return getCurrentMission().isBuildingAllowed(bt);
    }

    public boolean isUnitAllowed(UnitType ut) {
        if (!campaignActive) return true;
        return getCurrentMission().isUnitAllowed(ut);
    }

    public static int getTheaterForMission(int idx) {
        for (int t = THEATER_COUNT - 1; t >= 0; t--) {
            if (idx >= THEATER_START[t]) return t;
        }
        return 0;
    }

    public static int getTheaterStartMission(int theater) {
        if (theater < 0) theater = 0;
        if (theater >= THEATER_COUNT) theater = THEATER_COUNT - 1;
        return THEATER_START[theater];
    }

    public static int getTheaterMissionCount(int theater) {
        if (theater < 0) theater = 0;
        if (theater >= THEATER_COUNT) theater = THEATER_COUNT - 1;
        return THEATER_SIZE[theater];
    }

    public boolean isTheaterUnlocked(int theater) {
        int start = getTheaterStartMission(theater);
        return highestUnlockedMission >= start;
    }

    public void selectTheater(int theater) {
        if (theater < 0 || theater >= THEATER_COUNT) return;
        if (!isTheaterUnlocked(theater)) return;
        this.currentTheater = theater;
        int start = getTheaterStartMission(theater);
        int count = getTheaterMissionCount(theater);
        int end = start + count - 1;
        if (currentMissionIndex < start || currentMissionIndex > end) {
            int target = Math.min(highestUnlockedMission, end);
            if (target < start) target = start;
            this.currentMissionIndex = target;
            this.briefingTimer = 0.0;
        }
    }

    public void openBriefing(int missionIndex) {
        if (missionIndex < 0) missionIndex = 0;
        if (missionIndex > highestUnlockedMission) missionIndex = highestUnlockedMission;
        if (missionIndex >= missions.length) missionIndex = missions.length - 1;

        this.currentMissionIndex = missionIndex;
        this.currentTheater = getTheaterForMission(missionIndex);
        this.briefingActive = true;
        this.briefingTimer = 0.0;
    }

    public void closeBriefing() {
        this.briefingActive = false;
    }

    public void deployCurrentMission(WarTech game) {
        this.briefingActive = false;
        resetMatchProgress();

        CampaignMission m = getCurrentMission();
        game.startMatch(m.mapSize, m.enemyDifficulty, m.startingCredits, m.mapSeed);

        this.campaignActive = true;

        setupMissionSpawns(game, m);

        game.statusToast = "OPERATION DEPLOYED: " + m.codename;
    }

    public void resetMatchProgress() {
        this.primary1Done = false;
        this.primary2Done = false;
        this.secondaryDone = false;
        this.bonusStatus = BonusState.PENDING;
        this.bonusIsSurvival = false;
        this.primary1IsSurvival = false;
        this.defeatReasonCode = DEFEAT_NONE;
        this.missionAccomplished = false;
        this.missionFailed = false;
        this.skitterersKilled = 0;
        this.dronesKilled = 0;
        this.walkersKilled = 0;
        this.collectorsLost = 0;
        this.repairsPerformed = 0;
        this.syndicateUnitsKilled = 0;
        this.trikesLost = 0;
        this.tanksLost = 0;
    }

    public void deactivateCampaign() {
        this.campaignActive = false;
        this.briefingActive = false;
        resetMatchProgress();
    }

    private void setupMissionSpawns(WarTech game, CampaignMission m) {
        if (!m.hasSyndicateEnemy) {
            removeFaction(game, Faction.ENEMY);
        }

        if (m.spawnGeodesNearPlayer > 0) {
            seedGeodesNearPlayerBase(game, m.spawnGeodesNearPlayer);
        }
        if (m.spawnEnemyStructures != null) {
            for (int i = 0; i < m.spawnEnemyStructures.length; i++) {
                placeEnemyStructure(game, m.spawnEnemyStructures[i]);
            }
        }
        UnitType[] defs = m.getEnemyDefenders(game.enemyAI.difficulty);
        if (defs != null && defs.length > 0 && m.hasSyndicateEnemy) {
            spawnEnemyDefenders(game, defs);
        }
        if (m.spawnRuinGuardiansWalkers > 0 || m.spawnRuinGuardiansDrones > 0) {
            spawnRuinGuardians(game, m.spawnRuinGuardiansWalkers, m.spawnRuinGuardiansDrones);
        }
        if (m.spawnAwakenedWalkers > 0) {
            spawnAwakenedWalkers(game, m.spawnAwakenedWalkers);
        }

        game.map.updateInfluence(game.buildings, game.buildingCount);
    }

    private void removeFaction(WarTech game, Faction faction) {
        for (int i = game.buildingCount - 1; i >= 0; i--) {
            Building b = game.buildings[i];
            if (b != null && b.faction == faction) {
                game.buildings[i] = game.buildings[--game.buildingCount];
                game.buildings[game.buildingCount] = null;
            }
        }
        for (int i = game.unitCount - 1; i >= 0; i--) {
            Unit u = game.units[i];
            if (u != null && u.faction == faction) {
                game.units[i] = game.units[--game.unitCount];
                game.units[game.unitCount] = null;
            }
        }
    }

    private void seedGeodesNearPlayerBase(WarTech game, int maxGeodes) {
        int cx = 8;
        int cy = game.map.height - 12;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && b.faction == Faction.PLAYER && b.type == BuildingType.CON_YARD) {
                cx = b.centerTileX();
                cy = b.centerTileY();
                break;
            }
        }

        int added = 0;
        for (int r = 0; r <= 22 && added < maxGeodes; r++) {
            for (int ty = cy - r; ty <= cy + r && added < maxGeodes; ty++) {
                for (int tx = cx - r; tx <= cx + r && added < maxGeodes; tx++) {
                    if (Math.max(Math.abs(tx - cx), Math.abs(ty - cy)) != r) continue;
                    if (!game.map.inBounds(tx, ty)) continue;
                    int idx = game.map.index(tx, ty);
                    if (game.map.crystals[idx] <= 0) continue;
                    if (game.map.geode[idx] != GameMap.GEODE_NONE) continue;
                    game.map.geode[idx] = GameMap.GEODE_DORMANT;
                    added++;
                }
            }
        }
    }

    private void spawnRuinGuardians(WarTech game, int walkers, int drones) {
        int spawnedWalkers = 0;
        int spawnedDrones = 0;
        for (int round = 0; round < 4; round++) {
            boolean placedAny = false;
            for (int ty = 0; ty < game.map.height; ty++) {
                for (int tx = 0; tx < game.map.width; tx++) {
                    if (spawnedWalkers >= walkers && spawnedDrones >= drones) return;
                    if (game.map.terrain[game.map.index(tx, ty)] != GameMap.TERRAIN_RUIN) continue;
                    if (game.map.buildingGrid[game.map.index(tx, ty)] != null) continue;

                    boolean placed;
                    if (spawnedWalkers < walkers) {
                        placed = spawnNeutralAtClearTile(game, UnitType.PRECURSOR_WALKER, tx, ty);
                        if (placed) spawnedWalkers++;
                    } else {
                        placed = spawnNeutralAtClearTile(game, UnitType.SCAVENGER_DRONE, tx, ty);
                        if (placed) spawnedDrones++;
                    }
                    if (placed) placedAny = true;
                }
            }
            if (!placedAny) return;
        }
    }

    private void spawnAwakenedWalkers(WarTech game, int count) {
        int cx = game.map.width / 2;
        int cy = game.map.height / 2;
        for (int i = 0; i < count; i++) {
            int tx = cx - count + i * 2;
            if (!spawnNeutralAtClearTile(game, UnitType.PRECURSOR_WALKER, tx, cy)) break;
        }
    }

    private boolean spawnNeutralAtClearTile(WarTech game, UnitType type, int tx, int ty) {
        for (int r = 0; r <= 4; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int nx = tx + dx;
                    int ny = ty + dy;
                    if (!game.map.inBounds(nx, ny)) continue;
                    int idx = game.map.index(nx, ny);
                    if (game.map.terrain[idx] == GameMap.TERRAIN_CLIFF) continue;
                    if (game.map.buildingGrid[idx] != null) continue;
                    return game.addUnit(new Unit(type, Faction.NEUTRAL,
                        (nx + 0.5) * GameMap.TILE_SIZE, (ny + 0.5) * GameMap.TILE_SIZE));
                }
            }
        }
        return false;
    }

    private boolean placeEnemyStructure(WarTech game, BuildingType type) {
        Building yard = null;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY && b.type == BuildingType.CON_YARD) {
                yard = b;
                break;
            }
        }
        int cx = yard != null ? yard.centerTileX() : game.map.width - 13;
        int cy = yard != null ? yard.centerTileY() : 6;

        for (int r = 2; r <= 10; r++) {
            for (int ty = cy - r; ty <= cy + r; ty++) {
                for (int tx = cx - r; tx <= cx + r; tx++) {
                    if (Math.max(Math.abs(tx - cx), Math.abs(ty - cy)) != r) continue;
                    if (!game.map.canBuildAt(tx, ty, type, false, game.buildings, game.buildingCount, game.units, game.unitCount)) continue;
                    if (!game.addBuilding(new Building(type, Faction.ENEMY, tx, ty))) return false;
                    game.map.updateInfluence(game.buildings, game.buildingCount);
                    return true;
                }
            }
        }
        return false;
    }

    private void spawnEnemyDefenders(WarTech game, UnitType[] types) {
        Building eCon = null;
        Building eRef = null;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) {
                if (b.type == BuildingType.CON_YARD) eCon = b;
                if (b.type == BuildingType.REFINERY) eRef = b;
            }
        }
        double bx = (eCon != null) ? eCon.getCenterX() : (game.map.width - 12) * GameMap.TILE_SIZE;
        double by = (eCon != null) ? eCon.getCenterY() : 8 * GameMap.TILE_SIZE;

        int spawned = 0;
        for (int i = 0; i < types.length; i++) {
            UnitType t = types[i];
            if (t == null) continue;
            double rawSx = game.enemyAI.getDefStationX(i, bx, by, eRef, eCon, game.map);
            double rawSy = game.enemyAI.getDefStationY(i, bx, by, eRef, eCon, game.map);
            double sx = game.enemyAI.resolveClearX(rawSx, rawSy, game.map);
            double sy = game.enemyAI.resolveClearY(rawSx, rawSy, game.map);

            Unit u = new Unit(t, Faction.ENEMY, sx, sy);
            u.isBaseDefender = true;
            u.stance = Stance.HUNT;
            u.huntStationX = sx;
            u.huntStationY = sy;
            u.hasHuntStation = true;
            u.returningToHuntStation = false;
            u.hasHuntWaypoint = false;
            if (!game.addUnit(u)) break;
            spawned++;
        }
        game.enemyAI.baseDefenderCount = spawned;
    }

    public void onUnitDestroyed(Unit u) {
        onUnitDestroyed(u, null);
    }

    public void onUnitDestroyed(Unit u, Faction killer) {
        if (u == null) return;
        boolean byPlayer = (killer == Faction.PLAYER);
        switch (u.faction) {
            case NEUTRAL -> {
                if (!byPlayer) return;
                switch (u.type) {
                    case SHARD_SKITTERER -> skitterersKilled++;
                    case SCAVENGER_DRONE -> dronesKilled++;
                    case PRECURSOR_WALKER -> walkersKilled++;
                    case null, default -> { }
                }
            }
            case ENEMY -> {
                if (byPlayer) syndicateUnitsKilled++;
            }
            case PLAYER -> {
                switch (u.type) {
                    case COLLECTOR -> collectorsLost++;
                    case SCOUT_TRIKE -> trikesLost++;
                    case COMBAT_TANK, SIEGE_TANK -> tanksLost++;
                    case null, default -> { }
                }
            }
            case null, default -> { }
        }
    }

    public void onVehicleRepaired(UnitType type) {
        if (type == UnitType.COLLECTOR) return;
        repairsPerformed++;
    }

    public void update(WarTech game, double dt) {
        if (briefingActive) {
            briefingTimer += dt;
            return;
        }

        if (!campaignActive || game.matchState != MatchState.IN_PROGRESS) {
            return;
        }

        objectives.evaluateObjectives(game);

        if (game.matchState != MatchState.IN_PROGRESS) return;

        if (primary1Done && primary2Done) {
            objectives.triggerVictory(game);
            return;
        }

        if (game.gameTime > 3.0) {
            int pBldgs = 0;
            for (int i = 0; i < game.buildingCount; i++) {
                Building b = game.buildings[i];
                if (b != null && !b.isDestroyed() && b.faction == Faction.PLAYER) pBldgs++;
            }
            int pUnits = 0;
            for (int i = 0; i < game.unitCount; i++) {
                Unit u = game.units[i];
                if (u != null && !u.isDestroyed() && u.faction == Faction.PLAYER) pUnits++;
            }
            if (pBldgs == 0 && pUnits == 0) {
                objectives.triggerDefeat(game, "VANGUARD FORCES WIPED OUT - SECTOR LOST", DEFEAT_WIPED_OUT);
            }
        }
    }

    public void advanceMission(WarTech game) {
        if (currentMissionIndex < missions.length - 1) {
            currentMissionIndex++;
            openBriefing(currentMissionIndex);
        } else {
            // Completed final mission! Open briefing for final mission
            openBriefing(currentMissionIndex);
        }
    }

    public void replayMission(WarTech game) {
        deployCurrentMission(game);
    }

    public static final int SAVE_VERSION = 1;

    public static int packState(int highest, int current) {
        return (SAVE_VERSION << 8)
            | (highest & 0x0F)
            | ((current & 0x0F) << 4);
    }

    public int getPackedState() {
        return packState(highestUnlockedMission, currentMissionIndex);
    }

    public void loadPackedState(int packed) {
        int version = (packed >> 8) & 0x0F;
        int lastMission = missions.length - 1;
        if (version == SAVE_VERSION) {
            this.highestUnlockedMission = packed & 0x0F;
            this.currentMissionIndex = (packed >> 4) & 0x0F;
        } else if (packed >= 64) {
            this.highestUnlockedMission = packed & 0x0F;
            this.currentMissionIndex = (packed >> 4) & 0x0F;
        } else {
            int h3 = packed & 0x07;
            int c3 = (packed >> 3) & 0x07;
            int h4 = packed & 0x0F;
            int c4 = (packed >> 4) & 0x0F;
            boolean v3 = h3 <= 4 && c3 <= h3;
            boolean v4 = h4 <= lastMission && c4 <= h4;
            if (v4 && !v3) {
                this.highestUnlockedMission = h4;
                this.currentMissionIndex = c4;
            } else {
                this.highestUnlockedMission = h3;
                this.currentMissionIndex = c3;
            }
        }
        if (this.highestUnlockedMission > lastMission) this.highestUnlockedMission = lastMission;
        if (this.highestUnlockedMission < 0) this.highestUnlockedMission = 0;
        if (this.currentMissionIndex > this.highestUnlockedMission) {
            this.currentMissionIndex = this.highestUnlockedMission;
        }
        if (this.currentMissionIndex < 0) this.currentMissionIndex = 0;
        this.currentTheater = getTheaterForMission(this.currentMissionIndex);
    }
}
