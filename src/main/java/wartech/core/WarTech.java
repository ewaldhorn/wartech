package wartech.core;

// The core that pulls it all together.
// I should split this monster up.

import wartech.ai.Difficulty;
import wartech.ai.EnemyAI;
import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.building.ProductionSystem;
import wartech.campaign.CampaignManager;
import wartech.combat.CombatEngine;
import wartech.combat.Effect;
import wartech.combat.Projectile;
import wartech.combat.ThreatSystem;
import wartech.combat.Wreckage;
import wartech.map.GameMap;
import wartech.map.MapSize;
import wartech.render.Renderer;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitNavigation;
import wartech.unit.UnitSpatialGrid;
import wartech.unit.UnitType;

public class WarTech {
    public static final String VERSION = BuildVersion.VERSION;

    public static final int WIDTH = Display.WIDTH;
    public static final int HEIGHT = Display.HEIGHT;

    public static final int MAX_BUILDINGS = 256;
    public static final int MAX_UNITS = 512;
    public static final int MAX_PROJECTILES = 256;
    public static final int MAX_EFFECTS = 256;
    public static final int MAX_WRECKS = 256;

    private static WarTech instance;

    public Surface screen;
    public Renderer renderer;
    public GameMap map;
    public Camera camera;
    public EnemyAI enemyAI;

    public CombatEngine combatEngine;
    public ProductionSystem productionSystem;
    public InputController inputController;
    public ThreatSystem threatSystem;

    public final Building[] buildings = new Building[MAX_BUILDINGS];
    public int buildingCount = 0;

    public final Unit[] units = new Unit[MAX_UNITS];
    public int unitCount = 0;
    public final UnitSpatialGrid spatialGrid = new UnitSpatialGrid();

    public final Projectile[] projectiles = new Projectile[MAX_PROJECTILES];
    public int projectileCount = 0;

    public final Effect[] effects = new Effect[MAX_EFFECTS];
    public int effectCount = 0;

    public final Wreckage[] wrecks = new Wreckage[MAX_WRECKS];
    public int wreckCount = 0;

    private void initPools() {
        for (int i = 0; i < MAX_PROJECTILES; i++) {
            projectiles[i] = new Projectile(0.0, 0.0, 0.0, 0.0, 0.0, 0, Faction.PLAYER, false, 0.0, null);
            projectiles[i].active = false;
        }
        
        for (int i = 0; i < MAX_EFFECTS; i++) {
            effects[i] = new Effect(Effect.TYPE_EXPLOSION, 0.0, 0.0, 0.0, 0.0);
            effects[i].active = false;
        }

        for (int i = 0; i < MAX_WRECKS; i++) {
            wrecks[i] = new Wreckage(0.0, 0.0, 0);
            wrecks[i].active = false;
        }
    }

    public static final int MAX_CREDITS = 99999;
    public static final double REFINERY_DEPOSIT_RADIUS = 139.0;
    public int playerCredits = 1200;
    public int playerPowerNet = 10;
    public BuildingType placingBuilding = null;

    public MatchState matchState = MatchState.IN_PROGRESS;
    public double matchOverTimer = 0.0;

    public boolean showSetupScreen = false;
    public boolean matchStarted = false;
    public Difficulty selectedDifficulty = Difficulty.MEDIUM;
    public int startingCredits = 1200;
    public long mapSeed = 42L;
    public int selectedMapSize = MapSize.CODE_SMALL_64;

    public GameMode selectedGameMode = GameMode.SKIRMISH;

    public Rng simRng = new Rng(42L);
    public Rng fxRng = new Rng(43L);
    public CampaignManager campaignManager = new CampaignManager();
    public boolean showObjectives = true;

    public int unitsKilled = 0;
    public int unitsLost = 0;
    public int buildingsDestroyed = 0;
    public int totalCreditsHarvested = 0;

    public static final BuildingType[] BUILD_ORDER = new BuildingType[] {
        BuildingType.WIND_TURBINE,
        BuildingType.REFINERY,
        BuildingType.BARRACKS,
        BuildingType.LIGHT_FACTORY,
        BuildingType.HEAVY_FACTORY,
        BuildingType.REPAIR_FACILITY,
        BuildingType.TURRET,
        BuildingType.RADAR,
        BuildingType.CON_YARD
    };

    public static final UnitType[] UNIT_ORDER = new UnitType[] {
        UnitType.TROOPER_RIFLE,
        UnitType.TROOPER_MISSILE,
        UnitType.MEDIC,
        UnitType.MECHANIC,
        UnitType.SCOUT_TRIKE,
        UnitType.COMBAT_TANK,
        UnitType.SIEGE_TANK,
        UnitType.COLLECTOR
    };

    public static final int MAX_UNIT_QUEUE = 8;
    public static final int UNIT_BUILD_SPEND_PER_TICK = 1;
    public static final int BUILD_SPEND_PER_TICK = 1;
    public static final double PRODUCTION_TICKS_PER_SECOND = 60.0;
    public static final double PRODUCTION_CLOCK_EPSILON = 1e-9;
    public final UnitType[] unitQueue = new UnitType[MAX_UNIT_QUEUE];
    public int unitQueueCount = 0;
    public int unitBuildSpent = 0;

    public int activeTab = 0; // 0 = BUILDINGS, 1 = UNITS
    public double productionClock = 0.0;
    public int capacityNotice = 0;
    public static final int NOTICE_NONE = 0;
    public static final int NOTICE_UNIT_CAP = 1;
    public static final int NOTICE_COLLECTOR_SKIPPED = 2;
    public String statusToast = "BASE READY";
    public Unit selectedUnit = null;
    public Building selectedBuilding = null;

    public int mouseX = 0;
    public int mouseY = 0;
    public boolean isDragging = false;
    public int dragStartX = 0;
    public int dragStartY = 0;

    public boolean keyLeft = false;
    public boolean keyRight = false;
    public boolean keyUp = false;
    public boolean keyDown = false;
    public boolean ctrlHeld = false;

    public static void wasm_init(int mapSizeCode) {
        if (instance == null) {
            instance = new WarTech();
            instance.initPools();
        }
        instance.init(mapSizeCode);
    }

    public static void wasm_init() {
        wasm_init(0);
    }

    public static void wasm_update(double dt) {
        if (instance != null) {
            instance.update(dt);
        }
    }

    public static void wasm_mouse_down(int x, int y, int button) {
        if (instance != null) instance.inputController.onMouseDown(x, y, button);
    }

    public static void wasm_mouse_up(int x, int y, int button) {
        if (instance != null) instance.inputController.onMouseUp(x, y, button);
    }

    public static void wasm_mouse_move(int x, int y) {
        if (instance != null) instance.inputController.onMouseMove(x, y);
    }

    public static void wasm_key_down(int code) {
        if (instance != null) instance.inputController.setKey(code, true);
    }

    public static void wasm_key_up(int code) {
        if (instance != null) instance.inputController.setKey(code, false);
    }

    public static int wasm_get_credits() {
        return instance != null ? instance.playerCredits : 0;
    }

    public static int wasm_get_power() {
        return instance != null ? instance.playerPowerNet : 0;
    }

    public static int wasm_get_match_state() {
        return instance != null ? instance.matchState.code : 0;
    }

    public static void wasm_release_input() {
        if (instance != null) instance.inputController.releaseInput();
    }

    public static void wasm_restart_match() {
        if (instance == null) return;
        if (instance.campaignManager != null && instance.campaignManager.campaignActive) {
            instance.campaignManager.replayMission(instance);
        } else {
            instance.startSkirmish(instance.selectedMapSize, instance.selectedDifficulty,
                instance.startingCredits, instance.mapSeed);
        }
    }

    public static int wasm_is_campaign_active() {
        return (instance != null && instance.campaignManager != null && instance.campaignManager.campaignActive) ? 1 : 0;
    }
    public static int wasm_get_campaign_save() {
        return (instance != null && instance.campaignManager != null) ? instance.campaignManager.getPackedState() : 0;
    }

    public static void wasm_load_campaign_save(int packed) {
        if (instance != null && instance.campaignManager != null) {
            instance.campaignManager.loadPackedState(packed);
        }
    }

    public void toggleObjectives() {
        showObjectives = !showObjectives;
        statusToast = showObjectives ? "DIRECTIVES EXPANDED" : "DIRECTIVES MINIMIZED [PRESS 'O' TO EXPAND]";
    }

    public static void wasm_zoom_in() {
        if (instance != null) instance.zoomIn();
    }

    public static void wasm_zoom_out() {
        if (instance != null) instance.zoomOut();
    }

    public static int wasm_get_zoom() {
        return instance != null ? instance.camera.getZoomPercent() : 50;
    }

    public void zoomIn() {
        double ax = (mouseX >= 0 && mouseX < Renderer.VIEW_WIDTH && mouseY >= 0 && mouseY < Renderer.SCREEN_HEIGHT)
            ? mouseX : (Renderer.VIEW_WIDTH / 2.0);
        double ay = (mouseX >= 0 && mouseX < Renderer.VIEW_WIDTH && mouseY >= 0 && mouseY < Renderer.SCREEN_HEIGHT)
            ? mouseY : (Renderer.SCREEN_HEIGHT / 2.0);
        camera.zoomIn(ax, ay);
        statusToast = "ZOOM: " + camera.getZoomPercent() + "%";
    }

    public void zoomOut() {
        double ax = (mouseX >= 0 && mouseX < Renderer.VIEW_WIDTH && mouseY >= 0 && mouseY < Renderer.SCREEN_HEIGHT)
            ? mouseX : (Renderer.VIEW_WIDTH / 2.0);
        double ay = (mouseX >= 0 && mouseX < Renderer.VIEW_WIDTH && mouseY >= 0 && mouseY < Renderer.SCREEN_HEIGHT)
            ? mouseY : (Renderer.SCREEN_HEIGHT / 2.0);
        camera.zoomOut(ax, ay);
        statusToast = "ZOOM: " + camera.getZoomPercent() + "%";
    }

    public boolean addBuilding(Building b) {
        if (b == null || buildingCount >= MAX_BUILDINGS) return false;
        buildings[buildingCount++] = b;
        return true;
    }

    public boolean addUnit(Unit u) {
        if (u == null || unitCount >= MAX_UNITS) return false;
        units[unitCount] = u;
        spatialGrid.insert(unitCount, u.x, u.y);
        unitCount++;
        return true;
    }

    public boolean spawnProjectile(double sx, double sy, double tx, double ty, double speed, int damage,
                                   Faction faction, boolean rocket, double splash, Building ignore) {
        if (projectileCount >= MAX_PROJECTILES) return false;
        projectiles[projectileCount++].init(sx, sy, tx, ty, speed, damage, faction, rocket, splash, ignore);
        return true;
    }

    public int addPlayerCredits(int amount) {
        int room = MAX_CREDITS - playerCredits;
        int accepted = Math.max(0, Math.min(amount, room));
        playerCredits += accepted;
        return accepted;
    }

    public void spawnEffect(byte type, double x, double y, double duration, double radius) {
        if (effectCount < MAX_EFFECTS) {
            effects[effectCount++].init(type, x, y, duration, radius);
        }
    }

    public void spawnWreck(double x, double y, int scrapValue) {
        if (wreckCount < MAX_WRECKS) {
            wrecks[wreckCount++].init(x, y, scrapValue);
        }
    }

    public void spawnWreck(Unit u) {
        spawnWreck(u.x, u.y, 100);
    }

    private void init(int mapSizeCode) {
        int base = Memory.base();
        screen = new Surface(base, WIDTH, HEIGHT);
        renderer = new Renderer(screen);

        combatEngine = new CombatEngine(this);
        productionSystem = new ProductionSystem(this);
        inputController = new InputController(this);
        threatSystem = new ThreatSystem(this);

        this.campaignManager = new CampaignManager();
        this.selectedGameMode = GameMode.SKIRMISH;

        this.selectedMapSize = mapSizeCode;
        startMatch(mapSizeCode, selectedDifficulty, startingCredits, mapSeed);
        spawnSkirmishDefenders(selectedDifficulty);

        this.showSetupScreen = true;
        this.matchStarted = false;
    }

    public void startMatch(int mapSizeCode, Difficulty diff, int credits, long seed) {
        if (campaignManager != null) {
            campaignManager.resetMatchProgress();
        }

        this.selectedMapSize = mapSizeCode;
        this.selectedDifficulty = diff;
        this.startingCredits = credits;
        this.mapSeed = seed;

        this.playerCredits = startingCredits;
        this.matchState = MatchState.IN_PROGRESS;
        this.showSetupScreen = false;
        this.matchStarted = true;
        this.showObjectives = true;
        this.gameTime = 0.0;
        this.matchOverTimer = 0.0;
        this.acquireSlice = 0;
        this.productionClock = 0.0;
        this.capacityNotice = NOTICE_NONE;
        this.isDragging = false;
        this.keyLeft = false;
        this.keyRight = false;
        this.keyUp = false;
        this.keyDown = false;
        this.ctrlHeld = false;
        this.unitsKilled = 0;
        this.unitsLost = 0;
        this.buildingsDestroyed = 0;
        this.totalCreditsHarvested = 0;

        // Reset game state
        for (int i = 0; i < unitCount; i++) units[i] = null;
        for (int i = 0; i < buildingCount; i++) buildings[i] = null;
        for (int i = 0; i < projectileCount; i++) projectiles[i].active = false;
        for (int i = 0; i < effectCount; i++) effects[i].active = false;
        for (int i = 0; i < wreckCount; i++) wrecks[i].active = false;
        unitCount = 0;
        buildingCount = 0;
        projectileCount = 0;
        effectCount = 0;
        wreckCount = 0;
        for (int i = 0; i < MAX_UNIT_QUEUE; i++) unitQueue[i] = null;
        unitQueueCount = 0;
        unitBuildSpent = 0;
        selectedUnit = null;
        selectedBuilding = null;
        placingBuilding = null;
        if (inputController != null) {
            inputController.cancelInput();
        }

        this.simRng = new Rng(seed);
        this.fxRng = new Rng(seed ^ 0x5DEECE66DL);

        var mapSize = MapSize.fromCode(mapSizeCode);
        map = new GameMap(mapSize.tiles, mapSize.tiles);
        map.generate(mapSeed);
        spatialGrid.init(map.width * GameMap.TILE_SIZE, map.height * GameMap.TILE_SIZE);

        camera = new Camera(mapSize.worldPx, mapSize.worldPx);
        camera.centerOn(12 * GameMap.TILE_SIZE, (map.height - 10) * GameMap.TILE_SIZE);

        enemyAI = new EnemyAI();
        enemyAI.initDifficulty(diff);

        addBuilding(new Building(BuildingType.CON_YARD, Faction.PLAYER, 8, map.height - 12));
        addBuilding(new Building(BuildingType.WIND_TURBINE, Faction.PLAYER, 12, map.height - 12));
        addBuilding(new Building(BuildingType.REFINERY, Faction.PLAYER, 12, map.height - 8));

        var pCol = new Unit(UnitType.COLLECTOR, Faction.PLAYER, 14 * GameMap.TILE_SIZE, (map.height - 5) * GameMap.TILE_SIZE);
        pCol.autoCollect = true;
        addUnit(pCol);
        addUnit(new Unit(UnitType.TROOPER_RIFLE, Faction.PLAYER, 9 * GameMap.TILE_SIZE, (map.height - 7) * GameMap.TILE_SIZE));
        addUnit(new Unit(UnitType.SCOUT_TRIKE, Faction.PLAYER, 10 * GameMap.TILE_SIZE, (map.height - 7) * GameMap.TILE_SIZE));

        addBuilding(new Building(BuildingType.CON_YARD, Faction.ENEMY, map.width - 14, 5));
        addBuilding(new Building(BuildingType.WIND_TURBINE, Faction.ENEMY, map.width - 10, 5));
        addBuilding(new Building(BuildingType.REFINERY, Faction.ENEMY, map.width - 10, 8));

        var eCol = new Unit(UnitType.COLLECTOR, Faction.ENEMY, (map.width - 9) * GameMap.TILE_SIZE, 11 * GameMap.TILE_SIZE);
        eCol.autoCollect = true;
        addUnit(eCol);
        addUnit(new Unit(UnitType.RAVAGER_TANK, Faction.ENEMY, (map.width - 12) * GameMap.TILE_SIZE, 10 * GameMap.TILE_SIZE));
        if (diff == Difficulty.HARD) {
            addUnit(new Unit(UnitType.VANDAL, Faction.ENEMY, (map.width - 14) * GameMap.TILE_SIZE, 10 * GameMap.TILE_SIZE));
        }

        threatSystem.reinforcementsSpawned = 0;
        threatSystem.spawnRuinsThreats(map);

        map.updateInfluence(buildings, buildingCount);
        String diffName = diff.label();
        statusToast = "MISSION STARTED [" + diffName + "] SEED:" + mapSeed;
    }

    public void startSkirmish(int mapSizeCode, Difficulty diff, int credits, long seed) {
        if (campaignManager != null) {
            campaignManager.deactivateCampaign();
        }
        startMatch(mapSizeCode, diff, credits, seed);
        spawnSkirmishDefenders(diff);
    }

    public void spawnSkirmishDefenders(Difficulty diff) {
        UnitType[] defTypes = switch (diff) {
            case EASY -> new UnitType[] {
                UnitType.RAIDER, UnitType.RAIDER, UnitType.VANDAL
            };
            case MEDIUM -> new UnitType[] {
                UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER,
                UnitType.VANDAL, UnitType.VANDAL,
                UnitType.REAVER_BUGGY
            };
            case HARD -> new UnitType[] {
                UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER,
                UnitType.RAIDER, UnitType.RAIDER, UnitType.RAIDER,
                UnitType.VANDAL, UnitType.VANDAL, UnitType.VANDAL, UnitType.VANDAL,
                UnitType.REAVER_BUGGY, UnitType.REAVER_BUGGY
            };
        };

        Building eCon = null;
        Building eRef = null;
        for (int i = 0; i < buildingCount; i++) {
            Building b = buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) {
                if (b.type == BuildingType.CON_YARD) eCon = b;
                if (b.type == BuildingType.REFINERY) eRef = b;
            }
        }
        double bx = (eCon != null) ? eCon.getCenterX() : (map.width - 12) * GameMap.TILE_SIZE;
        double by = (eCon != null) ? eCon.getCenterY() : 8 * GameMap.TILE_SIZE;

        for (int i = 0; i < defTypes.length; i++) {
            UnitType t = defTypes[i];
            double rawSx = enemyAI.getDefStationX(i, bx, by, eRef, eCon, map);
            double rawSy = enemyAI.getDefStationY(i, bx, by, eRef, eCon, map);
            double sx = enemyAI.resolveClearX(rawSx, rawSy, map);
            double sy = enemyAI.resolveClearY(rawSx, rawSy, map);

            Unit u = new Unit(t, Faction.ENEMY, sx, sy);
            u.isBaseDefender = true;
            u.stance = Stance.HUNT;
            u.huntStationX = sx;
            u.huntStationY = sy;
            u.hasHuntStation = true;
            u.returningToHuntStation = false;
            u.hasHuntWaypoint = false;
            addUnit(u);
        }
        enemyAI.baseDefenderCount = defTypes.length;
    }

    public double gameTime = 0.0;

    private static final int ACQUIRE_SLICES = 4;
    private int acquireSlice = 0;

    private void update(double dt) {
        if (showSetupScreen) {
            if (renderer != null && map != null && camera != null) {
                renderer.render(map, camera, this);
                screen.present();
            }
            return;
        }

        if (campaignManager != null && campaignManager.briefingActive) {
            campaignManager.update(this, dt);
            if (renderer != null && map != null && camera != null) {
                renderer.render(map, camera, this);
                screen.present();
            }
            return;
        }

        if (dt <= 0.0 || dt > 0.1) dt = 0.016667;

        if (matchState != MatchState.IN_PROGRESS) {
            matchOverTimer += dt;
            updateEffectsAndWrecks(dt);
            if (matchState == MatchState.VICTORY && fxRng.chance(0.15)) {
                double fxX = camera.x + fxRng.nextDouble() * Camera.VIEW_WIDTH;
                double fxY = camera.y + fxRng.nextDouble() * Camera.VIEW_HEIGHT;
                spawnEffect(Effect.TYPE_MUZZLE, fxX, fxY, 0.4, 20.0);
            }
            renderer.render(map, camera, this);
            screen.present();
            return;
        }

        gameTime += dt;

        if (selectedUnit != null && selectedUnit.isDestroyed()) {
            selectedUnit = null;
        }
        if (selectedBuilding != null && selectedBuilding.isDestroyed()) {
            selectedBuilding = null;
        }

        double panX = 0;
        double panY = 0;
        if (keyLeft) panX -= Camera.PAN_SPEED * dt;
        if (keyRight) panX += Camera.PAN_SPEED * dt;
        if (keyUp) panY -= Camera.PAN_SPEED * dt;
        if (keyDown) panY += Camera.PAN_SPEED * dt;
        if (panX != 0 || panY != 0) {
            camera.pan(panX, panY);
        }

        compactBuildings();
        playerPowerNet = 0;
        for (int i = 0; i < buildingCount; i++) {
            var b = buildings[i];
            if (b.isDestroyed() || b.constructing || b.faction != Faction.PLAYER) continue;
            playerPowerNet += b.type.powerNet;
        }

        map.resetVisibility();
        for (int i = 0; i < buildingCount; i++) {
            var b = buildings[i];
            if (b.isDestroyed() || b.constructing || b.faction != Faction.PLAYER) continue;
            int cx = b.centerTileX();
            int cy = b.centerTileY();
            map.revealCircle(cx, cy, 9);
        }
        for (int i = 0; i < unitCount; i++) {
            var u = units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER) continue;
            int utx = (int) (u.x / GameMap.TILE_SIZE);
            int uty = (int) (u.y / GameMap.TILE_SIZE);
            map.revealCircle(utx, uty, u.type.sightRadiusTiles);
        }

        UnitNavigation.resetPathBudget();
        for (int i = unitCount - 1; i >= 0; i--) {
            var u = units[i];
            if (u.isDestroyed()) {
                switch (u.faction) {
                    case ENEMY -> unitsKilled++;
                    case PLAYER -> unitsLost++;
                    case null, default -> { }
                }
                
                if (campaignManager != null) {
                    campaignManager.onUnitDestroyed(u, u.lastDamagerFaction);
                }
                
                if (u.type.leavesWreck()) {
                    spawnWreck(u);
                }

                spawnEffect(Effect.TYPE_EXPLOSION, u.x, u.y, 0.4, u.type.radius + 11.0);
                if (selectedUnit == u) selectedUnit = null;
                units[i] = units[--unitCount];
                units[unitCount] = null;
            }
        }

        spatialGrid.rebuild(units, unitCount);

        for (int i = 0; i < unitCount; i++) {
            var u = units[i];
            u.update(dt, map);

            if (u.type == UnitType.COLLECTOR && !u.yielding) {
                boolean creditsFull = (u.faction == Faction.PLAYER && playerCredits >= MAX_CREDITS)
                        || (u.faction == Faction.ENEMY && enemyAI.credits >= MAX_CREDITS);
                if (u.harvestRetryTimer > 0.0) u.harvestRetryTimer -= dt;
                if (u.crystalLoad > 0) {
                    Building ref = findNearestRefinery(u.x, u.y, u.faction);
                    if (ref != null) {
                        double dSq = (ref.getCenterX() - u.x) * (ref.getCenterX() - u.x) + (ref.getCenterY() + 53.0 - u.y) * (ref.getCenterY() + 53.0 - u.y);
                        if (dSq <= REFINERY_DEPOSIT_RADIUS * REFINERY_DEPOSIT_RADIUS) {
                            if (u.faction == Faction.PLAYER) {
                                int deposit = addPlayerCredits(u.crystalLoad);
                                if (deposit > 0) {
                                    u.crystalLoad -= deposit;
                                    totalCreditsHarvested += deposit;
                                    statusToast = "DEPOSITED +" + deposit + " CREDITS";
                                    spawnEffect(Effect.TYPE_HARVEST_DUST, ref.getCenterX(), ref.getCenterY() + 53.0, 0.4, 28.0);
                                }
                                creditsFull = playerCredits >= MAX_CREDITS;
                            } else {
                                u.crystalLoad -= enemyAI.addCredits(u.crystalLoad);
                                creditsFull = enemyAI.credits >= MAX_CREDITS;
                            }
                            if (u.crystalLoad == 0) {
                                u.returningToRefinery = false;
                                if (u.autoCollect && !creditsFull) {
                                    inputController.orders.sendCollectorToHarvest(u);
                                }
                            } else {
                                u.returningToRefinery = true; // still holding crystals so wait
                            }
                        }
                    }
                }

                if (creditsFull) {
                    Building ref = findNearestRefinery(u.x, u.y, u.faction);
                    if (ref != null && !u.returningToRefinery) {
                        double hx = ref.getCenterX() - u.x;
                        double hy = ref.getCenterY() + 53.0 - u.y;
                        if (hx * hx + hy * hy > REFINERY_DEPOSIT_RADIUS * REFINERY_DEPOSIT_RADIUS) {
                            u.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                        }
                        u.returningToRefinery = true;
                        if (u.selected) statusToast = "CREDIT CAP REACHED - COLLECTORS HOLDING AT BASE";
                    }
                } else if (u.returningToRefinery && !u.moving && u.crystalLoad == 0 && u.autoCollect) {
                    u.returningToRefinery = false;
                    inputController.orders.sendCollectorToHarvest(u);
                }

                if (u.returningToRefinery && !u.moving) {
                    Building ref = findNearestRefinery(u.x, u.y, u.faction);
                    boolean atRefinery = false;
                    if (ref != null) {
                        double rdx = ref.getCenterX() - u.x;
                        double rdy = ref.getCenterY() + 53.0 - u.y;
                        atRefinery = rdx * rdx + rdy * rdy <= REFINERY_DEPOSIT_RADIUS * REFINERY_DEPOSIT_RADIUS;
                    }
                    if (!atRefinery) {
                        u.returningToRefinery = false;
                        if (u.crystalLoad >= Unit.MAX_LOAD) {
                            if (ref != null && u.harvestRetryTimer <= 0.0) {
                                u.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                                u.returningToRefinery = true;
                                u.harvestRetryTimer = Unit.HARVEST_RETRY_INTERVAL;
                            }
                        } else if (u.autoCollect) {
                            inputController.orders.sendCollectorToHarvest(u);
                        }
                    }
                }

                if (u.autoCollect && !u.returningToRefinery && !creditsFull) {
                    if (u.crystalLoad >= Unit.MAX_LOAD) {
                        Building ref = findNearestRefinery(u.x, u.y, u.faction);
                        if (ref != null && u.harvestRetryTimer <= 0.0) {
                            u.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                            u.returningToRefinery = true;
                            u.harvestRetryTimer = Unit.HARVEST_RETRY_INTERVAL;
                            if (u.selected && u.faction == Faction.PLAYER) statusToast = "COLLECTOR FULL - RETURNING TO REFINERY";
                        }
                    } else {
                        int utx = (int) (u.x / GameMap.TILE_SIZE);
                        int uty = (int) (u.y / GameMap.TILE_SIZE);
                        boolean onCrystals = false;
                        if (map.inBounds(utx, uty)) {
                            int cIdx = map.index(utx, uty);
                            if (map.crystals[cIdx] > 0) {
                                onCrystals = true;
                                u.harvestTargetX = u.x;
                                u.harvestTargetY = u.y;
                                u.harvestTimer += dt;
                                if (u.harvestTimer >= 1.2) {
                                    u.harvestTimer = 0.0;
                                    int mined = Math.min(Math.min(map.crystals[cIdx], Unit.HARVEST_YIELD), Unit.MAX_LOAD - u.crystalLoad);
                                    map.crystals[cIdx] -= mined;
                                    u.crystalLoad += mined;
                                    spawnEffect(Effect.TYPE_HARVEST_DUST, u.x, u.y, 0.3, 21.0);

                                    threatSystem.checkGeodeDisturbance(utx, uty, u);

                                    if (u.crystalLoad >= Unit.MAX_LOAD) {
                                        Building ref = findNearestRefinery(u.x, u.y, u.faction);
                                        if (ref != null) {
                                            u.moveTo(ref.getCenterX(), ref.getCenterY() + 53.0);
                                            u.returningToRefinery = true;
                                            if (u.selected && u.faction == Faction.PLAYER) statusToast = "COLLECTOR FULL - RETURNING TO BASE";
                                        }
                                    }
                                }
                            }
                        }

                        if (!onCrystals && !u.moving) {
                            inputController.orders.sendCollectorToHarvest(u);
                        }
                    }
                }
            }

            if (!u.yielding) {
                combatEngine.updateMechanic(u, dt);
            }

            // Medic Automation & Field Healing Loop
            combatEngine.updateMedic(u, dt);

            if (u.type.attackDamage > 0) {
                if (u.attackTargetBuilding != null && !u.attackTargetBuilding.isDestroyed()) {
                    double scanDist = (u.stance == Stance.HUNT) ? u.type.sightRange * 2.0 : u.type.attackRange;
                    Unit threat = combatEngine.findDangerousUnitInRange(u, scanDist);
                    if (threat != null && u.attackTargetUnit != threat) {
                        u.attackUnit(threat);
                    }
                }

                boolean targetless = u.attackTargetUnit == null && u.attackTargetBuilding == null;
                boolean scanTurn = targetless && ((i + acquireSlice) % ACQUIRE_SLICES == 0);
                switch (u.stance) {
                    case HUNT -> {
                        if (scanTurn) {
                            combatEngine.acquireHuntTarget(u);
                        }
                    }
                    case GUARD -> {
                        if (scanTurn) {
                            combatEngine.findNearestHostile(u);
                        }
                    }
                    case DEFAULT -> {
                        boolean provokedOnly = (u.faction == Faction.NEUTRAL && u.type != UnitType.SHARD_SKITTERER)
                            || u.type == UnitType.MEDIC;
                        if (!provokedOnly || u.threatTimer > 0.0) {
                            if (scanTurn) {
                                combatEngine.findNearestHostile(u);
                            }
                        }
                    }
                }
            }

            // Combat Fire
            if (u.type.attackDamage > 0 && u.reloadCooldown <= 0.0) {
                if (u.attackTargetUnit != null && !u.attackTargetUnit.isDestroyed()) {
                    if (!combatEngine.isDangerousTarget(u.faction, u.attackTargetUnit)) {
                        u.attackTargetUnit = null;
                    } else {
                        double dist = Math.sqrt((u.attackTargetUnit.x - u.x) * (u.attackTargetUnit.x - u.x) + (u.attackTargetUnit.y - u.y) * (u.attackTargetUnit.y - u.y));
                        if (dist <= u.type.attackRange && map.hasLineOfSight(u.x, u.y, u.attackTargetUnit.x, u.attackTargetUnit.y, null)) {
                            combatEngine.fireProjectile(u, u.attackTargetUnit.x, u.attackTargetUnit.y);
                        }
                    }
                }

                if (u.attackTargetUnit == null && u.attackTargetBuilding != null && !u.attackTargetBuilding.isDestroyed()) {
                    double dist = Math.sqrt((u.attackTargetBuilding.getCenterX() - u.x) * (u.attackTargetBuilding.getCenterX() - u.x) + (u.attackTargetBuilding.getCenterY() - u.y) * (u.attackTargetBuilding.getCenterY() - u.y));
                    if (dist <= u.type.attackRange && map.hasLineOfSight(u.x, u.y, u.attackTargetBuilding.getCenterX(), u.attackTargetBuilding.getCenterY(), u.attackTargetBuilding)) {
                        combatEngine.fireProjectile(u, u.attackTargetBuilding.getCenterX(), u.attackTargetBuilding.getCenterY());
                    }
                }
            }
        }
        acquireSlice = (acquireSlice + 1) % ACQUIRE_SLICES;

        spatialGrid.rebuild(units, unitCount);
        resolveUnitCollisions();

        UnitNavigation.updateCorridorTraffic(units, unitCount, map, dt, spatialGrid);

        threatSystem.updateScavengers(dt, map);
        combatEngine.updateDefenseTurrets(dt);
        combatEngine.updateProjectiles(dt);
        updateEffectsAndWrecks(dt);
        productionSystem.updateConstruction(consumeProductionTicks(dt));
        enemyAI.update(dt, map, this);

        if (campaignManager != null && campaignManager.campaignActive) {
            campaignManager.update(this, dt);
        } else if (matchState == MatchState.IN_PROGRESS && gameTime > 2.0) {
            int activeEnemyBuildings = 0;
            for (int i = 0; i < buildingCount; i++) {
                var b = buildings[i];
                if (b.faction == Faction.ENEMY && !b.isDestroyed()) {
                    activeEnemyBuildings++;
                }
            }

            int activePlayerBuildings = 0;
            for (int i = 0; i < buildingCount; i++) {
                var b = buildings[i];
                if (b.faction == Faction.PLAYER && !b.isDestroyed()) {
                    activePlayerBuildings++;
                }
            }

            int activePlayerUnits = 0;
            for (int i = 0; i < unitCount; i++) {
                var u = units[i];
                if (u.faction == Faction.PLAYER && !u.isDestroyed()) {
                    activePlayerUnits++;
                }
            }

            if (activeEnemyBuildings == 0) {
                matchState = MatchState.VICTORY;
                matchOverTimer = 0.0;
                statusToast = "VICTORY: CRIMSON SYNDICATE ERADICATED!";
            } else if (activePlayerBuildings == 0 && activePlayerUnits == 0) {
                matchState = MatchState.DEFEAT;
                matchOverTimer = 0.0;
                statusToast = "DEFEAT: VANGUARD FORCES DESTROYED!";
            }
        }

        if (matchState != MatchState.IN_PROGRESS) {
            matchOverTimer += dt;
            if (matchState == MatchState.VICTORY && fxRng.chance(0.15)) {
                double fxX = camera.x + fxRng.nextDouble() * Camera.VIEW_WIDTH;
                double fxY = camera.y + fxRng.nextDouble() * Camera.VIEW_HEIGHT;
                spawnEffect(Effect.TYPE_MUZZLE, fxX, fxY, 0.4, 20.0);
            }
        }

        renderer.render(map, camera, this);
        screen.present();
    }

    private int consumeProductionTicks(double dt) {
        double rate = PRODUCTION_TICKS_PER_SECOND * (playerPowerNet < 0 ? 0.5 : 1.0);
        productionClock += dt * rate;
        int ticks = (int) (productionClock + PRODUCTION_CLOCK_EPSILON);
        if (ticks > 0) {
            productionClock -= ticks;
        }
        return ticks;
    }

    private void updateEffectsAndWrecks(double dt) {
        for (int i = effectCount - 1; i >= 0; i--) {
            var fx = effects[i];
            fx.update(dt);
            if (!fx.active) {
                effectCount--;
                effects[i] = effects[effectCount];
                effects[effectCount] = fx;
            }
        }
        for (int i = wreckCount - 1; i >= 0; i--) {
            var wrk = wrecks[i];
            wrk.update(dt);
            if (!wrk.active) {
                wreckCount--;
                wrecks[i] = wrecks[wreckCount];
                wrecks[wreckCount] = wrk;
            }
        }
    }

    public void damageBuilding(Building b, int dmg, Faction fromFaction) {
        if (b == null || b.isDestroyed()) return;
        b.takeDamage(dmg, fromFaction);
        if (b.isDestroyed()) {
            spawnEffect(Effect.TYPE_EXPLOSION, b.getCenterX(), b.getCenterY(), 0.6, 64.0);
            map.updateInfluence(buildings, buildingCount);
            if (b.faction == Faction.ENEMY && fromFaction == Faction.PLAYER) {
                buildingsDestroyed++;
            }
            if (selectedBuilding == b) {
                selectedBuilding = null;
            }
        }
    }

    public void compactBuildings() {
        boolean compacted = false;
        for (int i = buildingCount - 1; i >= 0; i--) {
            var b = buildings[i];
            if (b.isDestroyed()) {
                if (selectedBuilding == b) {
                    selectedBuilding = null;
                }
                buildings[i] = buildings[--buildingCount];
                buildings[buildingCount] = null;
                compacted = true;
            }
        }
        if (compacted) {
            map.updateInfluence(buildings, buildingCount);
        }
    }

    public void resolveUnitCollisions() {
        for (int pass = 0; pass < 2; pass++) {
            if (pass > 0) {
                spatialGrid.rebuild(units, unitCount);
            }
            for (int i = 0; i < unitCount; i++) {
                var uA = units[i];
                if (uA == null || uA.isDestroyed()) continue;
                double rA = uA.type.radius * 0.85;
                double massA = uA.type.mass;

                int cx = spatialGrid.getCellX(uA.x);
                int cy = spatialGrid.getCellY(uA.y);
                int minCx = Math.max(0, cx - 1);
                int maxCx = Math.min(spatialGrid.cols - 1, cx + 1);
                int minCy = Math.max(0, cy - 1);
                int maxCy = Math.min(spatialGrid.rows - 1, cy + 1);

                for (int ncy = minCy; ncy <= maxCy; ncy++) {
                    int rowOffset = ncy * spatialGrid.cols;
                    for (int ncx = minCx; ncx <= maxCx; ncx++) {
                        int j = spatialGrid.head[rowOffset + ncx];
                        while (j != -1) {
                            if (j > i) {
                                var uB = units[j];
                                if (uB != null && !uB.isDestroyed()) {
                                    double rB = uB.type.radius * 0.85;
                                    double minDist = rA + rB;

                                    double dx = uA.x - uB.x;
                                    double dy = uA.y - uB.y;
                                    if (Math.abs(dx) < minDist && Math.abs(dy) < minDist) {
                                        double distSq = dx * dx + dy * dy;
                                        if (distSq < minDist * minDist) {
                                            double dist = Math.sqrt(distSq);
                                            double overlap = minDist - dist;
                                            double nx, ny;
                                            if (dist > 0.001) {
                                                nx = dx / dist;
                                                ny = dy / dist;
                                            } else {
                                                double angle = ((i * 37 + j * 17) & 7) * (Math.PI / 4.0);
                                                nx = Math.cos(angle);
                                                ny = Math.sin(angle);
                                            }

                                            double massB = uB.type.mass;
                                            double effMassA = massA * (uA.moving ? 1.0 : 1.5);
                                            double effMassB = massB * (uB.moving ? 1.0 : 1.5);
                                            double ratioA = effMassB / (effMassA + effMassB);
                                            double ratioB = effMassA / (effMassA + effMassB);

                                            if (uB.faction == uA.faction) {
                                                boolean parkedA = !uA.moving && !uA.yielding && UnitNavigation.inSingleLane(uA, map);
                                                boolean parkedB = !uB.moving && !uB.yielding && UnitNavigation.inSingleLane(uB, map);
                                                if (parkedA && !parkedB) {
                                                    ratioA = 0.0;
                                                    ratioB = 1.0;
                                                } else if (parkedB && !parkedA) {
                                                    ratioA = 1.0;
                                                    ratioB = 0.0;
                                                }
                                            }

                                            double push = overlap * 0.55;
                                            uA.x += nx * push * ratioA;
                                            uA.y += ny * push * ratioA;
                                            uB.x -= nx * push * ratioB;
                                            uB.y -= ny * push * ratioB;
                                        }
                                    }
                                }
                            }
                            j = spatialGrid.next[j];
                        }
                    }
                }
            }
        }

        for (int i = 0; i < unitCount; i++) {
            var u = units[i];
            if (u == null || u.isDestroyed()) continue;
            UnitNavigation.resolveObstacleOverlap(u, map, u.getObstacleClearance(), null);
        }
        spatialGrid.rebuild(units, unitCount);
    }

    public Building findNearestRefinery(double px, double py, Faction faction) {
        Building best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < buildingCount; i++) {
            var b = buildings[i];
            if (b.isDestroyed() || b.constructing || b.faction != faction || b.type != BuildingType.REFINERY) continue;
            double d = (b.getCenterX() - px) * (b.getCenterX() - px) + (b.getCenterY() - py) * (b.getCenterY() - py);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = b;
            }
        }
        return best;
    }

    public Building findNearestBuilding(double px, double py, Faction faction) {
        Building best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < buildingCount; i++) {
            var b = buildings[i];
            if (b.isDestroyed() || b.faction != faction) continue;
            double d = (b.getCenterX() - px) * (b.getCenterX() - px) + (b.getCenterY() - py) * (b.getCenterY() - py);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = b;
            }
        }
        return best;
    }

    public boolean withinBaseLeash(double px, double py, Faction faction) {
        Building b = findNearestBuilding(px, py, faction);
        if (b == null) return true;
        double dx = b.getCenterX() - px;
        double dy = b.getCenterY() - py;
        double leash = Unit.MECHANIC_LEASH_RANGE;
        return dx * dx + dy * dy <= leash * leash;
    }

    public boolean hasRadar(Faction f) {
        if (f == Faction.PLAYER && playerPowerNet < 0) return false;
        for (int i = 0; i < buildingCount; i++) {
            var b = buildings[i];
            if (!b.isDestroyed() && !b.constructing && b.faction == f && b.type == BuildingType.RADAR) {
                return true;
            }
        }
        return false;
    }

    public boolean canProduce(BuildingType bt) { return productionSystem.canProduce(bt); }
    public boolean canProduce(UnitType ut) { return productionSystem.canProduce(ut); }
    public BuildingType getAvailableBuilding(int slot) { return productionSystem.getAvailableBuilding(slot); }
    public UnitType getAvailableUnit(int slot) { return productionSystem.getAvailableUnit(slot); }
    public void trainUnit(UnitType ut) { productionSystem.trainUnit(ut); }
    public void cancelUnit(UnitType ut) { productionSystem.cancelUnit(ut); }
    public void sellSelectedBuilding() { productionSystem.sellSelectedBuilding(); }
    public void repairSelectedBuilding() { productionSystem.repairSelectedBuilding(); }
    public Building findProductionBuilding(BuildingType type, Faction faction) { return productionSystem.findProductionBuilding(type, faction); }
    public void setPrimaryBuilding(Building target) { productionSystem.setPrimaryBuilding(target); }
    public Building findBuilding(BuildingType type, Faction faction) { return productionSystem.findBuilding(type, faction); }
    public int countBuildings(BuildingType type, Faction faction) { return productionSystem.countBuildings(type, faction); }
    public int countUnits(UnitType type, Faction faction) { return productionSystem.countUnits(type, faction); }
    public int countQueuedUnits(UnitType type, Faction faction) { return productionSystem.countQueuedUnits(type, faction); }

    public boolean isDangerousTarget(Faction myFaction, Unit target) { return combatEngine.isDangerousTarget(myFaction, target); }
    public boolean isRivalBuilding(Faction myFaction, Building b) { return combatEngine.isRivalBuilding(myFaction, b); }
    public Unit findDangerousUnitInRange(Unit u, double range) { return combatEngine.findDangerousUnitInRange(u, range); }
    public void acquireHuntTarget(Unit u) { combatEngine.acquireHuntTarget(u); }
    public void applySplash(double x, double y, double radius, int dmg, Faction fromFaction, Unit directVictim) {
        combatEngine.applySplash(x, y, radius, dmg, fromFaction, directVictim);
    }

    public void issueRightClickOrder(double worldX, double worldY) { inputController.orders.issueRightClickOrder(worldX, worldY); }
    public void stopSelectedUnits() { inputController.orders.stopSelectedUnits(); }
    public void guardSelectedUnits() { inputController.orders.guardSelectedUnits(); }
    public void huntSelectedUnits() { inputController.orders.huntSelectedUnits(); }
    public void sendCollectorToHarvest(Unit u) { inputController.orders.sendCollectorToHarvest(u); }
    public void toggleCollectSelectedCollector() { inputController.orders.toggleCollectSelectedCollector(); }
    public void unloadSelectedCollector() { inputController.orders.unloadSelectedCollector(); }
    public void toggleRepairSelectedMechanic() { inputController.orders.toggleRepairSelectedMechanic(); }
}
