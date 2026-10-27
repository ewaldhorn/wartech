package wartech.campaign;

import wartech.ai.Difficulty;
import wartech.building.BuildingType;
import wartech.map.MapSize;
import wartech.unit.UnitType;

public final class CampaignData {
    public static final int MISSION_COUNT = 11;
    public static final int THEATER_COUNT = 3;
    public static final int[] THEATER_START = { 0, 4, 8 };
    public static final int[] THEATER_SIZE = { 4, 4, 3 };

    public static CampaignMission[] createMissions() {
        CampaignMission[] m = new CampaignMission[MISSION_COUNT];

        // Mission 0: Beachhead on Kryos (OPERATION: DESERT DAWN)
        m[CampaignMission.MISSION_DESERT_DAWN] = new CampaignMission.Builder(CampaignMission.MISSION_DESERT_DAWN)
            .codename("OPERATION: DESERT DAWN")
            .title("Beachhead on Kryos")
            .sector("SECTOR 7-G (CRIMSON BADLANDS)")
            .briefingText(new String[] {
                "COMMANDER: Welcome to Planet Kryos. Vanguard High Command has deployed our",
                "expeditionary task force to establish an operational beachhead in the badlands.",
                "Scouts report rich crystal fields, but indigenous crystalline lithoids (Shard",
                "Skitterers) dwell dormant inside the geode nests across the mineral plateaus.",
                "TASK: Deploy a Collector to store 5000 credits of raw crystals and eliminate",
                "at least 6 aggressive Skitterers with Scout Trikes. Maintain positive power grid."
            })
            .primaryObjectives(new String[] { "STORE: 5000 CREDITS IN RESERVE", "CLEAR FAUNA: 6 SKITTERERS ELIMINATED" })
            .secondaryObjectives(new String[] { "BONUS: MAINTAIN POSITIVE BASE POWER" })
            .startingCredits(1000)
            .mapSeed(101L)
            .enemyDifficulty(Difficulty.EASY)
            .allow(BuildingType.LIGHT_FACTORY)
            .spawnGeodesNearPlayer(8)
            .targetCredits(5000)
            .targetKills(6)
            .build();

        // Mission 1: Syndicate Encroachment (OPERATION: IRON BULWARK)
        m[CampaignMission.MISSION_IRON_BULWARK] = new CampaignMission.Builder(CampaignMission.MISSION_IRON_BULWARK)
            .codename("OPERATION: IRON BULWARK")
            .title("Syndicate Encroachment")
            .sector("SECTOR 8-C (IRON VALLEY)")
            .briefingText(new String[] {
                "URGENT DISPATCH: Crimson Syndicate raiders have breached Sector 8-C! They have",
                "erected a forward staging base and are sending combat patrols toward our perimeter.",
                "Command has authorized construction of the Barracks and automated Gun Turrets.",
                "TASK: Defend our Construction Yard at all costs. Assemble an infantry squad of",
                "Rifle Troopers and Rocket Troopers to obliterate the Syndicate forward outpost."
            })
            .primaryObjectives(new String[] { "DEFEND: CON YARD MUST SURVIVE", "ELIMINATE: DESTROY SYNDICATE OUTPOST" })
            .secondaryObjectives(new String[] { "BONUS: CONSTRUCT AT LEAST 2 GUN TURRETS" })
            .startingCredits(2000)
            .mapSeed(202L)
            .enemyDifficulty(Difficulty.EASY)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER })
            .targetBonusCount(2)
            .build();

        // Mission 2: Sulphur Flats (OPERATION: VIPER'S NEST)
        m[CampaignMission.MISSION_VIPERS_NEST] = new CampaignMission.Builder(CampaignMission.MISSION_VIPERS_NEST)
            .codename("OPERATION: VIPER'S NEST")
            .title("Sulphur Flats")
            .sector("SECTOR 7-H (SULPHUR FLATS)")
            .briefingText(new String[] {
                "URGENT DISPATCH: Seismic tremors in the Sulphur Flats have fractured a massive",
                "subterranean geode nursery! Dense packs of aggressive Shard Skitterers are swarming.",
                "Vanguard staging depot requires 20000 refined credits to secure regional supply.",
                "TASK: Sweep the sulphur dunes with Scout Trikes, exterminate at least 12 Skitterers,",
                "and store 20000 credits. Maintain tactical speed and avoid any Scout Trike losses!"
            })
            .primaryObjectives(new String[] { "PURGE HIVE: 12 SKITTERERS ELIMINATED", "STORE: 20000 CREDITS IN RESERVE" })
            .secondaryObjectives(new String[] { "BONUS: ZERO SCOUT TRIKE LOSSES" })
            .startingCredits(1800)
            .mapSeed(102L)
            .enemyDifficulty(Difficulty.EASY)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.RADAR)
            .spawnGeodesNearPlayer(14)
            .targetCredits(20000)
            .targetKills(12)
            .build();

        // Mission 3: The Anvil Chokepoint (OPERATION: FORWARD REDOUBT)
        m[CampaignMission.MISSION_FORWARD_REDOUBT] = new CampaignMission.Builder(CampaignMission.MISSION_FORWARD_REDOUBT)
            .codename("OPERATION: FORWARD REDOUBT")
            .title("The Anvil Chokepoint")
            .sector("SECTOR 9-B (THE ANVIL)")
            .briefingText(new String[] {
                "TACTICAL ALERT: Syndicate mechanized raiding patrols are pressing through the Anvil Gorge.",
                "If they breach the canyon, our regional headquarters will be cut off and encircled.",
                "TASK: Anchor the gorge with an automated Gun Turret line and infantry fireteams.",
                "Eliminate at least 20 hostile Syndicate invaders and establish 4 defensive Turrets",
                "along the perimeter bluffs while protecting our Construction Yard."
            })
            .primaryObjectives(new String[] { "DEFEND GORGE: 20 SYNDICATE INVADERS DESTROYED", "FORTIFY: CONSTRUCT AT LEAST 4 GUN TURRETS" })
            .secondaryObjectives(new String[] { "BONUS: CON YARD SUSTAINS ZERO DAMAGE" })
            .startingCredits(2400)
            .mapSeed(204L)
            .enemyDifficulty(Difficulty.EASY)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .spawnEnemyStructures(new BuildingType[] { BuildingType.BARRACKS, BuildingType.TURRET })
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .targetKills(20)
            .targetBonusCount(4)
            .build();

        // Mission 4: Derelict Relics (OPERATION: ANCIENT ECHOES)
        m[CampaignMission.MISSION_ANCIENT_ECHOES] = new CampaignMission.Builder(CampaignMission.MISSION_ANCIENT_ECHOES)
            .codename("OPERATION: ANCIENT ECHOES")
            .title("Derelict Relics")
            .sector("SECTOR 12-A (FORBIDDEN CRATER)")
            .briefingText(new String[] {
                "INTELLIGENCE REPORT: Long-range radar detected ancient Precursor monolith ruins",
                "in the crater basin. Rogue Scavenger Drones and Quad-Walkers guard the relic sites.",
                "We have unlocked the vehicle factories: manufacture medium Combat Tanks and fast",
                "Scout Trikes to break the automata. TASK: Breach the crater ruins, destroy 2 Precursor",
                "Quad-Walkers, and store 30000 credits from the ancient mineral vaults while holding",
                "off Syndicate probes."
            })
            .primaryObjectives(new String[] { "RUINS: DESTROY 2 PRECURSOR WALKERS", "STORE: 30000 CR FROM CRATER" })
            .secondaryObjectives(new String[] { "BONUS: DESTROY 4 SCAVENGER DRONES" })
            .startingCredits(2500)
            .mapSeed(303L)
            .enemyDifficulty(Difficulty.MEDIUM)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .spawnRuinGuardiansWalkers(4)
            .spawnRuinGuardiansDrones(5)
            .targetCredits(30000)
            .targetKills(2)
            .targetBonusCount(4)
            .build();

        // Mission 5: The Iron Graveyard (OPERATION: SCAVENGER RUN)
        m[CampaignMission.MISSION_SCAVENGER_RUN] = new CampaignMission.Builder(CampaignMission.MISSION_SCAVENGER_RUN)
            .codename("OPERATION: SCAVENGER RUN")
            .title("The Iron Graveyard")
            .sector("SECTOR 13-D (THE GRAVEYARD)")
            .briefingText(new String[] {
                "RECON BRIEFING: Sector 13-D is a rusted battlefield littered with scorched chassis.",
                "Ancient Precursor Scavenger Drones are torching the wrecks to replicate reinforcements.",
                "Syndicate raiders have also deployed an expeditionary outpost to seize the technology.",
                "TASK: Obliterate 8 Scavenger Drones before they strip the field, and wipe out the",
                "Syndicate expeditionary command to secure the graveyard."
            })
            .primaryObjectives(new String[] { "INTERCEPT: DESTROY 8 SCAVENGER DRONES", "EXPEDITION: DESTROY SYNDICATE OUTPOST" })
            .secondaryObjectives(new String[] { "BONUS: PREVENT SCAVENGER REPLICATION" })
            .startingCredits(3000)
            .mapSeed(305L)
            .enemyDifficulty(Difficulty.MEDIUM)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.MECHANIC)
            .spawnRuinGuardiansWalkers(4)
            .spawnRuinGuardiansDrones(8)
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY })
            .targetKills(8)
            .build();

        // Mission 6: Siege of Redoubt Bravo (OPERATION: CRIMSON FORTRESS)
        m[CampaignMission.MISSION_CRIMSON_FORTRESS] = new CampaignMission.Builder(CampaignMission.MISSION_CRIMSON_FORTRESS)
            .codename("OPERATION: CRIMSON FORTRESS")
            .title("Siege of Redoubt Bravo")
            .sector("SECTOR 15-F (OBSIDIAN RIDGE)")
            .briefingText(new String[] {
                "TACTICAL BRIEFING: Syndicate High Command has fortified Obsidian Ridge with heavy",
                "armor factories and perimeter turrets. Frontal assault requires heavy armor.",
                "Authorized: Heavy War Factory, Siege Tanks, and Combat Support line (Medics & Mechanics).",
                "TASK: Smash the Syndicate Fortress! Obliterate their Heavy Factory, Con Yard, and",
                "perimeter defense turrets. Field Mechanics to salvage combat wrecks in the field."
            })
            .primaryObjectives(new String[] { "SIEGE: DESTROY ENEMY WAR FACTORY & CON YARD", "DEFENSES: DESTROY ALL SYNDICATE TURRETS" })
            .secondaryObjectives(new String[] { "BONUS: FIELD REPAIR 3 COMBAT VEHICLES" })
            .startingCredits(3500)
            .mapSize(MapSize.CODE_LARGE_128)
            .mapSeed(404L)
            .enemyDifficulty(Difficulty.MEDIUM)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.SIEGE_TANK)
            .allow(UnitType.MECHANIC)
            .allow(UnitType.MEDIC)
            .spawnEnemyStructures(new BuildingType[] { BuildingType.HEAVY_FACTORY, BuildingType.TURRET, BuildingType.TURRET })
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .targetBonusCount(3)
            .build();

        // Mission 7: Dunestalker Ridge (OPERATION: CONVOY EXTRACTION)
        m[CampaignMission.MISSION_CONVOY_EXTRACTION] = new CampaignMission.Builder(CampaignMission.MISSION_CONVOY_EXTRACTION)
            .codename("OPERATION: CONVOY EXTRACTION")
            .title("Dunestalker Ridge")
            .sector("SECTOR 16-A (DUNESTALKER RIDGE)")
            .briefingText(new String[] {
                "STRATEGIC DIRECTIVE: Fleet logistics require massive crystal refined shipments.",
                "Dunestalker Ridge harbors two separated mineral basins, but Syndicate hunter packs",
                "patrol the transit corridors. TASK: Establish forward mining posts, store 50000",
                "credits across the basins, and destroy the Syndicate staging base in the eastern dunes.",
                "Field Mechanics to keep our haulers and escort tanks operational."
            })
            .primaryObjectives(new String[] { "EXTRACTION: STORE 50000 CREDITS", "STAGING BASE: DESTROY SYNDICATE COMMAND" })
            .secondaryObjectives(new String[] { "BONUS: FIELD REPAIR 5 VEHICLES" })
            .startingCredits(4000)
            .mapSize(MapSize.CODE_LARGE_128)
            .mapSeed(408L)
            .enemyDifficulty(Difficulty.MEDIUM)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.SIEGE_TANK)
            .allow(UnitType.MECHANIC)
            .allow(UnitType.MEDIC)
            .spawnEnemyStructures(new BuildingType[] { BuildingType.HEAVY_FACTORY, BuildingType.BARRACKS, BuildingType.TURRET })
            .spawnRuinGuardiansDrones(1)
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT })
            .targetCredits(50000)
            .targetBonusCount(5)
            .build();

        // Mission 8: Basalt Quarry (OPERATION: DREAD HARVEST)
        m[CampaignMission.MISSION_DREAD_HARVEST] = new CampaignMission.Builder(CampaignMission.MISSION_DREAD_HARVEST)
            .codename("OPERATION: DREAD HARVEST")
            .title("Basalt Quarry")
            .sector("SECTOR 18-C (BASALT QUARRY)")
            .briefingText(new String[] {
                "MAXIMUM ALERT: Syndicate forces have erected a heavily fortified dual-refinery citadel",
                "in the quarry, fueled by a relentless stream of heavy armor. Left unchecked, they",
                "will overrun the entire hemisphere. TASK: Cut their supply lines! Destroy both",
                "Syndicate Refineries, obliterate their Heavy War Factory and demolish their Con Yard."
            })
            .primaryObjectives(new String[] { "CUT SUPPLY: DESTROY BOTH ENEMY REFINERIES", "CRUSH BASE: DESTROY WAR FACTORY & CON YARD" })
            .secondaryObjectives(new String[] { "BONUS: ZERO TANK CASUALTIES" })
            .startingCredits(4500)
            .mapSize(MapSize.CODE_LARGE_128)
            .mapSeed(502L)
            .enemyDifficulty(Difficulty.HARD)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.SIEGE_TANK)
            .allow(UnitType.MECHANIC)
            .allow(UnitType.MEDIC)
            .spawnEnemyStructures(new BuildingType[] { BuildingType.HEAVY_FACTORY, BuildingType.REFINERY })
            .enemyDefendersEasy(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .enemyDefendersMedium(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .enemyDefendersHard(new UnitType[] { UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT, UnitType.DREADNOUGHT })
            .build();

        // Mission 9: The Broken Vaults (OPERATION: TITAN AWAKENING)
        m[CampaignMission.MISSION_TITAN_AWAKENING] = new CampaignMission.Builder(CampaignMission.MISSION_TITAN_AWAKENING)
            .codename("OPERATION: TITAN AWAKENING")
            .title("The Broken Vaults")
            .sector("SECTOR 19-OMEGA (VAULT RUINS)")
            .briefingText(new String[] {
                "EMERGENCY DISPATCH: Subterranean precursor vaults have fractured, releasing a horde",
                "of ancient Quad-Walkers! Syndicate strike forces are scrambling to capture the relics.",
                "TASK: Purge the Precursor Titans! Neutralize at least 5 Quad-Walkers and wipe out the",
                "Syndicate forward command before they harness the ancient titan technology."
            })
            .primaryObjectives(new String[] { "TITAN PURGE: DESTROY 5 PRECURSOR WALKERS", "COMMAND: DESTROY SYNDICATE CON YARD" })
            .secondaryObjectives(new String[] { "BONUS: WIN MISSION UNDER 8 MINUTES" })
            .startingCredits(5000)
            .mapSize(MapSize.CODE_LARGE_128)
            .mapSeed(504L)
            .enemyDifficulty(Difficulty.HARD)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.SIEGE_TANK)
            .allow(UnitType.MECHANIC)
            .allow(UnitType.MEDIC)
            .spawnAwakenedWalkers(6)
            .targetKills(5)
            .targetTimeLimit(480.0)
            .build();

        // Mission 10: The Precursor Core (OPERATION: APEX CATACLYSM)
        m[CampaignMission.MISSION_APEX_CATACLYSM] = new CampaignMission.Builder(CampaignMission.MISSION_APEX_CATACLYSM)
            .codename("OPERATION: APEX CATACLYSM")
            .title("The Precursor Core")
            .sector("SECTOR 0-OMEGA (PLANETARY CRADLE)")
            .briefingText(new String[] {
                "FINAL DIRECTIVE: The central planetary Core has awakened, triggering a three-way",
                "cataclysm between Vanguard Expeditionary Forces, the Syndicate Citadel, and",
                "swarms of awakened Precursor Walkers defending the ancient heart.",
                "TASK: All military doctrine restrictions lifted. Full Vanguard arsenal authorized.",
                "Eradicate the Crimson Syndicate Citadel and purge the Precursor Core Guardians!"
            })
            .primaryObjectives(new String[] { "CONQUEST: ERADICATE SYNDICATE CITADEL", "GUARDIANS: DESTROY PRECURSOR GUARDIANS" })
            .secondaryObjectives(new String[] { "BONUS: ZERO COLLECTOR CASUALTIES" })
            .startingCredits(5000)
            .mapSize(MapSize.CODE_LARGE_128)
            .mapSeed(505L)
            .enemyDifficulty(Difficulty.HARD)
            .hasSyndicateEnemy(true)
            .allow(BuildingType.BARRACKS)
            .allow(BuildingType.LIGHT_FACTORY)
            .allow(BuildingType.HEAVY_FACTORY)
            .allow(BuildingType.TURRET)
            .allow(BuildingType.RADAR)
            .allow(BuildingType.REPAIR_FACILITY)
            .allow(UnitType.TROOPER_RIFLE)
            .allow(UnitType.TROOPER_MISSILE)
            .allow(UnitType.COMBAT_TANK)
            .allow(UnitType.SIEGE_TANK)
            .allow(UnitType.MECHANIC)
            .allow(UnitType.MEDIC)
            .spawnAwakenedWalkers(5)
            .targetKills(3)
            .build();

        return m;
    }
}
