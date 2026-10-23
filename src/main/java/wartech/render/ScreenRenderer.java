package wartech.render;

import wartech.ai.Difficulty;
import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.campaign.BonusState;
import wartech.campaign.CampaignManager;
import wartech.campaign.CampaignMission;
import wartech.core.Faction;
import wartech.core.GameMode;
import wartech.core.MatchState;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.map.MapSize;
import static wartech.render.RenderShared.*;
import static wartech.render.Renderer.*;

public final class ScreenRenderer {
    private final Surface screen;

    public ScreenRenderer(Surface screen) {
        this.screen = screen;
    }

    private static final String[] THEATER_NAMES = {
        "ACT I: BADLANDS [RECRUIT]", "ACT II: SYNDICATE [VETERAN]", "ACT III: DEEP CRADLE [CMD]"
    };

    private static boolean theaterNamesChecked = false;

    public static int briefingTextHeight(String[] lines, int maxWidth) {
        if (lines == null) return 0;
        int total = 0;
        for (String line : lines) {
            total += BitmapFont.getWrappedLineCountXL(line, maxWidth);
        }
        return total * BRIEFING_TEXT_LEADING;
    }

    public static int briefingReconY(String[] lines) {
        return Math.max(BRIEFING_TEXT_Y + briefingTextHeight(lines, BRIEFING_TEXT_W) + 8,
            BRIEFING_Y + 300);
    }

    public void renderMatchEndOverlay(WarTech game) {
        boolean isVictory = (game.matchState == MatchState.VICTORY);

        ditherFill(screen, 0, 0, VIEW_WIDTH, SCREEN_HEIGHT, 0xFF0A0D10);

        int bw = DEBRIEF_W;
        int bh = DEBRIEF_H;
        int bx = DEBRIEF_X;
        int by = DEBRIEF_Y;

        drawShadowRect(screen, bx + 8, by + 8, bw, bh);
        screen.fillRect(bx, by, bw, bh, 0xFF14181E);

        int primaryCol = isVictory ? Palette.GREEN : 0xFF4040FF;
        int secondaryCol = isVictory ? Palette.BLUE_SKY : 0xFFFF5050;
        int trimCol = isVictory ? 0xFF1C4524 : 0xFF451C1C;

        // Borders
        screen.drawLine(bx, by, bx + bw - 1, by, primaryCol);
        screen.drawLine(bx, by, bx, by + bh - 1, primaryCol);
        screen.drawLine(bx + bw - 1, by, bx + bw - 1, by + bh - 1, primaryCol);
        screen.drawLine(bx, by + bh - 1, bx + bw - 1, by + bh - 1, primaryCol);

        screen.drawLine(bx + 2, by + 2, bx + bw - 3, by + 2, trimCol);
        screen.drawLine(bx + 2, by + 2, bx + 2, by + bh - 3, trimCol);
        screen.drawLine(bx + bw - 3, by + 2, bx + bw - 3, by + bh - 3, trimCol);
        screen.drawLine(bx + 2, by + bh - 3, bx + bw - 3, by + bh - 3, trimCol);

        int bannerH = 68;
        screen.fillRect(bx + 3, by + 3, bw - 6, bannerH, trimCol);
        screen.drawLine(bx + 3, by + bannerH + 3, bx + bw - 4, by + bannerH + 3, primaryCol);

        if (isVictory) {
            String title = (game.campaignManager != null && game.campaignManager.campaignActive) ?
                "** OPERATION ACCOMPLISHED **" : "** MISSION ACCOMPLISHED **";
            int tw = title.length() * 12;
            BitmapFont.drawText(screen, title, bx + (bw - tw) / 2, by + 16, secondaryCol, 2);
            boolean campaignWin = game.campaignManager != null && game.campaignManager.campaignActive;
            String subHead = campaignWin ? game.campaignManager.getCurrentMission().titleUpper : "";
            String subTail = campaignWin ? " // SECTOR SECURED" : "CRIMSON SYNDICATE ERADICATED - SECTOR SECURED";
            int sw = (subHead.length() + subTail.length()) * 6;
            int subX = bx + (bw - sw) / 2;
            if (campaignWin) {
                BitmapFont.drawText(screen, subHead, subX, by + 44, 0xFFE0FFE0, 1);
                subX += BitmapFont.getTextWidth(subHead);
            }
            BitmapFont.drawText(screen, subTail, subX, by + 44, 0xFFE0FFE0, 1);
        } else {
            String title = (game.campaignManager != null && game.campaignManager.campaignActive) ?
                "XX OPERATION FAILED XX" : "XX MISSION FAILED XX";
            int tw = title.length() * 12;
            BitmapFont.drawText(screen, title, bx + (bw - tw) / 2, by + 16, 0xFF4040FF, 2);
            String sub = (game.campaignManager != null && game.campaignManager.campaignActive) ?
                "OBJECTIVES COMPROMISED - SECTOR LOST" :
                "VANGUARD FORCES DESTROYED - SECTOR LOST";
            int sw = sub.length() * 6;
            BitmapFont.drawText(screen, sub, bx + (bw - sw) / 2, by + 44, 0xFFFFD0D0, 1);
        }

        int statY = by + 90;
        BitmapFont.drawText(screen, "TACTICAL COMBAT DEBRIEF", bx + 28, statY, Palette.GOLD, 2);
        screen.drawLine(bx + 28, statY + 22, bx + bw - 28, statY + 22, 0xFF2A3D4D);

        // Operation Time
        int totalSecs = (int) game.gameTime;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        BitmapFont.drawTextLarge(screen, "OPERATION DURATION:", bx + 28, statY + 34, Palette.GRAY);
        {
            int tx0 = bx + 28;
            if (mins < 10) {
                BitmapFont.drawGlyph(screen, '0', tx0, statY + 48, Palette.WHITE, 2);
                tx0 += BitmapFont.intWidth(0, 2);
            }
            BitmapFont.drawInt(screen, mins, tx0, statY + 48, Palette.WHITE, 2);
            tx0 += BitmapFont.intWidth(mins, 2);
            BitmapFont.drawText(screen, ":", tx0, statY + 48, Palette.WHITE, 2);
            tx0 += BitmapFont.getTextWidth(":") * 2;
            if (secs < 10) {
                BitmapFont.drawGlyph(screen, '0', tx0, statY + 48, Palette.WHITE, 2);
                tx0 += BitmapFont.intWidth(0, 2);
            }
            BitmapFont.drawInt(screen, secs, tx0, statY + 48, Palette.WHITE, 2);
        }

        BitmapFont.drawTextLarge(screen, "SPICE REFINED:", bx + 280, statY + 34, Palette.GRAY);
        BitmapFont.drawText(screen, "$", bx + 280, statY + 48, Palette.GREEN_BRIGHT, 2);
        BitmapFont.drawInt(screen, game.totalCreditsHarvested, bx + 280 + BitmapFont.getTextWidth("$") * 2,
            statY + 48, Palette.GREEN_BRIGHT, 2);

        BitmapFont.drawTextLarge(screen, "HOSTILES ELIMINATED:", bx + 28, statY + 80, Palette.GRAY);
        {
            int kx = bx + 28;
            BitmapFont.drawInt(screen, game.unitsKilled, kx, statY + 94, Palette.BLUE_SKY, 2);
            kx += BitmapFont.intWidth(game.unitsKilled, 2);
            BitmapFont.drawText(screen, " UNITS (", kx, statY + 94, Palette.BLUE_SKY, 2);
            kx += BitmapFont.getTextWidth(" UNITS (") * 2;
            BitmapFont.drawInt(screen, game.buildingsDestroyed, kx, statY + 94, Palette.BLUE_SKY, 2);
            kx += BitmapFont.intWidth(game.buildingsDestroyed, 2);
            BitmapFont.drawText(screen, " BASES)", kx, statY + 94, Palette.BLUE_SKY, 2);
        }

        BitmapFont.drawTextLarge(screen, "VANGUARD CASUALTIES:", bx + 280, statY + 80, Palette.GRAY);
        int lossCol = (game.unitsLost == 0) ? Palette.GREEN : 0xFFFF5050;
        {
            int lx = bx + 280;
            BitmapFont.drawInt(screen, game.unitsLost, lx, statY + 94, lossCol, 2);
            lx += BitmapFont.intWidth(game.unitsLost, 2);
            BitmapFont.drawText(screen, " UNITS LOST", lx, statY + 94, lossCol, 2);
        }

        int evalY = statY + 138;
        screen.fillRect(bx + 28, evalY, bw - 56, 28, 0xFF182028);
        screen.drawLine(bx + 28, evalY, bx + bw - 28, evalY, 0xFF2A3D4D);
        screen.drawLine(bx + 28, evalY + 28, bx + bw - 28, evalY + 28, 0xFF2A3D4D);
        String outcome = isVictory ? "OUTCOME: COMPLETE DOMINION & STRATEGIC VICTORY" : "OUTCOME: BASE DESTROYED - CASUALTY EVACUATION NEEDED";
        int outCol = isVictory ? Palette.GREEN : 0xFFFF4040;
        BitmapFont.drawTextLarge(screen, outcome, bx + 36, evalY + 9, outCol);

        if (game.campaignManager != null && game.campaignManager.campaignActive) {
            CampaignMission m = game.campaignManager.getCurrentMission();
            if (m != null && m.secondaryObjectives.length > 0) {
                BonusState bonus = game.campaignManager.bonusStatus;
                String bonusLabel = switch (bonus) {
                    case ACHIEVED -> "BONUS OBJECTIVE: EARNED";
                    case FAILED -> "BONUS OBJECTIVE: MISSED";
                    case PENDING -> "BONUS OBJECTIVE: NOT EARNED";
                };
                int bonusCol = switch (bonus) {
                    case ACHIEVED -> Palette.GREEN_DONE;
                    case FAILED -> Palette.TEXT_FAIL;
                    case PENDING -> 0xFF94A3B8;
                };
                BitmapFont.drawTextLarge(screen, bonusLabel, bx + 36, evalY + 38, bonusCol);
                BitmapFont.drawText(screen, m.secondaryObjectives[0], bx + 36, evalY + 54, 0xFF94A3B8, 1);
            }
        }

        int btn1X = DEBRIEF_BTN1_X;
        int btn1W = DEBRIEF_BTN1_W;
        int btn2X = DEBRIEF_BTN2_X;
        int btn2W = DEBRIEF_BTN2_W;
        int btnH = DEBRIEF_BTN_H;
        int btnY = DEBRIEF_BTN_Y;

        boolean isHovered1 = (game.mouseX >= btn1X && game.mouseX < btn1X + btn1W && game.mouseY >= btnY && game.mouseY < btnY + btnH);
        int btn1Bg = isHovered1 ? (isVictory ? 0xFF2C5535 : 0xFF552C2C) : (isVictory ? 0xFF1C3A22 : 0xFF3A1C1C);
        screen.fillRect(btn1X, btnY, btn1W, btnH, btn1Bg);

        int border1 = isHovered1 ? Palette.WHITE : primaryCol;
        screen.drawLine(btn1X, btnY, btn1X + btn1W - 1, btnY, border1);
        screen.drawLine(btn1X, btnY, btn1X, btnY + btnH - 1, border1);
        screen.drawLine(btn1X + btn1W - 1, btnY, btn1X + btn1W - 1, btnY + btnH - 1, border1);
        screen.drawLine(btn1X, btnY + btnH - 1, btn1X + btn1W - 1, btnY + btnH - 1, border1);

        String btnLabel;
        if (game.campaignManager != null && game.campaignManager.campaignActive) {
            if (isVictory) {
                btnLabel = (game.campaignManager.currentMissionIndex < CampaignManager.missionCount() - 1) ? "NEXT OPERATION" : "MISSION SELECT";
            } else {
                btnLabel = "RETRY OPERATION";
            }
        } else {
            btnLabel = isVictory ? "PLAY AGAIN" : "RETRY MISSION";
        }
        int blw = btnLabel.length() * 12;
        BitmapFont.drawText(screen, btnLabel, btn1X + (btn1W - blw) / 2, btnY + 14, isHovered1 ? Palette.WHITE : primaryCol, 2);

        boolean isHovered2 = (game.mouseX >= btn2X && game.mouseX < btn2X + btn2W && game.mouseY >= btnY && game.mouseY < btnY + btnH);
        int btn2Bg = isHovered2 ? 0xFF4A3820 : Palette.PANEL_DARK;
        screen.fillRect(btn2X, btnY, btn2W, btnH, btn2Bg);

        int border2 = isHovered2 ? Palette.WHITE : Palette.BLUE_PALE;
        screen.drawLine(btn2X, btnY, btn2X + btn2W - 1, btnY, border2);
        screen.drawLine(btn2X, btnY, btn2X, btnY + btnH - 1, border2);
        screen.drawLine(btn2X + btn2W - 1, btnY, btn2X + btn2W - 1, btnY + btnH - 1, border2);
        screen.drawLine(btn2X, btnY + btnH - 1, btn2X + btn2W - 1, btnY + btnH - 1, border2);

        String setupLabel = (game.campaignManager != null && game.campaignManager.campaignActive) ? "MISSION SELECT" : "MISSION SETUP";
        int slw = setupLabel.length() * 12;
        BitmapFont.drawText(screen, setupLabel, btn2X + (btn2W - slw) / 2, btnY + 14, isHovered2 ? Palette.WHITE : Palette.BLUE_PALE, 2);
    }

    public void renderMissionSetupScreen(WarTech game) {
        ditherFill(screen, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0xFF080B0E);

        int x0 = SETUP_X;
        int y0 = SETUP_Y;
        int w = SETUP_W;
        int h = SETUP_H;

        drawShadowRect(screen, x0 + 8, y0 + 8, w, h);
        screen.fillRect(x0, y0, w, h, 0xFF14181E);

        screen.drawLine(x0, y0, x0 + w - 1, y0, Palette.BLUE_PALE);
        screen.drawLine(x0, y0, x0, y0 + h - 1, Palette.BLUE_PALE);
        screen.drawLine(x0 + w - 1, y0, x0 + w - 1, y0 + h - 1, Palette.BLUE_PALE);
        screen.drawLine(x0, y0 + h - 1, x0 + w - 1, y0 + h - 1, Palette.BLUE_PALE);

        screen.drawLine(x0 + 2, y0 + 2, x0 + w - 3, y0 + 2, 0xFF4A3A22);
        screen.drawLine(x0 + 2, y0 + 2, x0 + 2, y0 + h - 3, 0xFF4A3A22);
        screen.drawLine(x0 + w - 3, y0 + 2, x0 + w - 3, y0 + h - 3, 0xFF4A3A22);
        screen.drawLine(x0 + 2, y0 + h - 3, x0 + w - 3, y0 + h - 3, 0xFF4A3A22);

        int headerH = 64;
        screen.fillRect(x0 + 3, y0 + 3, w - 6, headerH, Palette.PANEL_DARK);
        screen.drawLine(x0 + 3, y0 + headerH + 3, x0 + w - 4, y0 + headerH + 3, Palette.BLUE_PALE);

        BitmapFont.drawText(screen, "WARTECH RTS v", x0 + SETUP_COL_X, y0 + 16, Palette.BLUE_PALE, 2);
        BitmapFont.drawText(screen, WarTech.VERSION,
            x0 + SETUP_COL_X + BitmapFont.getTextWidth("WARTECH RTS v") * 2, y0 + 16, Palette.BLUE_PALE, 2);
        BitmapFont.drawText(screen, " - MISSION SETUP",
            x0 + SETUP_COL_X + (BitmapFont.getTextWidth("WARTECH RTS v") + WarTech.VERSION.length() * 6) * 2,
            y0 + 16, Palette.BLUE_PALE, 2);
        BitmapFont.drawTextXL(screen, "SELECT COMBAT DOCTRINE, THEATER PROFILE AND STRATEGIC FUNDS", x0 + SETUP_COL_X, y0 + 44, 0xFF9C8E78);

        int cardY = SETUP_DIFF_Y;
        int sec1Y = cardY - 22;
        BitmapFont.drawTextXL(screen, "1. ENEMY COMBAT DOCTRINE (DIFFICULTY):", x0 + SETUP_COL_X, sec1Y, Palette.GOLD);

        drawDifficultyCard(game, x0 + SETUP_COL_X, cardY, SETUP_CARD_W, SETUP_CARD_H, Difficulty.EASY,
            "RECRUIT", "EASY - 55s WAVES", "LIGHT RAIDS / RELAXED AI", Palette.GREEN_DONE);

        drawDifficultyCard(game, x0 + SETUP_CARD_X2, cardY, SETUP_CARD_W, SETUP_CARD_H, Difficulty.MEDIUM,
            "VETERAN", "MEDIUM - 35s WAVES", "COMBINED ARMS / REGULAR AI", Palette.TAN);

        drawDifficultyCard(game, x0 + SETUP_CARD_X3, cardY, SETUP_CARD_W, SETUP_CARD_H, Difficulty.HARD,
            "COMMANDER", "HARD - 22s WAVES", "HEAVY BLITZ / DUAL REFINERIES", Palette.TEXT_FAIL);

        int fundBtnY = SETUP_FUND_Y;
        int sec2Y = fundBtnY - 22;
        BitmapFont.drawTextXL(screen, "2. STARTING TREASURY FUNDS:", x0 + SETUP_COL_X, sec2Y, Palette.GOLD);

        drawCreditOption(game, x0 + SETUP_COL_X, fundBtnY, SETUP_FUND_W, SETUP_FUND_H, 800, "800 CR [SCARCITY]");
        drawCreditOption(game, x0 + SETUP_FUND_X2, fundBtnY, SETUP_FUND_W, SETUP_FUND_H, 1200, "1200 CR [STANDARD]");
        drawCreditOption(game, x0 + SETUP_FUND_X3, fundBtnY, SETUP_FUND_W, SETUP_FUND_H, 2500, "2500 CR [PROSPEROUS]");
        drawCreditOption(game, x0 + SETUP_FUND_X4, fundBtnY, SETUP_FUND_W, SETUP_FUND_H, 5000, "5000 CR [WAR CHEST]");

        int seedRowY = SETUP_SEED_Y;
        int sec3Y = seedRowY - 20;
        BitmapFont.drawTextXL(screen, "3. PROCEDURAL TERRAIN SEED:", x0 + SETUP_COL_X, sec3Y, Palette.GOLD);

        screen.fillRect(x0 + SETUP_COL_X, seedRowY, SETUP_SEED_BOX_W, SETUP_SEED_H, 0xFF182028);
        screen.drawLine(x0 + SETUP_COL_X, seedRowY, x0 + SETUP_COL_X + SETUP_SEED_BOX_W - 1, seedRowY, 0xFF4A5568);
        screen.drawLine(x0 + SETUP_COL_X, seedRowY + SETUP_SEED_H - 1, x0 + SETUP_COL_X + SETUP_SEED_BOX_W - 1, seedRowY + SETUP_SEED_H - 1, 0xFF4A5568);
        screen.drawLine(x0 + SETUP_COL_X, seedRowY, x0 + SETUP_COL_X, seedRowY + SETUP_SEED_H - 1, 0xFF4A5568);
        screen.drawLine(x0 + SETUP_COL_X + SETUP_SEED_BOX_W - 1, seedRowY, x0 + SETUP_COL_X + SETUP_SEED_BOX_W - 1, seedRowY + SETUP_SEED_H - 1, 0xFF4A5568);
        BitmapFont.drawText(screen, "SEED: ", x0 + 36, seedRowY + 12, Palette.BLUE_PALE, 2);
        BitmapFont.drawInt(screen, (int) game.mapSeed, x0 + 36 + BitmapFont.getTextWidth("SEED: ") * 2,
            seedRowY + 12, Palette.BLUE_PALE, 2);

        drawGenericButton(game, x0 + SETUP_SEED_MINUS_X, seedRowY, SETUP_SEED_STEP_W, SETUP_SEED_H, "-", 2, Palette.BLUE_PALE);
        drawGenericButton(game, x0 + SETUP_SEED_PLUS_X, seedRowY, SETUP_SEED_STEP_W, SETUP_SEED_H, "+", 2, Palette.BLUE_PALE);
        drawGenericButtonLarge(game, x0 + SETUP_SEED_RANDOM_X, seedRowY, SETUP_SEED_RANDOM_W, SETUP_SEED_H, "RANDOMIZE SEED", Palette.TAN);

        BitmapFont.drawTextLarge(screen, "Dunes, rock crags, crystal fields and precursor ruins generated from seed.", x0 + SETUP_COL_X, seedRowY + 41, 0xFF7A6E5E);

        int scaleBtnY = SETUP_SCALE_Y;
        int sec4Y = scaleBtnY - 20;
        BitmapFont.drawTextXL(screen, "4. THEATER SCALE & CHOKEPOINTS:", x0 + SETUP_COL_X, sec4Y, Palette.GOLD);

        drawScaleOption(game, x0 + SETUP_COL_X, scaleBtnY, SETUP_SCALE_W, SETUP_SCALE_H, MapSize.CODE_SMALL_64, "64 x 64 (FAST SKIRMISH)");
        drawScaleOption(game, x0 + SETUP_SCALE_X2, scaleBtnY, SETUP_SCALE_W, SETUP_SCALE_H, MapSize.CODE_LARGE_128, "128 x 128 (EPIC WARZONE)");

        int modeBtnY = SETUP_MODE_Y;
        int modeBtnH = SETUP_MODE_H;
        int sec5Y = modeBtnY - 20;
        BitmapFont.drawTextXL(screen, "5. OPERATION & GAME MODE:", x0 + SETUP_COL_X, sec5Y, Palette.GOLD);

        drawModeButton(game, x0 + SETUP_COL_X, modeBtnY, SETUP_CARD_W, modeBtnH, GameMode.SKIRMISH,
            "PLAY SKIRMISH", "FAST / CUSTOM BATTLE", Palette.GREEN_DONE);

        drawModeButton(game, x0 + SETUP_CARD_X2, modeBtnY, SETUP_CARD_W, modeBtnH, GameMode.START_CAMPAIGN,
            "START CAMPAIGN", "VANGUARD STORY", Palette.BLUE_SOFT);

        drawModeButton(game, x0 + SETUP_CARD_X3, modeBtnY, SETUP_CARD_W, modeBtnH, GameMode.LOAD_CAMPAIGN,
            "LOAD CAMPAIGN", "RESTORE ARCHIVE", Palette.TAN);

        int deployH = SETUP_DEPLOY_H;
        int deployX = x0 + (w - SETUP_DEPLOY_W) / 2;
        int deployY = SETUP_DEPLOY_Y;

        boolean isDepHovered = (game.mouseX >= deployX && game.mouseX < deployX + SETUP_DEPLOY_W &&
                                game.mouseY >= deployY && game.mouseY < deployY + deployH);

        int depBg = isDepHovered ? 0xFF255A35 : 0xFF143822;
        screen.fillRect(deployX, deployY, SETUP_DEPLOY_W, deployH, depBg);

        int depBorder = isDepHovered ? Palette.WHITE : Palette.GREEN;
        screen.drawLine(deployX, deployY, deployX + SETUP_DEPLOY_W - 1, deployY, depBorder);
        screen.drawLine(deployX, deployY, deployX, deployY + deployH - 1, depBorder);
        screen.drawLine(deployX + SETUP_DEPLOY_W - 1, deployY, deployX + SETUP_DEPLOY_W - 1, deployY + deployH - 1, depBorder);
        screen.drawLine(deployX, deployY + deployH - 1, deployX + SETUP_DEPLOY_W - 1, deployY + deployH - 1, depBorder);

        String depLabel = "DEPLOY TO BATTLEFIELD";
        int dlw = depLabel.length() * 12;
        BitmapFont.drawText(screen, depLabel, deployX + (SETUP_DEPLOY_W - dlw) / 2, deployY + 18, isDepHovered ? Palette.WHITE : Palette.GREEN, 2);

        String subDep = "CLICK DEPLOY OR PRESS SPACEBAR / ENTER TO LAUNCH MISSION";
        int sdw = BitmapFont.getTextXLWidth(subDep);
        BitmapFont.drawTextXL(screen, subDep, x0 + (w - sdw) / 2, deployY + 62, Palette.BLUE_PALE);
    }


    private void drawModeButton(WarTech game, int x, int y, int w, int h, GameMode mode,
                                String title, String sub, int accentCol) {
        boolean isSelected = (game.selectedGameMode == mode);
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isSelected ? 0xFF2A2016 : (isHovered ? 0xFF221A12 : 0xFF16120E);
        screen.fillRect(x, y, w, h, bg);

        int border = isSelected ? Palette.BLUE_PALE : (isHovered ? 0xFF88A0B0 : 0xFF3D3224);
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        if (isSelected) {
            screen.drawLine(x + 2, y + 2, x + w - 3, y + 2, Palette.BLUE_PALE);
            screen.drawLine(x + 2, y + 2, x + 2, y + h - 3, Palette.BLUE_PALE);
            screen.drawLine(x + w - 3, y + 2, x + w - 3, y + h - 3, Palette.BLUE_PALE);
            screen.drawLine(x + 2, y + h - 3, x + w - 3, y + h - 3, Palette.BLUE_PALE);
        }

        BitmapFont.drawTextXL(screen, title, x + 10, y + 6, isSelected ? Palette.WHITE : accentCol);
        BitmapFont.drawTextLarge(screen, sub, x + 10, y + 22, isSelected ? Palette.BLUE_PALE : 0xFF8C7E68);
    }


    private void drawDifficultyCard(WarTech game, int x, int y, int w, int h, Difficulty diff,
                                    String title, String sub, String desc, int titleCol) {
        boolean isSelected = (game.selectedDifficulty == diff);
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isSelected ? 0xFF352516 : (isHovered ? 0xFF261D14 : 0xFF181410);
        screen.fillRect(x, y, w, h, bg);

        int border = isSelected ? Palette.BLUE_PALE : (isHovered ? 0xFF88A0B0 : 0xFF3D3224);
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        if (isSelected) {
            screen.drawLine(x + 2, y + 2, x + w - 3, y + 2, Palette.BLUE_PALE);
            screen.drawLine(x + 2, y + 2, x + 2, y + h - 3, Palette.BLUE_PALE);
            screen.drawLine(x + w - 3, y + 2, x + w - 3, y + h - 3, Palette.BLUE_PALE);
            screen.drawLine(x + 2, y + h - 3, x + w - 3, y + h - 3, Palette.BLUE_PALE);
        }

        BitmapFont.drawText(screen, title, x + 14, y + 12, isSelected ? Palette.WHITE : titleCol, 2);
        BitmapFont.drawTextXL(screen, sub, x + 14, y + 36, isSelected ? Palette.BLUE_PALE : 0xFF9C8E78);
        BitmapFont.drawText(screen, desc, x + 14, y + 56, isSelected ? Palette.WHITE : 0xFF7A6E5E, 1);

        if (isSelected) {
            screen.fillRect(x + w - 62, y + 10, 52, 18, 0xFF143822);
            screen.drawLine(x + w - 62, y + 10, x + w - 10, y + 10, Palette.GREEN);
            screen.drawLine(x + w - 62, y + 28, x + w - 10, y + 28, Palette.GREEN);
            screen.drawLine(x + w - 62, y + 10, x + w - 62, y + 28, Palette.GREEN);
            screen.drawLine(x + w - 10, y + 10, x + w - 10, y + 28, Palette.GREEN);
            BitmapFont.drawTextLarge(screen, "ACTIVE", x + w - 58, y + 15, Palette.GREEN);
        }
    }


    private void drawCreditOption(WarTech game, int x, int y, int w, int h, int amount, String label) {
        boolean isSelected = (game.startingCredits == amount);
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isSelected ? 0xFF352516 : (isHovered ? Palette.PANEL_DARK : 0xFF18130E);
        screen.fillRect(x, y, w, h, bg);

        int border = isSelected ? Palette.BLUE_PALE : (isHovered ? 0xFF88A0B0 : 0xFF3D3224);
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        int txtCol = isSelected ? Palette.BLUE_SKY : (isHovered ? Palette.WHITE : 0xFF9C8E78);
        int tw = BitmapFont.getTextLargeWidth(label);
        BitmapFont.drawTextLarge(screen, label, x + (w - tw) / 2, y + 14, txtCol);
    }


    private void drawScaleOption(WarTech game, int x, int y, int w, int h, int code, String label) {
        boolean isSelected = (game.selectedMapSize == code);
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isSelected ? 0xFF352516 : (isHovered ? Palette.PANEL_DARK : 0xFF18130E);
        screen.fillRect(x, y, w, h, bg);

        int border = isSelected ? Palette.BLUE_PALE : (isHovered ? 0xFF88A0B0 : 0xFF3D3224);
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        int txtCol = isSelected ? Palette.GREEN : (isHovered ? Palette.WHITE : 0xFF9C8E78);
        int tw = BitmapFont.getTextXLWidth(label);
        BitmapFont.drawTextXL(screen, label, x + (w - tw) / 2, y + 14, txtCol);
    }


    private void drawGenericButtonLarge(WarTech game, int x, int y, int w, int h, String label, int col) {
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isHovered ? 0xFF362A1C : 0xFF201810;
        screen.fillRect(x, y, w, h, bg);

        int border = isHovered ? Palette.WHITE : 0xFF504030;
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        int tw = BitmapFont.getTextXLWidth(label);
        BitmapFont.drawTextXL(screen, label, x + (w - tw) / 2, y + 14, isHovered ? Palette.WHITE : col);
    }


    private void drawGenericButton(WarTech game, int x, int y, int w, int h, String label, int fontScale, int col) {
        boolean isHovered = (game.mouseX >= x && game.mouseX < x + w && game.mouseY >= y && game.mouseY < y + h);

        int bg = isHovered ? 0xFF362A1C : 0xFF201810;
        screen.fillRect(x, y, w, h, bg);

        int border = isHovered ? Palette.WHITE : 0xFF504030;
        screen.drawLine(x, y, x + w - 1, y, border);
        screen.drawLine(x, y, x, y + h - 1, border);
        screen.drawLine(x + w - 1, y, x + w - 1, y + h - 1, border);
        screen.drawLine(x, y + h - 1, x + w - 1, y + h - 1, border);

        int charW = fontScale == 2 ? 12 : 6;
        int tw = label.length() * charW;
        int yOff = fontScale == 2 ? 12 : 14;
        BitmapFont.drawText(screen, label, x + (w - tw) / 2, y + yOff, isHovered ? Palette.WHITE : col, fontScale);
    }


    public void renderCampaignObjectiveTracker(WarTech game) {
        CampaignMission m = game.campaignManager.getCurrentMission();
        if (m == null) return;

        int boxX = OBJ_X;
        int boxY = OBJ_Y;

        if (!game.showObjectives) {
            int badgeW = OBJ_BADGE_W;
            int badgeH = OBJ_BADGE_H;
            boolean isHover = (game.mouseX >= boxX && game.mouseX < boxX + badgeW && game.mouseY >= boxY && game.mouseY < boxY + badgeH);
            screen.fillRect(boxX, boxY, badgeW, badgeH, isHover ? 0xFF162538 : 0xFF0D141F);
            int border = isHover ? Palette.TAN : Palette.ORANGE_DEEP;
            screen.drawLine(boxX, boxY, boxX + badgeW - 1, boxY, border);
            screen.drawLine(boxX, boxY, boxX, boxY + badgeH - 1, border);
            screen.drawLine(boxX + badgeW - 1, boxY, boxX + badgeW - 1, boxY + badgeH - 1, border);
            screen.drawLine(boxX, boxY + badgeH - 1, boxX + badgeW - 1, boxY + badgeH - 1, border);

            BitmapFont.drawTextLarge(screen, "[+] DIRECTIVES [O]", boxX + 10, boxY + 8, isHover ? Palette.WHITE : Palette.TAN);
            return;
        }

        int boxW = OBJ_W;
        int boxH = OBJ_H;

        screen.fillRect(boxX, boxY, boxW, boxH, 0xFF0D141F);

        int borderCol = Palette.ORANGE_DEEP;
        screen.drawLine(boxX, boxY, boxX + boxW - 1, boxY, borderCol);
        screen.drawLine(boxX, boxY, boxX, boxY + boxH - 1, borderCol);
        screen.drawLine(boxX + boxW - 1, boxY, boxX + boxW - 1, boxY + boxH - 1, borderCol);
        screen.drawLine(boxX, boxY + boxH - 1, boxX + boxW - 1, boxY + boxH - 1, borderCol);

        BitmapFont.drawTextLarge(screen, "DIRECTIVES: ", boxX + 8, boxY + 8, Palette.TAN);
        BitmapFont.drawTextLarge(screen, m.codename, boxX + 8 + BitmapFont.getTextLargeWidth("DIRECTIVES: "),
            boxY + 8, Palette.TAN);

        int btnMinX = OBJ_MIN_X;
        int btnMinY = OBJ_MIN_Y;
        int btnMinW = OBJ_MIN_W;
        int btnMinH = OBJ_MIN_H;
        boolean isMinHover = (game.mouseX >= btnMinX && game.mouseX < btnMinX + btnMinW && game.mouseY >= btnMinY && game.mouseY < btnMinY + btnMinH);
        screen.fillRect(btnMinX, btnMinY, btnMinW, btnMinH, isMinHover ? Palette.BROWN_DARK : 0xFF101722);
        int minBorder = isMinHover ? Palette.WHITE : Palette.ORANGE_DEEP;
        screen.drawLine(btnMinX, btnMinY, btnMinX + btnMinW - 1, btnMinY, minBorder);
        screen.drawLine(btnMinX, btnMinY, btnMinX, btnMinY + btnMinH - 1, minBorder);
        screen.drawLine(btnMinX + btnMinW - 1, btnMinY, btnMinX + btnMinW - 1, btnMinY + btnMinH - 1, minBorder);
        screen.drawLine(btnMinX, btnMinY + btnMinH - 1, btnMinX + btnMinW - 1, btnMinY + btnMinH - 1, minBorder);
        BitmapFont.drawTextLarge(screen, "-", btnMinX + 8, btnMinY + 4, isMinHover ? Palette.WHITE : Palette.TAN);

        screen.drawLine(boxX + 8, boxY + OBJ_HEADER_H, boxX + boxW - 8, boxY + OBJ_HEADER_H, Palette.BROWN_DARK);

        final int NV = BitmapFont.NO_VALUE;
        CampaignManager cm = game.campaignManager;

        String p1Pre;
        int p1Val = NV;
        String p1Suf = null;
        switch (m.id) {
            case CampaignMission.MISSION_DESERT_DAWN -> {
            p1Pre = "HARVEST CR: "; p1Val = game.totalCreditsHarvested; p1Suf = m.targetCreditsSuffix;
            }
            case CampaignMission.MISSION_IRON_BULWARK -> {
            p1Pre = !cm.primary1Done ? "DEFEND: CON YARD (STATUS: LOST)"
                : (game.matchState == MatchState.VICTORY
                    ? "DEFEND: CON YARD (STATUS: SECURE)" : "DEFEND: CON YARD (STATUS: HOLD)");
            }
            case CampaignMission.MISSION_VIPERS_NEST -> {
            p1Pre = "PURGE HIVE: "; p1Val = cm.skitterersKilled; p1Suf = m.targetKillsSuffix;
            }
            case CampaignMission.MISSION_FORWARD_REDOUBT -> {
            p1Pre = "DEFEND GORGE: "; p1Val = cm.syndicateUnitsKilled; p1Suf = m.targetKillsSuffix;
            }
            case CampaignMission.MISSION_ANCIENT_ECHOES -> {
            p1Pre = "WALKERS DESTROYED: "; p1Val = cm.walkersKilled; p1Suf = m.targetKillsSuffix;
            }
            case CampaignMission.MISSION_SCAVENGER_RUN -> {
            p1Pre = "DRONES DESTROYED: "; p1Val = cm.dronesKilled; p1Suf = m.targetKillsSuffix;
            }
            case CampaignMission.MISSION_CRIMSON_FORTRESS -> {
            p1Pre = "DESTROY WAR FACTORY & CON YARD";
            }
            case CampaignMission.MISSION_CONVOY_EXTRACTION -> {
            p1Pre = "HARVEST CR: "; p1Val = game.totalCreditsHarvested; p1Suf = m.targetCreditsSuffix;
            }
            case CampaignMission.MISSION_DREAD_HARVEST -> {
            p1Pre = "DESTROY REFINERIES ("; p1Suf = " LEFT)";
            p1Val = countEnemyBuildings(game, BuildingType.REFINERY);
            }
            case CampaignMission.MISSION_TITAN_AWAKENING -> {
            p1Pre = "TITANS PURGED: "; p1Val = cm.walkersKilled; p1Suf = m.targetKillsSuffix;
            }
            default -> {
            p1Pre = "DESTROY CITADEL STRUCTURES";
            }
        }
        boolean p1Done = cm.primary1Done && (!cm.primary1IsSurvival || game.matchState == MatchState.VICTORY);
        drawObjectiveLine(p1Done ? "[X] " : "[ ] ", p1Pre, p1Val, p1Suf, boxX + 8, boxY + 30, p1Done ? Palette.GREEN_DONE : Palette.BLUE_SOFT);

        String p2Pre;
        int p2Val = NV;
        String p2Suf = null;
        switch (m.id) {
            case CampaignMission.MISSION_DESERT_DAWN -> {
            p2Pre = "SKITTERERS ELIMINATED: "; p2Val = cm.skitterersKilled; p2Suf = m.targetKillsSuffix;
            }
            case CampaignMission.MISSION_IRON_BULWARK -> {
            p2Pre = "DESTROY OUTPOST ("; p2Suf = " LEFT)"; p2Val = countEnemyBuildings(game, null);
            }
            case CampaignMission.MISSION_VIPERS_NEST -> {
            p2Pre = "HARVEST CR: "; p2Val = game.totalCreditsHarvested; p2Suf = m.targetCreditsSuffix;
            }
            case CampaignMission.MISSION_FORWARD_REDOUBT -> {
            int pTurrets = 0;
            for (int i = 0; i < game.buildingCount; i++) {
                Building bl = game.buildings[i];
                if (bl != null && !bl.isDestroyed() && !bl.constructing
                        && bl.faction == Faction.PLAYER && bl.type == BuildingType.TURRET) pTurrets++;
            }
            p2Pre = "GUN TURRETS: "; p2Val = pTurrets; p2Suf = m.targetBonusCountSuffix;
            }
            case CampaignMission.MISSION_ANCIENT_ECHOES -> {
            p2Pre = "HARVEST ANCIENT CR: "; p2Val = game.totalCreditsHarvested; p2Suf = m.targetCreditsSuffix;
            }
            case CampaignMission.MISSION_SCAVENGER_RUN -> {
            p2Pre = "CLEAR EXPEDITION ("; p2Suf = " LEFT)"; p2Val = countEnemyBuildings(game, null);
            }
            case CampaignMission.MISSION_CRIMSON_FORTRESS -> {
            p2Pre = "DESTROY ENEMY TURRETS ("; p2Suf = " LEFT)";
            p2Val = countEnemyBuildings(game, BuildingType.TURRET);
            }
            case CampaignMission.MISSION_CONVOY_EXTRACTION -> {
            p2Pre = "DESTROY STAGING BASE ("; p2Suf = " LEFT)"; p2Val = countEnemyBuildings(game, null);
            }
            case CampaignMission.MISSION_DREAD_HARVEST -> {
            p2Pre = "DESTROY WAR FACTORY & CON YARD";
            }
            case CampaignMission.MISSION_TITAN_AWAKENING -> {
            p2Pre = "DESTROY ENEMY CON YARD";
            }
            default -> {
            p2Pre = "PURGE WALKERS: "; p2Val = cm.walkersKilled; p2Suf = m.targetKillsSuffix;
            }
        }
        boolean p2Done = cm.primary2Done;
        drawObjectiveLine(p2Done ? "[X] " : "[ ] ", p2Pre, p2Val, p2Suf, boxX + 8, boxY + 52, p2Done ? Palette.GREEN_DONE : Palette.BLUE_SOFT);

        BonusState bonus = cm.bonusStatus;
        String bonusMark = switch (bonus) {
            case ACHIEVED -> "[*] ";
            case FAILED -> "[X] ";
            case PENDING -> "[ ] ";
        };
        int bonusCol = switch (bonus) {
            case ACHIEVED -> Palette.TAN;
            case FAILED -> Palette.TEXT_FAIL;
            case PENDING -> 0xFF94A3B8;
        };
        String bonusText = (m.secondaryObjectives != null && m.secondaryObjectives.length > 0)
            ? m.secondaryObjectives[0] : "NO BONUS OBJECTIVE";
        drawObjectiveLine(bonusMark, bonusText, NV, null, boxX + 8, boxY + 74, bonusCol);

        BitmapFont.drawText(screen, "PRESS 'O' OR CLICK [-] TO HIDE", boxX + 8, boxY + 92, 0xFF64748B, 1);
    }

    private static int countEnemyBuildings(WarTech game, BuildingType type) {
        int n = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY && (type == null || b.type == type)) n++;
        }
        return n;
    }

    private void drawObjectiveLine(String mark, String prefix, int value, String suffix, int x, int y, int color) {
        BitmapFont.drawLargeLine(screen, mark, BitmapFont.NO_VALUE, null, x, y, color);
        int markW = mark.length() * (BitmapFont.CHAR_WIDTH_LG + BitmapFont.SPACING_LG);
        BitmapFont.drawLargeLine(screen, prefix, value, suffix, x + markW, y, color);
    }

    public void renderCampaignBriefing(WarTech game) {
        CampaignMission m = game.campaignManager.getCurrentMission();
        if (m == null) return;

        ditherFill(screen, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0xFF080D14);

        int bx = BRIEFING_X;
        int by = BRIEFING_Y;
        int bw = BRIEFING_W;
        int bh = BRIEFING_H;

        drawShadowRect(screen, bx + 8, by + 8, bw, bh);
        screen.fillRect(bx, by, bw, bh, 0xFF101722);

        screen.drawLine(bx, by, bx + bw - 1, by, Palette.BLUE_PALE);
        screen.drawLine(bx, by, bx, by + bh - 1, Palette.BLUE_PALE);
        screen.drawLine(bx + bw - 1, by, bx + bw - 1, by + bh - 1, Palette.BLUE_PALE);
        screen.drawLine(bx, by + bh - 1, bx + bw - 1, by + bh - 1, Palette.BLUE_PALE);

        screen.drawLine(bx + 2, by + 2, bx + bw - 3, by + 2, Palette.BROWN_DARK);
        screen.drawLine(bx + 2, by + 2, bx + 2, by + bh - 3, Palette.BROWN_DARK);
        screen.drawLine(bx + bw - 3, by + 2, bx + bw - 3, by + bh - 3, Palette.BROWN_DARK);
        screen.drawLine(bx + 2, by + bh - 3, bx + bw - 3, by + bh - 3, Palette.BROWN_DARK);

        screen.fillRect(bx + 3, by + 3, bw - 6, 40, 0xFF182232);
        screen.drawLine(bx + 3, by + 43, bx + bw - 4, by + 43, Palette.BLUE_PALE);
        BitmapFont.drawTextWithShadow(screen, "VANGUARD COMMAND // TACTICAL BRIEFING CONSOLE", bx + 20, by + 16, Palette.BLUE_PALE, 2);

        int theaterW = 256;
        int theaterH = BRIEFING_THEATER_H;
        int theaterY = BRIEFING_THEATER_Y;
        int theaterCount = CampaignManager.THEATER_COUNT;
        if (!theaterNamesChecked) {
            theaterNamesChecked = true;
            if (THEATER_NAMES.length < theaterCount) {
                throw new IllegalStateException("THEATER_NAMES is shorter than the baked theater count");
            }
        }
        for (int t = 0; t < theaterCount; t++) {
            int tx = bx + 24 + t * 264;
            boolean isCurTheater = (t == game.campaignManager.currentTheater);
            boolean isUnlocked = game.campaignManager.isTheaterUnlocked(t);
            boolean isHover = (game.mouseX >= tx && game.mouseX < tx + theaterW && game.mouseY >= theaterY && game.mouseY < theaterY + theaterH);

            int tabBg = isCurTheater ? 0xFF2A2416 : (isUnlocked ? (isHover ? 0xFF223548 : 0xFF141F2D) : 0xFF0D141E);
            int tabBorder = isCurTheater ? Palette.BLUE_PALE : (isUnlocked ? (isHover ? Palette.WHITE : Palette.TAN) : 0xFF334155);
            screen.fillRect(tx, theaterY, theaterW, theaterH, tabBg);

            screen.drawLine(tx, theaterY, tx + theaterW - 1, theaterY, tabBorder);
            screen.drawLine(tx, theaterY, tx, theaterY + theaterH - 1, tabBorder);
            screen.drawLine(tx + theaterW - 1, theaterY, tx + theaterW - 1, theaterY + theaterH - 1, tabBorder);
            screen.drawLine(tx, theaterY + theaterH - 1, tx + theaterW - 1, theaterY + theaterH - 1, tabBorder);

            int textCol;
            int labelW;
            if (isCurTheater) {
                textCol = Palette.BLUE_SOFT;
                labelW = BitmapFont.getTextXLWidth(THEATER_NAMES[t]) + BitmapFont.getTextXLWidth(" *");
            } else if (isUnlocked) {
                textCol = Palette.TAN;
                labelW = BitmapFont.getTextXLWidth(THEATER_NAMES[t]);
            } else {
                textCol = 0xFF475569;
                labelW = BitmapFont.getTextXLWidth("ACT 0") + BitmapFont.xlIntWidth(t + 1)
                    + BitmapFont.getTextXLWidth(" [LOCKED]");
            }
            int labelX = tx + (theaterW - labelW) / 2;
            if (isUnlocked || isCurTheater) {
                BitmapFont.drawTextXL(screen, THEATER_NAMES[t], labelX, theaterY + 8, textCol);
                if (isCurTheater) {
                    BitmapFont.drawTextXL(screen, " *", labelX + BitmapFont.getTextXLWidth(THEATER_NAMES[t]),
                        theaterY + 8, textCol);
                }
            } else {
                BitmapFont.drawLineXL(screen, "ACT 0", t + 1, " [LOCKED]", labelX, theaterY + 8, textCol);
            }
        }

        int act = game.campaignManager.currentTheater;
        int startOp = CampaignManager.getTheaterStartMission(act);
        int opCount = CampaignManager.getTheaterMissionCount(act);
        int tabGap = (act == 2) ? 10 : 8;
        int tabW = (act == 2) ? 254 : 190;
        int tabH = BRIEFING_TAB_H;
        int tabY = BRIEFING_TAB_Y;
        for (int i = 0; i < opCount; i++) {
            int opIdx = startOp + i;
            int tabX = bx + 24 + i * (tabW + tabGap);
            boolean isCur = (opIdx == game.campaignManager.currentMissionIndex);
            boolean isUnlocked = (opIdx <= game.campaignManager.highestUnlockedMission);
            boolean isHover = (game.mouseX >= tabX && game.mouseX < tabX + tabW && game.mouseY >= tabY && game.mouseY < tabY + tabH);

            int tabBg = isCur ? 0xFF2A2416 : (isUnlocked ? (isHover ? 0xFF223548 : 0xFF141F2D) : 0xFF0D141E);
            int tabBorder = isCur ? Palette.BLUE_PALE : (isUnlocked ? (isHover ? Palette.WHITE : Palette.TAN) : 0xFF334155);
            screen.fillRect(tabX, tabY, tabW, tabH, tabBg);

            screen.drawLine(tabX, tabY, tabX + tabW - 1, tabY, tabBorder);
            screen.drawLine(tabX, tabY, tabX, tabY + tabH - 1, tabBorder);
            screen.drawLine(tabX + tabW - 1, tabY, tabX + tabW - 1, tabY + tabH - 1, tabBorder);
            screen.drawLine(tabX, tabY + tabH - 1, tabX + tabW - 1, tabY + tabH - 1, tabBorder);

            int textCol;
            String opPrefix = (opIdx < 9) ? "OP 0" : "OP ";
            String opSuffix = isCur ? " [ACTIVE]" : (isUnlocked ? " READY" : " [LOCK]");
            textCol = isCur ? Palette.BLUE_SOFT : (isUnlocked ? Palette.TAN : 0xFF475569);
            int tw = BitmapFont.getTextXLWidth(opPrefix) + BitmapFont.xlIntWidth(opIdx + 1)
                + BitmapFont.getTextXLWidth(opSuffix);
            BitmapFont.drawLineXL(screen, opPrefix, opIdx + 1, opSuffix, tabX + (tabW - tw) / 2, tabY + 7, textCol);
        }

        int infoY = by + 116;
        BitmapFont.drawText(screen, m.codename, bx + 24, infoY, Palette.BLUE_SOFT, 2);
        BitmapFont.drawTextXL(screen, m.titleUpper, bx + 24, infoY + 22, Palette.TAN);
        BitmapFont.drawTextXL(screen, " // ", bx + 24 + BitmapFont.getTextXLWidth(m.titleUpper), infoY + 22, Palette.TAN);
        BitmapFont.drawTextXL(screen, m.sector, bx + 24 + BitmapFont.getTextXLWidth(m.titleUpper) + BitmapFont.getTextXLWidth(" // "),
            infoY + 22, Palette.TAN);
        screen.drawLine(bx + 20, infoY + 38, bx + bw - 20, infoY + 38, 0xFF2A3D4D);

        int leftX = bx + 24;
        int leftY = infoY + 46;
        BitmapFont.drawTextXL(screen, "TRANSMISSION LOG:", leftX, leftY, 0xFF94A3B8);
        screen.drawLine(leftX, leftY + 14, leftX + 460, leftY + 14, Palette.BROWN_DARK);

        int maxChars = (int) (game.campaignManager.briefingTimer * 45);
        int drawnChars = 0;
        int textY = BRIEFING_TEXT_Y;
        for (int i = 0; i < m.briefingText.length; i++) {
            String line = m.briefingText[i];
            if (drawnChars >= maxChars) break;
            int avail = maxChars - drawnChars;
            textY = BitmapFont.drawTextXLWrapped(screen, line, BRIEFING_TEXT_X, textY,
                BRIEFING_TEXT_W, 0xFFE2E8F0, avail);
            drawnChars += line.length();
        }

        int reconY = briefingReconY(m.briefingText);
        int reconW = 460;
        int reconH = 172;
        screen.fillRect(leftX, reconY, reconW, reconH, 0xFF0D141F);
        screen.drawLine(leftX, reconY, leftX + reconW - 1, reconY, Palette.ORANGE_DEEP);
        screen.drawLine(leftX, reconY, leftX, reconY + reconH - 1, Palette.ORANGE_DEEP);
        screen.drawLine(leftX + reconW - 1, reconY, leftX + reconW - 1, reconY + reconH - 1, Palette.ORANGE_DEEP);
        screen.drawLine(leftX, reconY + reconH - 1, leftX + reconW - 1, reconY + reconH - 1, Palette.ORANGE_DEEP);

        BitmapFont.drawTextXL(screen, "TACTICAL RECONNAISSANCE DOSSIER", leftX + 12, reconY + 10, Palette.TAN);
        screen.drawLine(leftX + 12, reconY + 24, leftX + reconW - 12, reconY + 24, Palette.BROWN_DARK);

        BitmapFont.drawTextXL(screen, "SECTOR ENVIRONMENT: PLANET KRYOS BADLANDS", leftX + 12, reconY + 34, 0xFFCBD5E1);
        String threatStr = m.enemyDifficulty.threatProfile();
        BitmapFont.drawTextXL(screen, "THREAT PROFILE: ", leftX + 12, reconY + 52, Palette.BLUE_SOFT);
        BitmapFont.drawTextXL(screen, threatStr, leftX + 12 + BitmapFont.getTextXLWidth("THREAT PROFILE: "),
            reconY + 52, Palette.BLUE_SOFT);
        BitmapFont.drawLineXL(screen, "INITIAL DEPLOYMENT CAPITAL: $", m.startingCredits, null,
            leftX + 12, reconY + 70, Palette.GREEN_DONE);

        String hostiles = switch (m.id) {
            case CampaignMission.MISSION_DESERT_DAWN -> "INDIGENOUS CRYSTAL LITHOIDS (SHARD SKITTERERS)";
            case CampaignMission.MISSION_IRON_BULWARK -> "CRIMSON SYNDICATE FORWARD RECON PATROLS";
            case CampaignMission.MISSION_VIPERS_NEST -> "DENSE SULPHUR FISSURE LITHOID SWARMS";
            case CampaignMission.MISSION_FORWARD_REDOUBT -> "SYNDICATE CHOKEPOINT MECHANIZED RAIDERS";
            case CampaignMission.MISSION_ANCIENT_ECHOES -> "PRECURSOR GUARDIAN AUTOMATA & DRONES";
            case CampaignMission.MISSION_SCAVENGER_RUN -> "ROGUE PRECURSOR SCAVENGER DRONE PACKS";
            case CampaignMission.MISSION_CRIMSON_FORTRESS -> "FORTIFIED SYNDICATE WAR FACTORY & TURRETS";
            case CampaignMission.MISSION_CONVOY_EXTRACTION -> "SYNDICATE DUNESTALKER RAIDER PACKS";
            case CampaignMission.MISSION_DREAD_HARVEST -> "DUAL-REFINERY SYNDICATE HEAVY ARMOR CITADEL";
            case CampaignMission.MISSION_TITAN_AWAKENING -> "AWAKENED PRECURSOR TITANS & VAULT DEFENDERS";
            default -> "SYNDICATE CITADEL & PRECURSOR AWAKENED HIVE";
        };
        BitmapFont.drawTextXL(screen, "CONFIRMED HOSTILES:", leftX + 12, reconY + 92, Palette.TEXT_FAIL);
        BitmapFont.drawTextXL(screen, hostiles, leftX + 12, reconY + 110, 0xFFCBD5E1);
        BitmapFont.drawTextXL(screen, "SATELLITE STATUS: SCAN COMPLETED // LOCK ACQUIRED", leftX + 12, reconY + 140, Palette.GREEN);

        int rightX = bx + 508;
        int rightY = leftY;
        BitmapFont.drawTextXL(screen, "PRIMARY DIRECTIVES:", rightX, rightY, Palette.TEXT_FAIL);
        screen.drawLine(rightX, rightY + 14, rightX + 300, rightY + 14, Palette.BROWN_DARK);

        int dirY = rightY + 24;
        for (int i = 0; i < m.primaryDirectives.length; i++) {
            String entry = m.primaryDirectives[i];
            int lines = BitmapFont.getWrappedLineCountXL(entry, BRIEFING_DIRECTIVE_W);
            BitmapFont.drawTextXLWrapped(screen, entry, rightX, dirY, BRIEFING_DIRECTIVE_W, Palette.WHITE);
            dirY += lines * BRIEFING_TEXT_LEADING + 7;
        }

        dirY += 8;
        BitmapFont.drawTextXL(screen, "SECONDARY DIRECTIVES:", rightX, dirY, 0xFFFBBF24);
        screen.drawLine(rightX, dirY + 14, rightX + 300, dirY + 14, Palette.BROWN_DARK);
        dirY += 24;
        for (int i = 0; i < m.secondaryDirectives.length; i++) {
            String entry = m.secondaryDirectives[i];
            int lines = BitmapFont.getWrappedLineCountXL(entry, BRIEFING_DIRECTIVE_W);
            BitmapFont.drawTextXLWrapped(screen, entry, rightX, dirY, BRIEFING_DIRECTIVE_W, 0xFFCBD5E1);
            dirY += lines * BRIEFING_TEXT_LEADING + 7;
        }

        dirY += 12;
        BitmapFont.drawTextXL(screen, "AUTHORIZED HARDWARE:", rightX, dirY, Palette.TAN);
        screen.drawLine(rightX, dirY + 14, rightX + 300, dirY + 14, Palette.BROWN_DARK);
        dirY += 24;

        BitmapFont.drawLineXL(screen, "TIER ", m.id + 1, " VANGUARD TECH:", rightX, dirY, Palette.BLUE_SOFT);
        dirY += 18;
        int assetRows = Math.max(m.unlockedStructures.length, m.unlockedUnits.length);
        int unitColumnX = rightX + 6 + ASSET_COLUMN_W;
        for (int i = 0; i < assetRows; i++) {
            int ay = dirY + i * 17;
            if (i < m.unlockedStructures.length) {
                BitmapFont.drawTextXL(screen, "- ", rightX + 6, ay, 0xFFE2E8F0);
                BitmapFont.drawTextXL(screen, m.unlockedStructures[i],
                    rightX + 6 + BitmapFont.getTextXLWidth("- "), ay, 0xFFE2E8F0);
            }
            if (i < m.unlockedUnits.length) {
                BitmapFont.drawTextXL(screen, "- ", unitColumnX, ay, 0xFFE2E8F0);
                BitmapFont.drawTextXL(screen, m.unlockedUnits[i],
                    unitColumnX + BitmapFont.getTextXLWidth("- "), ay, 0xFFE2E8F0);
            }
        }

        int btn1X = BRIEFING_DEPLOY_X;
        int btn1Y = BRIEFING_DEPLOY_Y;
        int btn1W = BRIEFING_DEPLOY_W;
        int btn1H = BRIEFING_DEPLOY_H;
        boolean hover1 = (game.mouseX >= btn1X && game.mouseX < btn1X + btn1W && game.mouseY >= btn1Y && game.mouseY < btn1Y + btn1H);

        screen.fillRect(btn1X, btn1Y, btn1W, btn1H, hover1 ? 0xFF16A34A : 0xFF15803D);
        int border1 = hover1 ? Palette.WHITE : 0xFF22C55E;
        screen.drawLine(btn1X, btn1Y, btn1X + btn1W - 1, btn1Y, border1);
        screen.drawLine(btn1X, btn1Y, btn1X, btn1Y + btn1H - 1, border1);
        screen.drawLine(btn1X + btn1W - 1, btn1Y, btn1X + btn1W - 1, btn1Y + btn1H - 1, border1);
        screen.drawLine(btn1X, btn1Y + btn1H - 1, btn1X + btn1W - 1, btn1Y + btn1H - 1, border1);

        String depLabel = "DEPLOY OPERATION";
        int dlw = depLabel.length() * 12;
        BitmapFont.drawText(screen, depLabel, btn1X + (btn1W - dlw) / 2, btn1Y + 14, Palette.WHITE, 2);

        int btn2X = BRIEFING_ABORT_X;
        int btn2Y = BRIEFING_ABORT_Y;
        int btn2W = BRIEFING_ABORT_W;
        int btn2H = BRIEFING_ABORT_H;
        boolean hover2 = (game.mouseX >= btn2X && game.mouseX < btn2X + btn2W && game.mouseY >= btn2Y && game.mouseY < btn2Y + btn2H);

        screen.fillRect(btn2X, btn2Y, btn2W, btn2H, hover2 ? 0xFF991B1B : 0xFF7F1D1D);
        int border2 = hover2 ? Palette.WHITE : 0xFFEF4444;
        screen.drawLine(btn2X, btn2Y, btn2X + btn2W - 1, btn2Y, border2);
        screen.drawLine(btn2X, btn2Y, btn2X, btn2Y + btn2H - 1, border2);
        screen.drawLine(btn2X + btn2W - 1, btn2Y, btn2X + btn2W - 1, btn2Y + btn2H - 1, border2);
        screen.drawLine(btn2X, btn2Y + btn2H - 1, btn2X + btn2W - 1, btn2Y + btn2H - 1, border2);

        String abortLabel = "ABORT TO MENU";
        int alw = abortLabel.length() * 12;
        BitmapFont.drawText(screen, abortLabel, btn2X + (btn2W - alw) / 2, btn2Y + 14, Palette.WHITE, 2);
    }
}
