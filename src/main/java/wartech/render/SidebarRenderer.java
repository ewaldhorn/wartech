package wartech.render;

// Command sidebar: minimap, zoom bar, and build/unit buttons. Owns the minimap cache.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Camera;
import wartech.core.Faction;
import wartech.core.Memory;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.map.GameMap;
import static wartech.render.RenderShared.*;
import static wartech.render.Renderer.*;

public final class SidebarRenderer {
    private final Surface screen;

    public SidebarRenderer(Surface screen) {
        this.screen = screen;
    }

    private final int[] minimapCache = new int[MINIMAP_SIZE * MINIMAP_SIZE];
    private final Building[] constructingByType = new Building[BuildingType.values().length];
    private int minimapCacheW = -1;
    private int minimapCacheH = -1;
    private double minimapCacheAt = -1.0e9;
    private static final double MINIMAP_REFRESH_SECONDS = 0.25;

    public void renderSidebar(GameMap map, Camera camera, WarTech game) {
        screen.fillRect(SIDEBAR_X, 0, SIDEBAR_WIDTH, SCREEN_HEIGHT, 0xFF141210);
        screen.drawLine(SIDEBAR_X, 0, SIDEBAR_X, SCREEN_HEIGHT, 0xFF352B20);
        screen.drawLine(SIDEBAR_X + 1, 0, SIDEBAR_X + 1, SCREEN_HEIGHT, 0xFF584834);

        renderMinimap(map, camera, game);

        renderZoomBar(camera);

        int tabY = 276;
        boolean isBuildTab = (game.activeTab == 0);

        screen.fillRect(SIDEBAR_X + 12, tabY, 144, 32, isBuildTab ? Palette.UMBER : 0xFF1A1612);
        screen.drawLine(SIDEBAR_X + 12, tabY, SIDEBAR_X + 12 + 144, tabY, isBuildTab ? Palette.BLUE_LIGHT : Palette.UMBER);
        screen.drawLine(SIDEBAR_X + 12, tabY, SIDEBAR_X + 12, tabY + 32, isBuildTab ? Palette.BLUE_LIGHT : Palette.UMBER);
        screen.drawLine(SIDEBAR_X + 12 + 144, tabY, SIDEBAR_X + 12 + 144, tabY + 32, Palette.BLACK_PANEL);
        BitmapFont.drawText(screen, "BASES", SIDEBAR_X + 54, tabY + 9, isBuildTab ? Palette.WHITE : 0xFF807060, 2);

        screen.fillRect(SIDEBAR_X + 164, tabY, 144, 32, !isBuildTab ? Palette.UMBER : 0xFF1A1612);
        screen.drawLine(SIDEBAR_X + 164, tabY, SIDEBAR_X + 164 + 144, tabY, !isBuildTab ? Palette.BLUE_LIGHT : Palette.UMBER);
        screen.drawLine(SIDEBAR_X + 164, tabY, SIDEBAR_X + 164, tabY + 32, !isBuildTab ? Palette.BLUE_LIGHT : Palette.UMBER);
        screen.drawLine(SIDEBAR_X + 164 + 144, tabY, SIDEBAR_X + 164 + 144, tabY + 32, Palette.BLACK_PANEL);
        BitmapFont.drawText(screen, "UNITS", SIDEBAR_X + 206, tabY + 9, !isBuildTab ? Palette.WHITE : 0xFF807060, 2);

        if (isBuildTab) {
            renderBuildButtons(game, BUILD_GRID_Y0);
        } else {
            renderUnitButtons(game, BUILD_GRID_Y0);
        }
    }

    private void drawMinimapGrid() {
        for (int g = 1; g < 4; g++) {
            int gx = MINIMAP_X + (MINIMAP_SIZE * g) / 4;
            int gy = MINIMAP_Y + (MINIMAP_SIZE * g) / 4;
            screen.drawLine(gx, MINIMAP_Y, gx, MINIMAP_Y + MINIMAP_SIZE - 1, 0xFF181F26);
            screen.drawLine(MINIMAP_X, gy, MINIMAP_X + MINIMAP_SIZE - 1, gy, 0xFF181F26);
        }
    }

    private void renderMinimap(GameMap map, Camera camera, WarTech game) {
        screen.fillRect(MINIMAP_X - 3, MINIMAP_Y - 3, MINIMAP_SIZE + 6, MINIMAP_SIZE + 6, Palette.PANEL_DARK);
        screen.drawLine(MINIMAP_X - 3, MINIMAP_Y - 3, MINIMAP_X + MINIMAP_SIZE + 2, MINIMAP_Y - 3, 0xFF6A543A);
        screen.drawLine(MINIMAP_X - 3, MINIMAP_Y - 3, MINIMAP_X - 3, MINIMAP_Y + MINIMAP_SIZE + 2, 0xFF6A543A);
        screen.drawLine(MINIMAP_X + MINIMAP_SIZE + 2, MINIMAP_Y - 3, MINIMAP_X + MINIMAP_SIZE + 2, MINIMAP_Y + MINIMAP_SIZE + 2, Palette.BLACK_PANEL);
        screen.drawLine(MINIMAP_X - 3, MINIMAP_Y + MINIMAP_SIZE + 2, MINIMAP_X + MINIMAP_SIZE + 2, MINIMAP_Y + MINIMAP_SIZE + 2, Palette.BLACK_PANEL);

        int brCol = Palette.BLUE_LIGHT;
        screen.fillRect(MINIMAP_X - 3, MINIMAP_Y - 3, 6, 2, brCol);
        screen.fillRect(MINIMAP_X - 3, MINIMAP_Y - 3, 2, 6, brCol);
        screen.fillRect(MINIMAP_X + MINIMAP_SIZE - 3, MINIMAP_Y - 3, 6, 2, brCol);
        screen.fillRect(MINIMAP_X + MINIMAP_SIZE + 1, MINIMAP_Y - 3, 2, 6, brCol);
        screen.fillRect(MINIMAP_X - 3, MINIMAP_Y + MINIMAP_SIZE + 1, 6, 2, brCol);
        screen.fillRect(MINIMAP_X - 3, MINIMAP_Y + MINIMAP_SIZE - 3, 2, 6, brCol);
        screen.fillRect(MINIMAP_X + MINIMAP_SIZE - 3, MINIMAP_Y + MINIMAP_SIZE + 1, 6, 2, brCol);
        screen.fillRect(MINIMAP_X + MINIMAP_SIZE + 1, MINIMAP_Y + MINIMAP_SIZE - 3, 2, 6, brCol);

        screen.fillRect(MINIMAP_X, MINIMAP_Y, MINIMAP_SIZE, MINIMAP_SIZE, 0xFF0D0F12);

        double scale = (double) MINIMAP_SIZE / map.width;
        boolean radarOnline = game.hasRadar(Faction.PLAYER);
        if (!radarOnline) {
            drawMinimapGrid();
            BitmapFont.drawText(screen, "NO RADAR LINK", MINIMAP_X + 42, MINIMAP_Y + 108, Palette.BLUE_LIGHT, 2);
            BitmapFont.drawText(screen, "BUILD RADAR OUTPOST", MINIMAP_X + 66, MINIMAP_Y + 132, 0xFF807060, 1);
            return;
        }

        boolean rebuild = minimapCacheW != map.width || minimapCacheH != map.height
            || game.gameTime - minimapCacheAt >= MINIMAP_REFRESH_SECONDS
            || game.gameTime < minimapCacheAt;
        if (rebuild) {
            minimapCacheW = map.width;
            minimapCacheH = map.height;
            minimapCacheAt = game.gameTime;
            for (int i = 0; i < minimapCache.length; i++) minimapCache[i] = 0xFF0D0F12;

            for (int ty = 0; ty < map.height; ty++) {
                int y0 = (int) (ty * scale);
                int y1 = (int) ((ty + 1) * scale);
                int h = Math.max(1, y1 - y0);
                if (y0 >= MINIMAP_SIZE) break;
                if (y0 + h > MINIMAP_SIZE) h = MINIMAP_SIZE - y0;

                for (int tx = 0; tx < map.width; tx++) {
                    int idx = map.index(tx, ty);
                    if (!map.explored[idx]) continue;
                    int col = AssetData.COLOR_SAND_MINIMAP;
                    if (map.terrain[idx] == GameMap.TERRAIN_BEDROCK) col = AssetData.COLOR_ROCK_MINIMAP;
                    else if (map.terrain[idx] == GameMap.TERRAIN_CLIFF) col = 0xFF46688C;
                    else if (map.terrain[idx] == GameMap.TERRAIN_RUIN) col = AssetData.COLOR_RUIN_MINIMAP;
                    else if (map.crystals[idx] > 0 && map.visible[idx]) col = AssetData.COLOR_CRYSTAL_MINIMAP;

                    if (!map.visible[idx]) {
                        int r = ((col) & 0xFF) * 45 / 100;
                        int g = ((col >> 8) & 0xFF) * 45 / 100;
                        int b = ((col >> 16) & 0xFF) * 45 / 100;
                        col = Palette.BLACK | (b << 16) | (g << 8) | r;
                    }

                    int x0 = (int) (tx * scale);
                    int x1 = (int) ((tx + 1) * scale);
                    int w = Math.max(1, x1 - x0);
                    if (x0 >= MINIMAP_SIZE) break;
                    if (x0 + w > MINIMAP_SIZE) w = MINIMAP_SIZE - x0;
                    for (int row = 0; row < h; row++) {
                        int base = (y0 + row) * MINIMAP_SIZE + x0;
                        for (int k = 0; k < w; k++) minimapCache[base + k] = col;
                    }
                }
            }
        }

        int mapBase = screen.base;
        int stride = screen.width;
        for (int row = 0; row < MINIMAP_SIZE; row++) {
            int addr = mapBase + ((MINIMAP_Y + row) * stride + MINIMAP_X) * 4;
            int src = row * MINIMAP_SIZE;
            for (int k = 0; k < MINIMAP_SIZE; k++) {
                Memory.put32(addr, minimapCache[src + k]);
                addr += 4;
            }
        }

        drawMinimapGrid();

        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed()) continue;
            int bidx = map.index(b.tileX, b.tileY);
            if (!map.explored[bidx]) continue;

            int bx0 = MINIMAP_X + (int) (b.tileX * scale);
            int by0 = MINIMAP_Y + (int) (b.tileY * scale);
            int bw = Math.max(4, (int) (b.type.widthTiles * scale));
            int bh = Math.max(4, (int) (b.type.heightTiles * scale));

            int bcol = (b.faction == Faction.PLAYER) ? 0xFFFF9030 : 0xFF2020FF;
            screen.fillRect(bx0, by0, bw, bh, bcol);
            if (bw >= 4 && bh >= 4) {
                screen.fillRect(bx0 + 1, by0 + 1, bw - 2, bh - 2, Palette.WHITE);
            }
        }

        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed()) continue;
            int tx = (int) (u.x / GameMap.TILE_SIZE);
            int ty = (int) (u.y / GameMap.TILE_SIZE);
            if (!map.inBounds(tx, ty)) continue;
            int uidx = map.index(tx, ty);
            if (u.faction != Faction.PLAYER && !map.visible[uidx]) continue;

            int mx = MINIMAP_X + (int) (u.x / GameMap.TILE_SIZE * scale);
            int my = MINIMAP_Y + (int) (u.y / GameMap.TILE_SIZE * scale);
            int dotCol = (u.faction == Faction.PLAYER) ? 0xFFFFB040 : (u.faction == Faction.ENEMY ? Palette.RED : 0xFF30F080);
            screen.fillRect(mx - 1, my - 1, 3, 3, dotCol);
        }

        if (game.selectedBuilding != null && game.selectedBuilding.faction == Faction.PLAYER && game.selectedBuilding.hasRallyPoint) {
            int bmx = MINIMAP_X + (int) (game.selectedBuilding.getCenterX() / GameMap.TILE_SIZE * scale);
            int bmy = MINIMAP_Y + (int) (game.selectedBuilding.getCenterY() / GameMap.TILE_SIZE * scale);
            int rmx = MINIMAP_X + (int) (game.selectedBuilding.rallyX / GameMap.TILE_SIZE * scale);
            int rmy = MINIMAP_Y + (int) (game.selectedBuilding.rallyY / GameMap.TILE_SIZE * scale);

            bmx = Math.clamp(bmx, MINIMAP_X, MINIMAP_X + MINIMAP_SIZE - 1);
            bmy = Math.clamp(bmy, MINIMAP_Y, MINIMAP_Y + MINIMAP_SIZE - 1);
            rmx = Math.clamp(rmx, MINIMAP_X, MINIMAP_X + MINIMAP_SIZE - 1);
            rmy = Math.clamp(rmy, MINIMAP_Y, MINIMAP_Y + MINIMAP_SIZE - 1);

            screen.drawLine(bmx, bmy, rmx, rmy, Palette.GREEN);
            int dotCol = ((int) (game.gameTime * 6.0) % 2 == 0) ? 0xFF00FFFF : Palette.GREEN;
            screen.fillRect(rmx - 1, rmy - 1, 3, 3, dotCol);
        }

        int vbx = MINIMAP_X + (int) (camera.x / GameMap.TILE_SIZE * scale);
        int vby = MINIMAP_Y + (int) (camera.y / GameMap.TILE_SIZE * scale);
        int vbw = (int) (camera.getWorldViewWidth() / GameMap.TILE_SIZE * scale);
        int vbh = (int) (camera.getWorldViewHeight() / GameMap.TILE_SIZE * scale);
        int vbx2 = Math.min(MINIMAP_X + MINIMAP_SIZE - 1, vbx + vbw);
        int vby2 = Math.min(MINIMAP_Y + MINIMAP_SIZE - 1, vby + vbh);
        screen.drawLine(vbx, vby, vbx2, vby, Palette.WHITE);
        screen.drawLine(vbx2, vby, vbx2, vby2, Palette.WHITE);
        screen.drawLine(vbx2, vby2, vbx, vby2, Palette.WHITE);
        screen.drawLine(vbx, vby2, vbx, vby, Palette.WHITE);
    }

    private void renderZoomBar(Camera camera) {
        int zoomY = 253;
        int btnW = 34;
        int btnH = 18;

        int btnMinusX = SIDEBAR_X + 76;
        screen.fillRect(btnMinusX, zoomY, btnW, btnH, Palette.PANEL_DARK);
        screen.drawLine(btnMinusX, zoomY, btnMinusX + btnW - 1, zoomY, Palette.SLATE_DARK);
        screen.drawLine(btnMinusX, zoomY, btnMinusX, zoomY + btnH - 1, Palette.SLATE_DARK);
        screen.drawLine(btnMinusX + btnW - 1, zoomY, btnMinusX + btnW - 1, zoomY + btnH - 1, Palette.BLACK_PANEL);
        screen.drawLine(btnMinusX, zoomY + btnH - 1, btnMinusX + btnW - 1, zoomY + btnH - 1, Palette.BLACK_PANEL);
        BitmapFont.drawText(screen, "-", btnMinusX + 14, zoomY + 5, Palette.BLUE_LIGHT, 1);

        int zoomPct = camera.getZoomPercent();
        int zTextW = 5 * 6 + BitmapFont.intWidth(zoomPct, 1) + 6;
        int zTextX = SIDEBAR_X + (SIDEBAR_WIDTH - zTextW) / 2;
        BitmapFont.drawText(screen, "ZOOM ", zTextX, zoomY + 5, Palette.GRAY_LIGHT, 1);
        int zNumX = zTextX + 5 * 6;
        BitmapFont.drawInt(screen, zoomPct, zNumX, zoomY + 5, Palette.GRAY_LIGHT, 1);
        BitmapFont.drawText(screen, "%", zNumX + BitmapFont.intWidth(zoomPct, 1), zoomY + 5, Palette.GRAY_LIGHT, 1);

        int btnPlusX = SIDEBAR_X + 210;
        screen.fillRect(btnPlusX, zoomY, btnW, btnH, Palette.PANEL_DARK);
        screen.drawLine(btnPlusX, zoomY, btnPlusX + btnW - 1, zoomY, Palette.SLATE_DARK);
        screen.drawLine(btnPlusX, zoomY, btnPlusX, zoomY + btnH - 1, Palette.SLATE_DARK);
        screen.drawLine(btnPlusX + btnW - 1, zoomY, btnPlusX + btnW - 1, zoomY + btnH - 1, Palette.BLACK_PANEL);
        screen.drawLine(btnPlusX, zoomY + btnH - 1, btnPlusX + btnW - 1, zoomY + btnH - 1, Palette.BLACK_PANEL);
        BitmapFont.drawText(screen, "+", btnPlusX + 14, zoomY + 5, Palette.BLUE_LIGHT, 1);
    }

    private void renderBuildButtons(WarTech game, int startY) {
        var btypes = WarTech.BUILD_ORDER;
        for (int i = 0; i < constructingByType.length; i++) constructingByType[i] = null;
        for (int bi = 0; bi < game.buildingCount; bi++) {
            var b = game.buildings[bi];
            if (b.faction == Faction.PLAYER && b.constructing && !b.isDestroyed()
                    && constructingByType[b.type.ordinal()] == null) {
                constructingByType[b.type.ordinal()] = b;
            }
        }
        int slot = 0;
        for (int i = 0; i < btypes.length; i++) {
            var bt = btypes[i];
            if (!game.canProduce(bt)) continue;
            int col = slot % 2;
            int row = slot / 2;
            int bx = SIDEBAR_X + 12 + col * 152;
            int by = startY + row * BUILD_GRID_ROW_H;
            int bw = 144;
            int bh = 34;

            Building constructingB = constructingByType[bt.ordinal()];
            boolean isConstructing = (constructingB != null);
            boolean canAfford = (game.playerCredits > 0 || bt.cost == 0);
            boolean isPlacing = (game.placingBuilding == bt);
            boolean isHovered = (game.mouseX >= bx && game.mouseX < bx + bw && game.mouseY >= by && game.mouseY < by + bh);

            int btnBg = isPlacing ? 0xFF2A3D24 : (isConstructing ? 0xFF1C281C : (canAfford ? (isHovered ? 0xFF453525 : 0xFF2B2218) : 0xFF181410));
            screen.fillRect(bx, by, bw, bh, btnBg);

            if (isConstructing) {
                double prog = constructingB.constructionProgress;
                int fillW = (int) (prog * (bw - 2));
                screen.fillRect(bx + 1, by + bh - 4, fillW, 3, (game.playerCredits < WarTech.BUILD_SPEND_PER_TICK) ? Palette.GOLD_DARK : Palette.GREEN);
                screen.fillRect(bx + 1 + fillW, by + bh - 4, bw - 2 - fillW, 3, 0xFF141814);
            }

            int borderCol = (isPlacing || isConstructing) ? Palette.GREEN_PURE : (isHovered ? Palette.GOLD : (canAfford ? Palette.BROWN_BORDER : 0xFF352820));
            screen.drawLine(bx, by, bx + bw - 1, by, borderCol);
            screen.drawLine(bx, by, bx, by + bh - 1, borderCol);
            screen.drawLine(bx + bw - 1, by, bx + bw - 1, by + bh - 1, isHovered ? borderCol : 0xFF140F0A);
            screen.drawLine(bx, by + bh - 1, bx + bw - 1, by + bh - 1, isHovered ? borderCol : 0xFF140F0A);

            String shortName = bt.shortName;
            int titleCol = (isPlacing || isConstructing) ? Palette.GREEN_PURE : (canAfford ? (isHovered ? Palette.WHITE : Palette.BLUE_LIGHT) : Palette.GRAY_DISABLED);
            BitmapFont.drawText(screen, shortName, bx + 28, by + 2, titleCol, 1);

            screen.fillRect(bx + 5, by + 7, 20, 20, Palette.BLACK_PANEL);
            renderBuildingIcon(screen, bt, bx + 6, by + 8);

            BuildingLabels labels = BuildingLabels.of(bt);
            if (isConstructing) {
                if (game.playerCredits < WarTech.BUILD_SPEND_PER_TICK) {
                    BitmapFont.drawText(screen, "ON HOLD", bx + 28, by + 12, Palette.BLUE_SKY, 1);
                } else {
                    int pct = (int) (constructingB.constructionProgress * 100);
                    BitmapFont.drawLine(screen, null, pct, "%", bx + 28, by + 12, Palette.GREEN_BRIGHT);
                }
            } else {
                int costCol = (game.playerCredits >= bt.cost) ? Palette.GREEN_BRIGHT : (canAfford ? Palette.BLUE_SKY : Palette.RED);
                BitmapFont.drawText(screen, labels.costText, bx + 28, by + 12, costCol, 1);
            }

            // Power & Footprint
            int pCol = (bt.powerNet >= 0) ? Palette.BLUE_SKY : 0xFF3080FF;
            BitmapFont.drawText(screen, labels.buttonMetaText, bx + 28, by + 22, pCol, 1);
            slot++;
        }

        if (slot == 0) {
            BitmapFont.drawText(screen, "CON YARD REQUIRED", SIDEBAR_X + 28, startY + 45, Palette.GRAY_DISABLED, 1);
        }
    }

    private void renderUnitButtons(WarTech game, int startY) {
        var utypes = WarTech.UNIT_ORDER;
        int slot = 0;
        for (int i = 0; i < utypes.length; i++) {
            var ut = utypes[i];
            if (!game.canProduce(ut)) continue;
            int col = slot % 2;
            int row = slot / 2;
            int bx = SIDEBAR_X + 12 + col * 152;
            int by = startY + row * BUILD_GRID_ROW_H;
            int bw = 144;
            int bh = 34;

            boolean isBuilding = (game.unitQueueCount > 0 && game.unitQueue[0] == ut);
            int queuedCount = 0;
            for (int q = 0; q < game.unitQueueCount; q++) {
                if (game.unitQueue[q] == ut) queuedCount++;
            }

            boolean isHovered = (game.mouseX >= bx && game.mouseX < bx + bw && game.mouseY >= by && game.mouseY < by + bh);

            int btnBg = isBuilding ? 0xFF1C281C : (isHovered ? 0xFF453525 : 0xFF2B2218);
            screen.fillRect(bx, by, bw, bh, btnBg);

            if (isBuilding) {
                double prog = (double) game.unitBuildSpent / ut.cost;
                int fillW = (int) (prog * (bw - 2));
                screen.fillRect(bx + 1, by + bh - 4, fillW, 3, (game.playerCredits < WarTech.UNIT_BUILD_SPEND_PER_TICK) ? Palette.GOLD_DARK : Palette.GREEN);
                screen.fillRect(bx + 1 + fillW, by + bh - 4, bw - 2 - fillW, 3, 0xFF141814);
            }

            int borderCol = isBuilding ? Palette.GREEN_PURE : (isHovered ? Palette.GOLD : Palette.BROWN_BORDER);
            screen.drawLine(bx, by, bx + bw - 1, by, borderCol);
            screen.drawLine(bx, by, bx, by + bh - 1, borderCol);
            screen.drawLine(bx + bw - 1, by, bx + bw - 1, by + bh - 1, isHovered ? borderCol : 0xFF140F0A);
            screen.drawLine(bx, by + bh - 1, bx + bw - 1, by + bh - 1, isHovered ? borderCol : 0xFF140F0A);

            String uName = ut.shortName;
            int titleCol = isBuilding ? Palette.GREEN : (isHovered ? Palette.WHITE : Palette.BLUE_LIGHT);
            BitmapFont.drawText(screen, uName, bx + 28, by + 2, titleCol, 1);

            screen.fillRect(bx + 5, by + 7, 20, 20, Palette.BLACK_PANEL);
            renderUnitIcon(screen, ut, bx + 6, by + 8);

            if (isBuilding) {
                if (game.playerCredits < WarTech.UNIT_BUILD_SPEND_PER_TICK) {
                    BitmapFont.drawText(screen, "ON HOLD", bx + 28, by + 12, Palette.BLUE_SKY, 1);
                } else {
                    int pct = (int) ((double) game.unitBuildSpent / ut.cost * 100);
                    BitmapFont.drawLine(screen, null, pct, "%", bx + 28, by + 12, Palette.GREEN_BRIGHT);
                }
            } else {
                BitmapFont.drawText(screen, UnitLabels.of(ut).costText, bx + 28, by + 12, Palette.GREEN_BRIGHT, 1);
            }

            if (queuedCount > 1 || (!isBuilding && queuedCount > 0)) {
                BitmapFont.drawLine(screen, "QUEUED x", queuedCount, null, bx + 28, by + 22, Palette.GOLD_LIGHT);
            } else {
                BitmapFont.drawText(screen, UnitLabels.of(ut).prereqHpText, bx + 28, by + 22, Palette.GOLD, 1);
            }
            slot++;
        }

        if (slot == 0) {
            BitmapFont.drawText(screen, "FACILITY REQUIRED", SIDEBAR_X + 28, startY + 45, Palette.GRAY_DISABLED, 1);
        }
    }
}
