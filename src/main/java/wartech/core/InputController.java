package wartech.core;

// Mouse dragging, hotkeys, sidebar commands, inspector buttons, and orders

import wartech.building.Building;
import wartech.campaign.CampaignManager;
import wartech.combat.Effect;
import wartech.map.GameMap;
import wartech.render.Renderer;
import wartech.unit.Unit;

public class InputController {

    private final WarTech game;

    private boolean objectivesKeyLatched = false;

    private Building pendingSell = null;
    private double pendingSellAt = 0.0;
    private static final double SELL_CONFIRM_WINDOW = 3.0;

    public final UnitOrders orders;
    public final SetupInput setup;

    public InputController(WarTech game) {
        this.game = game;
        this.orders = new UnitOrders(game);
        this.setup = new SetupInput(game);
    }

    public void onMouseDown(int x, int y, int button) {
        game.mouseX = x;
        game.mouseY = y;

        if (game.showSetupScreen) {
            setup.handleClick(x, y, button);
            return;
        }

        if (game.campaignManager != null && game.campaignManager.briefingActive) {
            handleBriefingClick(x, y, button);
            return;
        }

        if (game.matchState != MatchState.IN_PROGRESS) {
            handleDebriefClick(x, y, button);
            return;
        }

        // Minimap
        if (x >= Renderer.MINIMAP_X && x < Renderer.MINIMAP_X + Renderer.MINIMAP_SIZE &&
            y >= Renderer.MINIMAP_Y && y < Renderer.MINIMAP_Y + Renderer.MINIMAP_SIZE) {
            double scale = (double) Renderer.MINIMAP_SIZE / game.map.width;
            double wx = (x - Renderer.MINIMAP_X) / scale * GameMap.TILE_SIZE;
            double wy = (y - Renderer.MINIMAP_Y) / scale * GameMap.TILE_SIZE;
            if (button == MouseButton.LEFT) {
                if (game.hasRadar(Faction.PLAYER)) {
                    game.camera.centerOn(wx, wy);
                } else {
                    game.statusToast = "NO RADAR LINK - BUILD A RADAR OUTPOST";
                }
            } else if (button == MouseButton.RIGHT) {
                if (game.hasRadar(Faction.PLAYER)) {
                    orders.issueRightClickOrder(wx, wy);
                }
            }
            return;
        }

        // Sidebar 
        if (x >= Renderer.SIDEBAR_X) {
            handleSidebarClick(x, y, button);
            return;
        }

        // Directives
        if (game.campaignManager != null && game.campaignManager.campaignActive) {
            int objX = Renderer.OBJ_X;
            int objY = Renderer.OBJ_Y;
            if (game.showObjectives) {
                if (x >= objX && x < objX + Renderer.OBJ_W && y >= objY && y < objY + Renderer.OBJ_H) {
                    boolean onHeader = y < objY + Renderer.OBJ_HEADER_H;
                    if (button == MouseButton.LEFT && onHeader) {
                        game.toggleObjectives();
                    }
                    return;
                }
            } else {
                if (x >= objX && x < objX + Renderer.OBJ_BADGE_W && y >= objY && y < objY + Renderer.OBJ_BADGE_H) {
                    if (button == MouseButton.LEFT) game.toggleObjectives();
                    return;
                }
            }
        }

        // Left click in tactical viewport
        if (button == MouseButton.LEFT) {
            if (game.placingBuilding != null) {
                if (!game.canProduce(game.placingBuilding)) {
                    game.placingBuilding = null;
                    game.statusToast = "PREREQUISITE LOST";
                    return;
                }
                if (game.buildingCount >= WarTech.MAX_BUILDINGS) {
                    game.placingBuilding = null;
                    game.statusToast = "BASE CAPACITY FULL";
                    return;
                }
                double wx = game.camera.screenToWorldX(x);
                double wy = game.camera.screenToWorldY(y);
                int gtx = (int) (wx / GameMap.TILE_SIZE);
                int gty = (int) (wy / GameMap.TILE_SIZE);
                if (game.map.canBuildAt(gtx, gty, game.placingBuilding, true,
                        game.buildings, game.buildingCount, game.units, game.unitCount)) {
                    var b = new Building(game.placingBuilding, Faction.PLAYER, gtx, gty);
                    b.constructing = true;
                    b.buildSpent = 0;
                    b.constructionProgress = 0.0;
                    b.hp = Math.max(10, b.maxHp / 10);
                    if (game.addBuilding(b)) {
                        game.map.updateInfluence(game.buildings, game.buildingCount);
                        game.statusToast = b.type.displayNameUpper + " FOUNDATION LAID";
                    } else {
                        game.statusToast = "BASE CAPACITY FULL";
                    }
                    game.placingBuilding = null;
                } else {
                    game.statusToast = game.map.buildRejection(gtx, gty, game.placingBuilding, true,
                        game.buildings, game.buildingCount, game.units, game.unitCount).label();
                }
                return;
            }

            game.isDragging = true;
            game.dragStartX = x;
            game.dragStartY = y;
        }
        // Right click -> Orders
        else if (button == MouseButton.RIGHT) {
            double worldX = game.camera.screenToWorldX(x);
            double worldY = game.camera.screenToWorldY(y);
            orders.issueRightClickOrder(worldX, worldY);
        }
    }


    public void onMouseUp(int x, int y, int button) {
        if (button == MouseButton.LEFT && game.isDragging) {
            game.isDragging = false;
            if (x > Renderer.VIEW_WIDTH - 1) x = Renderer.VIEW_WIDTH - 1;
            if (x < 0) x = 0;
            if (y > Renderer.SCREEN_HEIGHT - 1) y = Renderer.SCREEN_HEIGHT - 1;
            if (y < 0) y = 0;
            int x0 = Math.min(game.dragStartX, x);
            int y0 = Math.min(game.dragStartY, y);
            int x1 = Math.max(game.dragStartX, x);
            int y1 = Math.max(game.dragStartY, y);

            double wx0 = game.camera.screenToWorldX(x0);
            double wy0 = game.camera.screenToWorldY(y0);
            double wx1 = game.camera.screenToWorldX(x1);
            double wy1 = game.camera.screenToWorldY(y1);

            boolean isSingleClick = (x1 - x0 < 6 && y1 - y0 < 6);

            if (isSingleClick) {
                // 1. Check if clicked on ANY unit
                Unit clickedUnit = null;
                double clickTol = 32.0 / game.camera.zoom;
                double bestDist = 69.0 / game.camera.zoom;
                for (int i = 0; i < game.unitCount; i++) {
                    var u = game.units[i];
                    if (u.isDestroyed()) continue;
                    if (u.faction != Faction.PLAYER && !game.map.visibleAt(u.x, u.y)) continue;
                    double dist = Math.sqrt((u.x - wx0) * (u.x - wx0) + (u.y - wy0) * (u.y - wy0));
                    if (dist <= u.type.radius + clickTol && dist < bestDist) {
                        bestDist = dist;
                        clickedUnit = u;
                    }
                }

                if (clickedUnit != null) {
                    game.selectedBuilding = null;
                    for (int i = 0; i < game.unitCount; i++) {
                        game.units[i].selected = false;
                    }
                    if (clickedUnit.faction == Faction.PLAYER) {
                        clickedUnit.selected = true;
                    }
                    game.selectedUnit = clickedUnit;
                    game.statusToast = clickedUnit.type.displayNameUpper + " INSPECTED";
                    return;
                }

                // 2. Check if clicked on ANY building
                Building clickedBuilding = null;
                for (int i = 0; i < game.buildingCount; i++) {
                    var b = game.buildings[i];
                    if (b.isDestroyed()) continue;
                    if (b.faction != Faction.PLAYER && !game.map.exploredAt(b.getCenterX(), b.getCenterY())) continue;
                    if (b.containsWorldPoint(wx0, wy0)) {
                        clickedBuilding = b;
                        break;
                    }
                }

                if (clickedBuilding != null) {
                    game.selectedBuilding = clickedBuilding;
                    game.selectedUnit = null;
                    for (int i = 0; i < game.unitCount; i++) {
                        game.units[i].selected = false;
                    }
                    game.statusToast = clickedBuilding.type.displayNameUpper + " INSPECTED";
                    return;
                }

                // 3. Clicked empty terrain so deselect
                game.selectedUnit = null;
                game.selectedBuilding = null;
                for (int i = 0; i < game.unitCount; i++) {
                    game.units[i].selected = false;
                }
            } else {
                // Drag selection box so select player units inside
                game.selectedBuilding = null;
                game.selectedUnit = null;
                int count = 0;
                for (int i = 0; i < game.unitCount; i++) {
                    var u = game.units[i];
                    if (u.isDestroyed() || u.faction != Faction.PLAYER) {
                        u.selected = false;
                        continue;
                    }
                    boolean inside = (u.x >= wx0 && u.x <= wx1 && u.y >= wy0 && u.y <= wy1);
                    u.selected = inside;
                    if (inside) {
                        count++;
                        if (game.selectedUnit == null) {
                            game.selectedUnit = u;
                        }
                    }
                }
                if (count > 0) {
                    game.statusToast = count + " UNITS SELECTED";
                }
            }
        }
    }

    public void onMouseMove(int x, int y) {
        game.mouseX = x;
        game.mouseY = y;
    }

    public void cancelInput() {
        releaseInput();
        game.placingBuilding = null;
        pendingSell = null;
    }

    public void releaseInput() {
        game.isDragging = false;
        game.keyLeft = false;
        game.keyRight = false;
        game.keyUp = false;
        game.keyDown = false;
        game.ctrlHeld = false;
        objectivesKeyLatched = false;
    }

    private void confirmSellSelectedBuilding() {
        Building b = game.selectedBuilding;
        if (b == null || b.isDestroyed()) return;
        boolean armed = pendingSell == b && game.gameTime >= pendingSellAt
            && game.gameTime - pendingSellAt <= SELL_CONFIRM_WINDOW;
        if (armed) {
            pendingSell = null;
            game.sellSelectedBuilding();
        } else {
            pendingSell = b;
            pendingSellAt = game.gameTime;
            game.statusToast = "CLICK SELL AGAIN TO CONFIRM";
        }
    }

    public void clearSelection() {
        game.selectedUnit = null;
        game.selectedBuilding = null;
        for (int i = 0; i < game.unitCount; i++) {
            game.units[i].selected = false;
        }
    }

    public void handleDebriefClick(int x, int y, int button) {
        int btn1X = Renderer.DEBRIEF_BTN1_X;
        int btn1W = Renderer.DEBRIEF_BTN1_W;
        int btn2X = Renderer.DEBRIEF_BTN2_X;
        int btn2W = Renderer.DEBRIEF_BTN2_W;
        int btnH = Renderer.DEBRIEF_BTN_H;
        int btnY = Renderer.DEBRIEF_BTN_Y;

        // Button 1: [PLAY AGAIN] or [NEXT OPERATION / RETRY OPERATION]
        if (button == MouseButton.LEFT && x >= btn1X && x < btn1X + btn1W && y >= btnY && y < btnY + btnH) {
            if (game.campaignManager != null && game.campaignManager.campaignActive) {
                if (game.matchState == MatchState.VICTORY) {
                    game.campaignManager.advanceMission(game);
                } else {
                    game.campaignManager.replayMission(game);
                }
            } else {
                game.startSkirmish(game.selectedMapSize, game.selectedDifficulty, game.startingCredits, game.mapSeed);
            }
            return;
        }

        // Button 2: [MISSION SETUP] or [MISSION SELECT]
        if (button == MouseButton.LEFT && x >= btn2X && x < btn2X + btn2W && y >= btnY && y < btnY + btnH) {
            if (game.campaignManager != null && game.campaignManager.campaignActive) {
                game.campaignManager.openBriefing(game.campaignManager.currentMissionIndex);
            } else {
                game.showSetupScreen = true;
                game.matchStarted = false;
            }
        }
    }


    public void handleBriefingClick(int x, int y, int button) {
        if (button != MouseButton.LEFT) return;
        int bx = Renderer.BRIEFING_X;
        int by = Renderer.BRIEFING_Y;  // not used yet, but we will need it when we fix fonts

        // 1. Theater Navigation Tabs (Act I / Act II / Act III)
        int theaterW = 256;
        int theaterH = Renderer.BRIEFING_THEATER_H;
        int theaterY = Renderer.BRIEFING_THEATER_Y;
        for (int t = 0; t < CampaignManager.THEATER_COUNT; t++) {
            int tx = bx + 24 + t * 264;
            if (x >= tx && x < tx + theaterW && y >= theaterY && y < theaterY + theaterH) {
                if (game.campaignManager.isTheaterUnlocked(t)) {
                    game.campaignManager.selectTheater(t);
                } else {
                    game.statusToast = "THEATER LOCKED: COMPLETE PRECEDING OPERATIONS";
                }
                return;
            }
        }

        // 2. Sub-Operation Tabs for the active Theater
        int act = game.campaignManager.currentTheater;
        int startOp = CampaignManager.getTheaterStartMission(act);
        int opCount = CampaignManager.getTheaterMissionCount(act);
        int tabGap = (act == 2) ? 10 : 8;
        int tabW = (act == 2) ? 254 : 190;
        int tabH = Renderer.BRIEFING_TAB_H;
        int tabY = Renderer.BRIEFING_TAB_Y;
        for (int i = 0; i < opCount; i++) {
            int opIdx = startOp + i;
            int tabX = bx + 24 + i * (tabW + tabGap);
            if (x >= tabX && x < tabX + tabW && y >= tabY && y < tabY + tabH) {
                if (opIdx <= game.campaignManager.highestUnlockedMission) {
                    game.campaignManager.openBriefing(opIdx);
                } else {
                    game.statusToast = "OPERATION LOCKED: COMPLETE PREVIOUS OPERATIONS";
                }
                return;
            }
        }

        // 2. Button 1: [DEPLOY OPERATION]
        int btn1X = Renderer.BRIEFING_DEPLOY_X;
        int btn1Y = Renderer.BRIEFING_DEPLOY_Y;
        int btn1W = Renderer.BRIEFING_DEPLOY_W;
        int btn1H = Renderer.BRIEFING_DEPLOY_H;
        if (x >= btn1X && x < btn1X + btn1W && y >= btn1Y && y < btn1Y + btn1H) {
            game.campaignManager.deployCurrentMission(game);
            return;
        }

        // 3. Button 2: [ABORT TO MENU]
        int btn2X = Renderer.BRIEFING_ABORT_X;
        int btn2Y = Renderer.BRIEFING_ABORT_Y;
        int btn2W = Renderer.BRIEFING_ABORT_W;
        int btn2H = Renderer.BRIEFING_ABORT_H;
        if (x >= btn2X && x < btn2X + btn2W && y >= btn2Y && y < btn2Y + btn2H) {
            game.campaignManager.closeBriefing();
            game.showSetupScreen = true;
            game.statusToast = "OPERATION ABORTED";
            return;
        }
    }


    public void handleSidebarClick(int x, int y, int button) {
        // Zoom Bar controls below minimap: [ - ] 50% [ + ] (y: 252 - 274)
        if (y >= Renderer.ZOOM_BAR_Y0 && y < Renderer.ZOOM_BAR_Y1) {
            if (button == MouseButton.LEFT) {
                if (x >= Renderer.ZOOM_MINUS_X0 && x < Renderer.ZOOM_MINUS_X1) {
                    game.zoomOut();
                } else if (x >= Renderer.ZOOM_PLUS_X0 && x < Renderer.ZOOM_PLUS_X1) {
                    game.zoomIn();
                } else if (x >= Renderer.ZOOM_MODE_X0 && x < Renderer.ZOOM_MODE_X1) {
                    if (game.camera.zoom < 0.45) game.camera.setZoom(Camera.ZOOM_DEFAULT);
                    else if (game.camera.zoom < 0.95) game.camera.setZoom(Camera.ZOOM_MAX);
                    else game.camera.setZoom(Camera.ZOOM_MIN);
                    game.statusToast = "ZOOM: " + game.camera.getZoomPercent() + "%";
                }
            }
            return;
        }

        if (y >= Renderer.SIDEBAR_TABS_Y0 && y < Renderer.SIDEBAR_TABS_Y1) {
            if (button == MouseButton.LEFT) {
                game.activeTab = (x < Renderer.SIDEBAR_COL_SPLIT_X) ? 0 : 1;
            }
            return;
        }

        if (y >= Renderer.BUILD_GRID_Y0 && y < Renderer.BUILD_GRID_Y1) {
            int row = (y - Renderer.BUILD_GRID_Y0) / Renderer.BUILD_GRID_ROW_H;
            int col = (x < Renderer.SIDEBAR_COL_SPLIT_X) ? 0 : 1;
            if (col >= 0 && col < Renderer.BUILD_GRID_COLS && row >= 0 && row < Renderer.BUILD_GRID_ROWS) {
                int slot = row * 2 + col;
                if (game.activeTab == 0) {
                    var bt = game.getAvailableBuilding(slot);
                    if (bt != null) {
                        if (button == MouseButton.LEFT) {
                            if (game.playerCredits > 0 || bt.cost == 0) {
                                game.placingBuilding = bt;
                                game.statusToast = "PLACE " + bt.displayNameUpper;
                            } else {
                                game.statusToast = "INSUFFICIENT CREDITS";
                            }
                        } else if (button == MouseButton.RIGHT) {
                            for (int bi = 0; bi < game.buildingCount; bi++) {
                                var b = game.buildings[bi];
                                if (b.faction == Faction.PLAYER && b.type == bt && b.constructing && !b.isDestroyed()) {
                                    game.addPlayerCredits(b.buildSpent);
                                    b.hp = 0;
                                    b.active = false;
                                    game.statusToast = bt.displayNameUpper + " CANCELLED";
                                    game.map.updateInfluence(game.buildings, game.buildingCount);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    var ut = game.getAvailableUnit(slot);
                    if (ut != null) {
                        if (button == MouseButton.LEFT) {
                            game.trainUnit(ut);
                        } else if (button == MouseButton.RIGHT) {
                            game.cancelUnit(ut);
                        }
                    }
                }
            }
            return;
        }

        if (y >= Renderer.INSPECTOR_Y0 && y < Renderer.INSPECTOR_Y1) {
            handleInspectorClick(x, y, button);
        }
    }


    public void handleInspectorClick(int x, int y, int button) {
        if (button != MouseButton.LEFT) return;
        if (game.selectedUnit != null && game.selectedUnit.faction == Faction.PLAYER) {
            if (x >= Renderer.INSPECTOR_BTN1_X0 && x < Renderer.INSPECTOR_BTN1_X1) {
                orders.stopSelectedUnits();
            } else if (x >= Renderer.INSPECTOR_BTN2_X0 && x < Renderer.INSPECTOR_BTN2_X1) {
                orders.guardSelectedUnits();
            } else if (x >= Renderer.INSPECTOR_BTN3_X0 && x < Renderer.INSPECTOR_BTN3_X1) {
                switch (game.selectedUnit.type) {
                    case COLLECTOR -> orders.toggleCollectSelectedCollector();
                    case MECHANIC -> orders.toggleRepairSelectedMechanic();
                    case null, default -> orders.huntSelectedUnits();
                }
            }
        } else if (game.selectedBuilding != null && game.selectedBuilding.faction == Faction.PLAYER) {
            if (game.selectedBuilding.type.canProduceUnits()) {
                if (x >= Renderer.INSPECTOR_BTN1_X0 && x < Renderer.INSPECTOR_BTN1_X1) {
                    game.repairSelectedBuilding();
                } else if (x >= Renderer.INSPECTOR_BTN2_X0 && x < Renderer.INSPECTOR_BTN2_X1) {
                    if (game.selectedBuilding.hasRallyPoint) {
                        game.selectedBuilding.clearRallyPoint();
                        game.statusToast = game.selectedBuilding.type.displayNameUpper + " RALLY POINT CLEARED";
                        game.spawnEffect(Effect.TYPE_EXPLOSION, game.selectedBuilding.getCenterX(), game.selectedBuilding.getCenterY(), 0.15, 8.0);
                    } else {
                        game.statusToast = "RIGHT-CLICK MAP TO SET RALLY POINT";
                    }
                } else if (x >= Renderer.INSPECTOR_BTN3_X0 && x < Renderer.INSPECTOR_BTN3_X1) {
                    confirmSellSelectedBuilding();
                }
            } else {
                if (x >= Renderer.INSPECTOR_BTN1_X0 && x < Renderer.INSPECTOR_WIDE1_X1) {
                    game.repairSelectedBuilding();
                } else if (x >= Renderer.INSPECTOR_WIDE2_X0 && x < Renderer.INSPECTOR_WIDE2_X1) {
                    confirmSellSelectedBuilding();
                }
            }
        }
    }


    public void setKey(int code, boolean isDown) {
        if (code == KeyCode.CONTROL) {
            game.ctrlHeld = isDown;
            return;
        }

        if (!isDown && code == KeyCode.O) {
            objectivesKeyLatched = false;
        }

        if (game.campaignManager != null && game.campaignManager.briefingActive) {
            if (isDown) {
                if (code == KeyCode.ENTER || code == KeyCode.SPACE) { // Deploy
                    game.campaignManager.deployCurrentMission(game);
                    return;
                }
                if (code == KeyCode.ESCAPE) { // Abort to Menu
                    game.campaignManager.closeBriefing();
                    game.showSetupScreen = true;
                    return;
                }
            }
            return;
        }

        if (game.showSetupScreen) {
            if (isDown && (code == KeyCode.ENTER || code == KeyCode.SPACE)) {
                setup.launchSelectedMode();
            }
            return;
        }

        // Escape unwinds the most specific pending interaction first.
        if (isDown && code == KeyCode.ESCAPE) {
            if (game.placingBuilding != null) {
                game.placingBuilding = null;
                game.statusToast = "PLACEMENT CANCELLED";
                return;
            }
            if (game.isDragging) {
                game.isDragging = false;
                return;
            }
            if (game.selectedUnit != null || game.selectedBuilding != null) {
                clearSelection();
                game.statusToast = "SELECTION CLEARED";
                return;
            }
            return;
        }

        if (isDown) {
            if (code == KeyCode.O) {
                if (!objectivesKeyLatched) {
                    objectivesKeyLatched = true;
                    game.toggleObjectives();
                }
                return;
            }
            if (code == KeyCode.BRACKET_LEFT || code == KeyCode.MINUS || code == KeyCode.NUMPAD_SUBTRACT) {
                game.zoomOut();
                return;
            }
            if (code == KeyCode.BRACKET_RIGHT || code == KeyCode.EQUALS || code == KeyCode.NUMPAD_ADD) {
                game.zoomIn();
                return;
            }
            if (code >= KeyCode.DIGIT0 && code <= KeyCode.DIGIT9) {
                if (game.matchState == MatchState.IN_PROGRESS) {
                    handleGroupKey(code - KeyCode.DIGIT0);
                }
                return;
            }
        }

        switch (code) {
            case KeyCode.LEFT, KeyCode.A -> game.keyLeft = isDown;
            case KeyCode.RIGHT, KeyCode.D -> game.keyRight = isDown;
            case KeyCode.UP, KeyCode.W -> game.keyUp = isDown;
            case KeyCode.DOWN, KeyCode.S -> game.keyDown = isDown;
            default -> { }
        }
    }

    private void handleGroupKey(int digit) {
        if (game.ctrlHeld) {
            assignGroup(digit);
        } else if (digit >= 1 && digit <= Unit.GROUP_MAX) {
            recallGroup(digit);
        }
    }

    private void assignGroup(int digit) {
        if (digit == Unit.GROUP_NONE) {
            int cleared = 0;
            for (int i = 0; i < game.unitCount; i++) {
                var u = game.units[i];
                if (u.selected && u.faction == Faction.PLAYER && !u.isDestroyed() && u.group != Unit.GROUP_NONE) {
                    u.group = Unit.GROUP_NONE;
                    cleared++;
                }
            }
            game.statusToast = cleared > 0 ? "REMOVED FROM GROUP" : "NO UNITS SELECTED";
            return;
        }
        int count = 0;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER) continue;
            if (u.selected) {
                u.group = digit;
                count++;
            } else if (u.group == digit) {
                u.group = Unit.GROUP_NONE;
            }
        }
        game.statusToast = count > 0 ? "GROUP " + digit + " ASSIGNED (" + count + ")" : "NO UNITS SELECTED";
    }

    private void recallGroup(int digit) {
        int count = 0;
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER || u.group != digit) continue;
            count++;
            sumX += u.x;
            sumY += u.y;
        }
        if (count == 0) {
            game.statusToast = "GROUP " + digit + " EMPTY";
            return;
        }
        if (selectionMatchesGroup(digit)) {
            game.camera.centerOn(sumX / count, sumY / count);
            game.statusToast = "GROUP " + digit;
            return;
        }
        game.selectedBuilding = null;
        game.selectedUnit = null;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER) {
                u.selected = false;
                continue;
            }
            u.selected = u.group == digit;
            if (u.selected && game.selectedUnit == null) {
                game.selectedUnit = u;
            }
        }
        game.statusToast = "GROUP " + digit + " (" + count + ")";
    }

    private boolean selectionMatchesGroup(int digit) {
        if (game.selectedBuilding != null) return false;
        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.PLAYER) continue;
            if (u.selected != (u.group == digit)) return false;
        }
        return true;
    }
}
