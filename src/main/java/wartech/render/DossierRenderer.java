package wartech.render;

// Tactical intel panel with unit and building dossiers, hover info, and base status

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitType;
import static wartech.render.RenderShared.*;
import static wartech.render.Renderer.*;

public final class DossierRenderer {
    private final Surface screen;

    public DossierRenderer(Surface screen) {
        this.screen = screen;
    }


    private static final int DOSSIER_TITLE_Y = 5;      // scale 2 -> rows  5..18
    private static final int DOSSIER_RULE_Y = 24;      // hairline under the title
    private static final int DOSSIER_PORTRAIT_Y = 30;  // 46x46 -> rows 30..75
    private static final int DOSSIER_NAME_Y = 32;      // scale 2 -> rows 32..45
    private static final int DOSSIER_FACTION_Y = 52;
    private static final int DOSSIER_HP_BAR_Y = 82;
    private static final int DOSSIER_STAT_Y = 114;
    private static final int DOSSIER_ORDER_Y = 152;

    public void renderTacticalIntel(WarTech game, GameMap map) {
        int px = INTEL_X;
        int py = INTEL_Y;
        int pw = INTEL_W;
        int ph = INTEL_H;

        screen.fillRect(px, py, pw, ph, 0xFF0D0F12);
        screen.drawLine(px, py, px + pw, py, 0xFF453525);
        screen.drawLine(px, py, px, py + ph, 0xFF453525);
        screen.drawLine(px + pw, py, px + pw, py + ph, Palette.BLACK_EDGE);
        screen.drawLine(px, py + ph, px + pw, py + ph, Palette.BLACK_EDGE);

        int hoverIdx = -1;
        if (game.mouseX >= BUILD_GRID_X0 && game.mouseX < BUILD_GRID_X1 &&
            game.mouseY >= BUILD_GRID_Y0 && game.mouseY < BUILD_GRID_Y1) {
            int row = (game.mouseY - BUILD_GRID_Y0) / BUILD_GRID_ROW_H;
            int col = (game.mouseX < SIDEBAR_COL_SPLIT_X) ? 0 : 1;
            if (col >= 0 && col < BUILD_GRID_COLS && row >= 0 && row < BUILD_GRID_ROWS) {
                hoverIdx = row * 2 + col;
            }
        }

        if (game.selectedUnit != null && !game.selectedUnit.isDestroyed()) {
            renderUnitDossier(game.selectedUnit, game, px, py, pw, ph);
            return;
        }

        if (game.selectedBuilding != null && !game.selectedBuilding.isDestroyed()) {
            renderBuildingDossier(game.selectedBuilding, game, px, py, pw, ph);
            return;
        }

        if (game.activeTab == 0 && hoverIdx >= 0) {
            var bt = game.getAvailableBuilding(hoverIdx);
            if (bt != null) {
                renderBuildHoverInfo(bt, game, px, py);
                return;
            }
        }
        if (game.activeTab == 1 && hoverIdx >= 0) {
            var ut = game.getAvailableUnit(hoverIdx);
            if (ut != null) {
                renderUnitHoverInfo(ut, game, px, py);
                return;
            }
        }

        if (game.placingBuilding != null) {
            renderPlacingInfo(game.placingBuilding, px, py);
            return;
        }

        renderBaseStatus(game, map, px, py, pw, ph);
    }

    private void drawDossierTitle(String title, int px, int py, int pw) {
        BitmapFont.drawText(screen, title, px + 12, py + DOSSIER_TITLE_Y, Palette.GOLD, 2);
        screen.drawLine(px + 10, py + DOSSIER_RULE_Y, px + pw - 10, py + DOSSIER_RULE_Y, 0xFF243040);
    }

    private void drawDossierPortrait(int px, int py) {
        int x = px + 10;
        int y = py + DOSSIER_PORTRAIT_Y;
        screen.fillRect(x, y, 46, 46, 0xFF181512);
        screen.drawLine(x, y, x + 45, y, Palette.SLATE_DARK);
        screen.drawLine(x, y, x, y + 45, Palette.SLATE_DARK);
        screen.drawLine(x + 45, y, x + 45, y + 45, Palette.BLACK_PANEL);
        screen.drawLine(x, y + 45, x + 45, y + 45, Palette.BLACK_PANEL);
    }

    private void renderUnitDossier(Unit u, WarTech game, int px, int py, int pw, int ph) {
        drawDossierTitle("UNIT DOSSIER", px, py, pw);
        drawDossierPortrait(px, py);
        renderUnitIcon(screen, u.type, px + 24, py + DOSSIER_PORTRAIT_Y + 14);

        BitmapFont.drawText(screen, u.type.displayNameUpper, px + 64, py + DOSSIER_NAME_Y, Palette.WHITE, 2);
        String fName = (u.faction == Faction.PLAYER) ? "[VANGUARD]" : (u.faction == Faction.ENEMY ? "[CRIMSON ENEMY]" : "[NEUTRAL]");
        int fCol = (u.faction == Faction.PLAYER) ? Palette.GOLD : (u.faction == Faction.ENEMY ? Palette.RED : Palette.GREEN_BRIGHT);
        BitmapFont.drawText(screen, fName, px + 64, py + DOSSIER_FACTION_Y, fCol, 1);

        int hpBarY = py + DOSSIER_HP_BAR_Y;
        int barW = pw - 20;
        screen.fillRect(px + 10, hpBarY, barW, 13, Palette.BLACK);
        int hpW = (int) ((double) u.hp / u.maxHp * (barW - 2));
        int hpCol = (u.hp > u.maxHp / 2) ? Palette.GREEN : (u.hp > u.maxHp / 4 ? Palette.BLUE_SKY : Palette.RED);
        if (hpW > 0) {
            screen.fillRect(px + 11, hpBarY + 1, hpW, 11, hpCol);
        }
        int hpPct = (int) ((double) u.hp / u.maxHp * 100);
        BitmapFont.drawText(screen, "HP: ", px + 12, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        int hpX = px + 12 + 4 * 6;
        BitmapFont.drawInt(screen, u.hp, hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        hpX += BitmapFont.intWidth(u.hp, 1);
        BitmapFont.drawText(screen, " / ", hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        hpX += 3 * 6;
        BitmapFont.drawInt(screen, u.maxHp, hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        hpX += BitmapFont.intWidth(u.maxHp, 1);
        BitmapFont.drawText(screen, " (", hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        hpX += 2 * 6;
        BitmapFont.drawInt(screen, hpPct, hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        hpX += BitmapFont.intWidth(hpPct, 1);
        BitmapFont.drawText(screen, "%)", hpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);

        int statY = py + DOSSIER_STAT_Y;
        String armorType = switch (Math.min(u.type.armor, 4)) {
            case 4 -> "HEAVY PLATING";
            case 3 -> "COMPOSITE";
            case 2 -> "REINFORCED";
            case 1 -> "LIGHT";
            default -> "NONE";
        };
        int armorX = px + 12;
        BitmapFont.drawText(screen, "ARMOR: ", armorX, statY, Palette.GRAY, 1);
        armorX += BitmapFont.getTextWidth("ARMOR: ");
        BitmapFont.drawText(screen, armorType, armorX, statY, Palette.GRAY, 1);
        armorX += BitmapFont.getTextWidth(armorType);
        BitmapFont.drawLine(screen, " (", u.type.armor, ")", armorX, statY, Palette.GRAY);
        int dmgX = BitmapFont.drawLine(screen, "DAMAGE: ", u.type.attackDamage, "  RANGE: ",
            px + 12, statY + 12, Palette.GRAY);
        dmgX = BitmapFont.drawLine(screen, null, (int) u.type.attackRange,
            (u.type.splashRadius > 0.0) ? "  SPLASH: " : null, dmgX, statY + 12, Palette.GRAY);
        if (u.type.splashRadius > 0.0) {
            BitmapFont.drawLine(screen, null, (int) u.type.splashRadius, null, dmgX, statY + 12, Palette.GRAY);
        }
        BitmapFont.drawLine(screen, "SPEED:  ", (int) u.type.speed, " px/sec", px + 12, statY + 24, Palette.GRAY);

        // Status and orders
        int orderY = py + DOSSIER_ORDER_Y;
        screen.drawLine(px + 10, orderY - 4, px + pw - 10, orderY - 4, 0xFF352B20);
        switch (u.type) {
            case COLLECTOR -> {
            int cargoPct = (int) ((double) u.crystalLoad / Unit.MAX_LOAD * 100);
            int cX = BitmapFont.drawLine(screen, "CARGO: ", u.crystalLoad, " / ",
                px + 12, orderY, Palette.GREEN_BRIGHT);
            cX = BitmapFont.drawLine(screen, null, Unit.MAX_LOAD, " (", cX, orderY, Palette.GREEN_BRIGHT);
            BitmapFont.drawLine(screen, null, cargoPct, "%)", cX, orderY, Palette.GREEN_BRIGHT);
            screen.fillRect(px + 10, orderY + 12, barW, 7, Palette.BLACK);
            int cW = (int) ((double) u.crystalLoad / Unit.MAX_LOAD * (barW - 2));
            if (cW > 0) screen.fillRect(px + 11, orderY + 13, cW, 5, 0xFF00FF80);

            String hStatus = u.returningToRefinery ? "RETURNING TO BASE" : (u.crystalLoad > 0 && !u.moving ? "MINING CRYSTALS" : (u.moving ? (u.autoCollect ? "HEADING TO CRYSTALS" : "EN ROUTE") : (u.autoCollect ? "SEEKING CRYSTALS" : "IDLE")));
            BitmapFont.drawText(screen, "STATUS: ", px + 12, orderY + 24, Palette.GOLD, 1);
            BitmapFont.drawText(screen, hStatus, px + 12 + BitmapFont.getTextWidth("STATUS: "), orderY + 24, Palette.GOLD, 1);
            }
            case MECHANIC -> {
            String mStatus = u.isRepairing ? "REPAIRING [1 CR/10 HP]" : (u.moving ? "EN ROUTE TO TARGET" : (u.autoRepair ? "ROAMING / SCANNING BASE" : "STANDBY"));
            BitmapFont.drawText(screen, "STATUS: ", px + 12, orderY, Palette.GREEN, 1);
            BitmapFont.drawText(screen, mStatus, px + 12 + BitmapFont.getTextWidth("STATUS: "), orderY, Palette.GREEN, 1);
            String autoStr = (u.autoRepair ? "AUTO-REPAIR: ENABLED (ROAMING)" : "AUTO-REPAIR: OFF");
            BitmapFont.drawText(screen, autoStr, px + 12, orderY + 14, u.autoRepair ? Palette.GOLD : Palette.GRAY, 1);
            }
            case MEDIC -> {
            BitmapFont.drawText(screen, "STATUS: FIELD MEDIC AURA", px + 12, orderY, Palette.GREEN, 1);
            BitmapFont.drawText(screen, "HEAL:   5 HP / 30 TICKS (TROOPS)", px + 12, orderY + 14, Palette.GOLD, 1);
            }
            case null, default -> {
            String ord = "IDLE - STANDBY";
            if (u.attackTargetUnit != null) {
                ord = (u.stance == Stance.HUNT) ? (u.moving ? "HUNTING [CLOSING IN]" : "HUNTING [ENGAGING]") : "ENGAGING HOSTILE";
            } else if (u.attackTargetBuilding != null) {
                ord = (u.stance == Stance.HUNT) ? (u.moving ? "HUNTING [CLOSING IN]" : "HUNTING [ENGAGING]") : "ENGAGING STRUCTURE";
            } else if (u.moving) {
                ord = (u.stance == Stance.HUNT) ? (u.returningToHuntStation ? "HUNTING [RETURNING TO POST]" : "HUNTING [PATROL ROUTE]") : ((u.stance == Stance.GUARD) ? "MOVING TO GUARD POST" : "MOVING TO TARGET");
            } else if (u.stance == Stance.GUARD) {
                ord = "HOLDING GUARD POST";
            } else if (u.stance == Stance.HUNT) {
                ord = "HUNTING [PERIMETER SCAN]";
            }
            BitmapFont.drawText(screen, "ORDERS: ", px + 12, orderY, Palette.GOLD, 1);
            BitmapFont.drawText(screen, ord, px + 12 + BitmapFont.getTextWidth("ORDERS: "), orderY, Palette.GOLD, 1);

            String modeName = "NORMAL";
            int modeCol = Palette.GRAY;
            switch (u.stance) {
                case GUARD -> {
                    modeName = "GUARD [HOLD GROUND]";
                    modeCol = Palette.GOLD;
                }
                case HUNT -> {
                    modeName = "HUNT [SEEK & DESTROY]";
                    modeCol = Palette.GREEN;
                }
                case DEFAULT -> { }
            }
            BitmapFont.drawText(screen, "MODE:   ", px + 12, orderY + 14, modeCol, 1);
            BitmapFont.drawText(screen, modeName, px + 12 + BitmapFont.getTextWidth("MODE:   "), orderY + 14, modeCol, 1);
            }
        }

        if (u.faction == Faction.PLAYER) {
            int btnY = py + 212; // 710
            screen.drawLine(px + 10, btnY - 5, px + pw - 10, btnY - 5, 0xFF352B20);

            boolean hStop = (game.mouseX >= INSPECTOR_BTN1_X0 && game.mouseX < INSPECTOR_BTN1_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
            boolean isStopped = (u.stance == Stance.DEFAULT && !u.moving && u.attackTargetUnit == null && u.attackTargetBuilding == null && u.repairTargetUnit == null && u.repairTargetBuilding == null);
            screen.fillRect(720, btnY, 90, 44, isStopped ? 0xFF3D2A18 : (hStop ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON));
            int stopBorder = isStopped ? Palette.BLUE_PALE : (hStop ? Palette.GOLD : Palette.BROWN_BORDER);
            screen.drawLine(720, btnY, 809, btnY, stopBorder);
            screen.drawLine(720, btnY, 720, btnY + 43, stopBorder);
            screen.drawLine(809, btnY, 809, btnY + 43, Palette.BLACK_EDGE);
            screen.drawLine(720, btnY + 43, 809, btnY + 43, Palette.BLACK_EDGE);
            if (isStopped) {
                screen.fillRect(722, btnY + 41, 86, 2, Palette.BLUE_PALE);
            }
            BitmapFont.drawText(screen, "STOP", 742, btnY + 14, isStopped ? Palette.WHITE : (hStop ? Palette.WHITE : Palette.BLUE_PALE), 2);

            boolean hGrd = (game.mouseX >= INSPECTOR_BTN2_X0 && game.mouseX < INSPECTOR_BTN2_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
            boolean isGuarding = (u.stance == Stance.GUARD);
            screen.fillRect(816, btnY, 92, 44, isGuarding ? 0xFF183248 : (hGrd ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON));
            int grdBorder = isGuarding ? Palette.GOLD : (hGrd ? Palette.GOLD : Palette.BROWN_BORDER);
            screen.drawLine(816, btnY, 907, btnY, grdBorder);
            screen.drawLine(816, btnY, 816, btnY + 43, grdBorder);
            screen.drawLine(907, btnY, 907, btnY + 43, Palette.BLACK_EDGE);
            screen.drawLine(816, btnY + 43, 907, btnY + 43, Palette.BLACK_EDGE);
            if (isGuarding) {
                screen.fillRect(818, btnY + 41, 88, 2, Palette.GOLD);
            }
            BitmapFont.drawText(screen, "GUARD", 830, btnY + 14, isGuarding ? 0xFF00FFFF : (hGrd ? Palette.WHITE : Palette.BLUE_PALE), 2);

            boolean isCollector = (u.type == UnitType.COLLECTOR);
            boolean isMechanic = (u.type == UnitType.MECHANIC);
            String a3Name = isCollector ? (u.crystalLoad > 0 ? "UNLOAD" : "COLLECT") : (isMechanic ? "REPAIR" : "HUNT");
            boolean hA3 = (game.mouseX >= INSPECTOR_BTN3_X0 && game.mouseX < INSPECTOR_BTN3_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
            boolean isHunting = (!isCollector && !isMechanic && u.stance == Stance.HUNT);
            boolean isCollecting = (isCollector && u.autoCollect);
            boolean isRepairMode = (isMechanic && u.autoRepair);
            boolean isA3Active = isHunting || isCollecting || isRepairMode;

            screen.fillRect(914, btnY, 92, 44, isA3Active ? 0xFF143820 : (hA3 ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON));
            int a3Border = isA3Active ? Palette.GREEN : (hA3 ? Palette.GOLD : Palette.BROWN_BORDER);
            screen.drawLine(914, btnY, 1005, btnY, a3Border);
            screen.drawLine(914, btnY, 914, btnY + 43, a3Border);
            screen.drawLine(1005, btnY, 1005, btnY + 43, Palette.BLACK_EDGE);
            screen.drawLine(914, btnY + 43, 1005, btnY + 43, Palette.BLACK_EDGE);
            if (isA3Active) {
                screen.fillRect(916, btnY + 41, 88, 2, Palette.GREEN);
            }
            int a3X = 936;
            if (isCollector) {
                a3X = (u.crystalLoad > 0) ? 926 : 922;
            } else if (isMechanic) {
                a3X = 926;
            }
            BitmapFont.drawText(screen, a3Name, a3X, btnY + 14, isA3Active ? 0xFF80FFB0 : (hA3 ? Palette.WHITE : Palette.GREEN), 2);
        }
    }


    private void renderBuildingDossier(Building b, WarTech game, int px, int py, int pw, int ph) {
        drawDossierTitle("STRUCTURE DOSSIER", px, py, pw);
        drawDossierPortrait(px, py);
        renderBuildingIcon(screen, b.type, px + 24, py + DOSSIER_PORTRAIT_Y + 14);

        BitmapFont.drawText(screen, b.type.displayNameUpper, px + 64, py + DOSSIER_NAME_Y, Palette.WHITE, 2);
        int fCol = b.constructing ? Palette.BLUE_SKY : ((b.faction == Faction.PLAYER) ? Palette.GOLD : Palette.RED);
        if (b.constructing) {
            BitmapFont.drawText(screen, "[UNDER CONSTRUCTION ", px + 64, py + DOSSIER_FACTION_Y, fCol, 1);
            BitmapFont.drawLine(screen, null, (int) (b.constructionProgress * 100), "%]",
                px + 64 + BitmapFont.getTextWidth("[UNDER CONSTRUCTION "), py + DOSSIER_FACTION_Y, fCol);
        } else {
            BitmapFont.drawText(screen,
                (b.faction == Faction.PLAYER) ? "[VANGUARD INSTALLATION]" : "[CRIMSON OUTPOST]",
                px + 64, py + DOSSIER_FACTION_Y, fCol, 1);
        }

        int hpBarY = py + DOSSIER_HP_BAR_Y;
        int barW = pw - 20;
        screen.fillRect(px + 10, hpBarY, barW, 13, Palette.BLACK);
        int hpW = (int) ((double) b.hp / b.maxHp * (barW - 2));
        int hpCol = (b.hp > b.maxHp / 2) ? Palette.GREEN : (b.hp > b.maxHp / 4 ? Palette.BLUE_SKY : Palette.RED);
        if (hpW > 0) screen.fillRect(px + 11, hpBarY + 1, hpW, 11, hpCol);

        int bHpPct = (int) ((double) b.hp / b.maxHp * 100);
        BitmapFont.drawText(screen, "HP: ", px + 12, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        int bHpX = px + 12 + 4 * 6;
        BitmapFont.drawInt(screen, b.hp, bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        bHpX += BitmapFont.intWidth(b.hp, 1);
        BitmapFont.drawText(screen, " / ", bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        bHpX += 3 * 6;
        BitmapFont.drawInt(screen, b.maxHp, bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        bHpX += BitmapFont.intWidth(b.maxHp, 1);
        BitmapFont.drawText(screen, " (", bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        bHpX += 2 * 6;
        BitmapFont.drawInt(screen, bHpPct, bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);
        bHpX += BitmapFont.intWidth(bHpPct, 1);
        BitmapFont.drawText(screen, "%)", bHpX, hpBarY + 16, Palette.GRAY_LIGHT, 1);

        // Power and size
        int statY = py + DOSSIER_STAT_Y;
        int pCol = (b.type.powerNet >= 0) ? Palette.BLUE_SKY : 0xFF3080FF;
        BuildingLabels labels = BuildingLabels.of(b.type);
        BitmapFont.drawText(screen, labels.powerOutputText, px + 12, statY, pCol, 1);
        BitmapFont.drawText(screen, labels.footprintText, px + 12, statY + 12, Palette.GRAY, 1);

        // Description
        boolean showBuildCost = b.constructing;
        String d2 = labels.dossierLine2;
        if (b.constructing) {
            d2 = (game != null && game.playerCredits < WarTech.BUILD_SPEND_PER_TICK) ? "HOLD: INSUFFICIENT CREDITS" : "Pacing: 1 credit per tick";
        }
        if (showBuildCost) {
            int bcX = BitmapFont.drawLine(screen, "Under construction: ", b.buildSpent, " / ",
                px + 12, statY + 26, Palette.GRAY);
            BitmapFont.drawLine(screen, null, b.type.cost, " credits", bcX, statY + 26, Palette.GRAY);
        } else {
            BitmapFont.drawText(screen, labels.dossierLine1, px + 12, statY + 26, Palette.GRAY, 1);
        }
        BitmapFont.drawText(screen, d2, px + 12, statY + 38, Palette.GRAY, 1);

        if (b.type.canProduceUnits() && b.faction == Faction.PLAYER) {
            if (b.hasRallyPoint) {
                int rtx = (int) (b.rallyX / GameMap.TILE_SIZE);
                int rty = (int) (b.rallyY / GameMap.TILE_SIZE);
                BitmapFont.drawText(screen, "RALLY POINT: (", px + 12, statY + 52, Palette.GREEN, 1);
                int rallyX = BitmapFont.drawLine(screen, null, rtx, ", ",
                    px + 12 + BitmapFont.getTextWidth("RALLY POINT: ("), statY + 52, Palette.GREEN);
                BitmapFont.drawLine(screen, null, rty, ")", rallyX, statY + 52, Palette.GREEN);
                BitmapFont.drawText(screen, "[RIGHT-CLICK MAP TO UPDATE]", px + 12, statY + 64, 0xFF7090A0, 1);
            } else {
                BitmapFont.drawText(screen, "RALLY POINT: NOT SET (AT EXIT)", px + 12, statY + 52, 0xFF808890, 1);
                BitmapFont.drawText(screen, "[RIGHT-CLICK MAP TO SET RALLY]", px + 12, statY + 64, 0xFF7090A0, 1);
            }
        }

        if (b.faction == Faction.PLAYER) {
            int btnY = py + 212; // 710
            screen.drawLine(px + 10, btnY - 5, px + pw - 10, btnY - 5, 0xFF352B20);

            if (b.type.canProduceUnits()) {
                boolean hRep = (game.mouseX >= INSPECTOR_BTN1_X0 && game.mouseX < INSPECTOR_BTN1_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
                screen.fillRect(720, btnY, 90, 44, hRep ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON);
                screen.drawLine(720, btnY, 809, btnY, hRep ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(720, btnY, 720, btnY + 43, hRep ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(809, btnY, 809, btnY + 43, Palette.BLACK_EDGE);
                screen.drawLine(720, btnY + 43, 809, btnY + 43, Palette.BLACK_EDGE);
                int repCol = (b.hp < b.maxHp) ? (hRep ? Palette.WHITE : Palette.GREEN) : Palette.GRAY_DISABLED;
                BitmapFont.drawText(screen, "REPAIR", 729, btnY + 8, repCol, 2);
                BitmapFont.drawText(screen, "$25", 756, btnY + 28, repCol, 1);

                boolean hRal = (game.mouseX >= INSPECTOR_BTN2_X0 && game.mouseX < INSPECTOR_BTN2_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
                screen.fillRect(816, btnY, 92, 44, b.hasRallyPoint ? 0xFF143820 : (hRal ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON));
                int ralBorder = b.hasRallyPoint ? Palette.GREEN : (hRal ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(816, btnY, 907, btnY, ralBorder);
                screen.drawLine(816, btnY, 816, btnY + 43, ralBorder);
                screen.drawLine(907, btnY, 907, btnY + 43, Palette.BLACK_EDGE);
                screen.drawLine(816, btnY + 43, 907, btnY + 43, Palette.BLACK_EDGE);
                if (b.hasRallyPoint) {
                    screen.fillRect(818, btnY + 41, 88, 2, Palette.GREEN);
                }
                String ralName = b.hasRallyPoint ? "CLEAR" : "RALLY";
                int ralCol = b.hasRallyPoint ? 0xFF80FFB0 : (hRal ? Palette.WHITE : Palette.GOLD);
                BitmapFont.drawText(screen, ralName, 832, btnY + 14, ralCol, 2);

                int refund = b.type.cost / 2;
                boolean hSell = (game.mouseX >= INSPECTOR_BTN3_X0 && game.mouseX < INSPECTOR_BTN3_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
                screen.fillRect(914, btnY, 92, 44, hSell ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON);
                screen.drawLine(914, btnY, 1005, btnY, hSell ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(914, btnY, 914, btnY + 43, hSell ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(1005, btnY, 1005, btnY + 43, Palette.BLACK_EDGE);
                screen.drawLine(914, btnY + 43, 1005, btnY + 43, Palette.BLACK_EDGE);
                BitmapFont.drawText(screen, "SELL", 936, btnY + 8, hSell ? Palette.WHITE : Palette.BLUE_SKY, 2);
                int refW = BitmapFont.getTextWidth("+$") + BitmapFont.intWidth(refund, 1);
                BitmapFont.drawLine(screen, "+$", refund, null, 914 + (92 - refW) / 2, btnY + 28, Palette.BLUE_SKY);
            } else {
                boolean hRep = (game.mouseX >= INSPECTOR_BTN1_X0 && game.mouseX < INSPECTOR_WIDE1_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
                screen.fillRect(720, btnY, 142, 44, hRep ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON);
                screen.drawLine(720, btnY, 861, btnY, hRep ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(720, btnY, 720, btnY + 43, hRep ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(861, btnY, 861, btnY + 43, Palette.BLACK_EDGE);
                screen.drawLine(720, btnY + 43, 861, btnY + 43, Palette.BLACK_EDGE);
                int repCol = (b.hp < b.maxHp) ? (hRep ? Palette.WHITE : Palette.GREEN) : Palette.GRAY_DISABLED;
                BitmapFont.drawText(screen, "REPAIR $25", 736, btnY + 14, repCol, 2);

                int refund = b.type.cost / 2;
                boolean hSell = (game.mouseX >= INSPECTOR_WIDE2_X0 && game.mouseX < INSPECTOR_WIDE2_X1 && game.mouseY >= btnY && game.mouseY < btnY + 44);
                screen.fillRect(866, btnY, 142, 44, hSell ? Palette.BROWN_HOVER : Palette.BROWN_BUTTON);
                screen.drawLine(866, btnY, 1007, btnY, hSell ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(866, btnY, 866, btnY + 43, hSell ? Palette.GOLD : Palette.BROWN_BORDER);
                screen.drawLine(1007, btnY, 1007, btnY + 43, Palette.BLACK_EDGE);
                screen.drawLine(866, btnY + 43, 1007, btnY + 43, Palette.BLACK_EDGE);
                BitmapFont.drawText(screen, "SELL +$", 878, btnY + 14, hSell ? Palette.WHITE : Palette.BLUE_SKY, 2);
                BitmapFont.drawInt(screen, refund, 878 + BitmapFont.getTextWidth("SELL +$") * 2, btnY + 14,
                    hSell ? Palette.WHITE : Palette.BLUE_SKY, 2);
            }
        }
    }


    private void renderBuildHoverInfo(BuildingType bt, WarTech game, int px, int py) {
        BitmapFont.drawText(screen, "SPECIFICATIONS", px + 12, py + 6, Palette.GOLD, 2);
        BitmapFont.drawText(screen, bt.displayNameUpper, px + 12, py + 28, Palette.WHITE, 2);
        BuildingLabels labels = BuildingLabels.of(bt);
        BitmapFont.drawText(screen, labels.hoverStatsText, px + 12, py + 48, Palette.BLUE_LIGHT, 1);

        BitmapFont.drawText(screen, labels.hoverLine1, px + 12, py + 66, Palette.GRAY, 1);
        BitmapFont.drawText(screen, labels.hoverLine2, px + 12, py + 78, Palette.GRAY, 1);
        BitmapFont.drawText(screen, labels.hoverLine3, px + 12, py + 90, Palette.GRAY, 1);

        boolean canAfford = (game.playerCredits >= bt.cost);
        String action = canAfford ? "[CLICK BUTTON TO BUILD]" : "[INSUFFICIENT CREDITS]";
        BitmapFont.drawText(screen, action, px + 12, py + 112, canAfford ? Palette.GREEN : Palette.RED, 1);
    }


    private void renderUnitHoverInfo(UnitType ut, WarTech game, int px, int py) {
        BitmapFont.drawText(screen, "SPECIFICATIONS", px + 12, py + 6, Palette.GOLD, 2);
        BitmapFont.drawText(screen, ut.displayNameUpper, px + 12, py + 28, Palette.WHITE, 2);
        UnitLabels labels = UnitLabels.of(ut);
        BitmapFont.drawText(screen, labels.hoverStatsText, px + 12, py + 48, Palette.BLUE_LIGHT, 1);

        BitmapFont.drawText(screen, labels.hoverLine1, px + 12, py + 66, Palette.GRAY, 1);
        BitmapFont.drawText(screen, labels.hoverLine2, px + 12, py + 78, Palette.GRAY, 1);
        BitmapFont.drawText(screen, labels.hoverLine3, px + 12, py + 90, Palette.GOLD, 1);

        boolean canAfford = (game.playerCredits >= ut.cost);
        String action = canAfford ? "[CLICK BUTTON TO TRAIN]" : "[INSUFFICIENT CREDITS]";
        BitmapFont.drawText(screen, action, px + 12, py + 112, canAfford ? Palette.GREEN : Palette.RED, 1);
    }


    private void renderPlacingInfo(BuildingType bt, int px, int py) {
        BitmapFont.drawText(screen, "DEPLOYING STRUCTURE", px + 12, py + 8, Palette.GREEN_PURE, 2);
        BitmapFont.drawText(screen, bt.displayNameUpper, px + 12, py + 30, Palette.WHITE, 2);

        BitmapFont.drawText(screen, "Target: Solid Bedrock", px + 12, py + 52, Palette.GRAY_LIGHT, 1);
        BitmapFont.drawText(screen, "GREEN = Valid in base territory", px + 12, py + 66, Palette.GREEN, 1);
        BitmapFont.drawText(screen, "RED   = Blocked or outside zone", px + 12, py + 80, Palette.RED, 1);
        BitmapFont.drawText(screen, "[LEFT CLICK]  Confirm Placement", px + 12, py + 102, Palette.BLUE_LIGHT, 1);
        BitmapFont.drawText(screen, "[RIGHT CLICK] Cancel Order", px + 12, py + 116, Palette.GRAY, 1);
    }


    private void renderBaseStatus(WarTech game, GameMap map, int px, int py, int pw, int ph) {
        BitmapFont.drawText(screen, "TACTICAL INTEL", px + 12, py + 6, Palette.GOLD, 2);
        BitmapFont.drawText(screen, "VANGUARD COMMAND NET", px + 12, py + 26, Palette.BLUE_LIGHT, 1);
        BitmapFont.drawText(screen, "CREDITS: $", px + 12, py + 40, Palette.GREEN_BRIGHT, 2);
        BitmapFont.drawInt(screen, game.playerCredits, px + 12 + 10 * 12, py + 40, Palette.GREEN_BRIGHT, 2);

        BitmapFont.drawText(screen, "PRIMARY DIRECTIVES:", px + 12, py + 64, Palette.WHITE, 1);
        BitmapFont.drawText(screen, "1. Build Wind Turbine (Power Grid)", px + 12, py + 78, Palette.GRAY, 1);
        BitmapFont.drawText(screen, "2. Build Refinery (Spice Cash)", px + 12, py + 90, Palette.GRAY, 1);
        BitmapFont.drawText(screen, "3. Guard Collector from swarms", px + 12, py + 102, Palette.GRAY, 1);
        BitmapFont.drawText(screen, "4. Train Tanks & Troopers", px + 12, py + 114, Palette.GRAY, 1);
        BitmapFont.drawText(screen, "5. Destroy Crimson Outpost!", px + 12, py + 126, Palette.GRAY, 1);

        boolean radarUp = game.hasRadar(Faction.PLAYER);
        int radarCol = radarUp ? Palette.GREEN_BRIGHT : 0xFF808080;
        BitmapFont.drawText(screen, "RADAR SECTOR: ", px + 12, py + 148, radarCol, 1);
        int radarX = px + 12 + 14 * 6;
        BitmapFont.drawInt(screen, map.width, radarX, py + 148, radarCol, 1);
        radarX += BitmapFont.intWidth(map.width, 1);
        BitmapFont.drawText(screen, "x", radarX, py + 148, radarCol, 1);
        radarX += 6;
        BitmapFont.drawInt(screen, map.height, radarX, py + 148, radarCol, 1);
        radarX += BitmapFont.intWidth(map.height, 1);
        BitmapFont.drawText(screen, radarUp ? " LINK: ONLINE" : " LINK: OFFLINE", radarX, py + 148, radarCol, 1);

        int power = game.playerPowerNet;
        int powerCol = power >= 0 ? Palette.GREEN_BRIGHT : Palette.RED;
        BitmapFont.drawText(screen, "POWER GRID: ", px + 12, py + 162, powerCol, 1);
        int powerX = px + 12 + 12 * 6;
        if (power >= 0) {
            BitmapFont.drawText(screen, "+", powerX, py + 162, powerCol, 1);
            powerX += 6;
        }
        BitmapFont.drawInt(screen, power, powerX, py + 162, powerCol, 1);
        powerX += BitmapFont.intWidth(power, 1);
        BitmapFont.drawText(screen, " MW", powerX, py + 162, powerCol, 1);
        if (power < 0) {
            BitmapFont.drawText(screen, "LOW POWER: PRODUCTION 50%", px + 12, py + 174, Palette.BLUE_SKY, 1);
        }

        BitmapFont.drawText(screen, "SYSTEM STATUS:", px + 12, py + 190, Palette.GRAY_LIGHT, 1);
        BitmapFont.drawText(screen, game.statusToast, px + 12, py + 204, Palette.GOLD, 1);
    }
}
