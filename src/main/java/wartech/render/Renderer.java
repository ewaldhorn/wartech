package wartech.render;

// Direct memory framebuffer software rasterizer
// Draws a 704x768 tactical viewport and a 320x768 retro command sidebar directly onto Surface

import wartech.core.Camera;
import wartech.core.Display;
import wartech.core.MatchState;
import wartech.core.Surface;
import wartech.core.WarTech;
import wartech.map.GameMap;

public final class Renderer {

    public static final int SCREEN_WIDTH = Display.WIDTH;
    public static final int SCREEN_HEIGHT = Display.HEIGHT;
    public static final int VIEW_WIDTH = Display.VIEW_WIDTH;
    
    public static final int SIDEBAR_WIDTH = Display.SIDEBAR_WIDTH;
    public static final int SIDEBAR_X = Display.SIDEBAR_X;
    
    public static final int INTEL_X = SIDEBAR_X + 12;
    public static final int INTEL_Y = 498;
    public static final int INTEL_W = 296;
    public static final int INTEL_H = 264;

    public static final int MINIMAP_X = 744;
    public static final int MINIMAP_Y = 10;
    public static final int MINIMAP_SIZE = 240;

    public static final int ZOOM_BAR_Y0 = 252;
    public static final int ZOOM_BAR_Y1 = 274;
    public static final int ZOOM_MINUS_X0 = SIDEBAR_X + 70;
    public static final int ZOOM_MINUS_X1 = SIDEBAR_X + 115;
    public static final int ZOOM_MODE_X0 = SIDEBAR_X + 115;
    public static final int ZOOM_MODE_X1 = SIDEBAR_X + 205;
    public static final int ZOOM_PLUS_X0 = SIDEBAR_X + 205;
    public static final int ZOOM_PLUS_X1 = SIDEBAR_X + 250;

    public static final int SIDEBAR_TABS_Y0 = 274;
    public static final int SIDEBAR_TABS_Y1 = 310;
    public static final int SIDEBAR_COL_SPLIT_X = SIDEBAR_X + 160;

    public static final int BUILD_GRID_Y0 = 312;
    public static final int BUILD_GRID_Y1 = 498;
    public static final int BUILD_GRID_ROW_H = 37;
    public static final int BUILD_GRID_X0 = SIDEBAR_X + 12;
    public static final int BUILD_GRID_X1 = SIDEBAR_X + 316;
    public static final int BUILD_GRID_ROWS = 5;
    public static final int BUILD_GRID_COLS = 2;

    public static final int INSPECTOR_Y0 = 708;
    public static final int INSPECTOR_Y1 = 758;
    public static final int INSPECTOR_BTN1_X0 = 720;
    public static final int INSPECTOR_BTN1_X1 = 810;
    public static final int INSPECTOR_BTN2_X0 = 816;
    public static final int INSPECTOR_BTN2_X1 = 908;
    public static final int INSPECTOR_BTN3_X0 = 914;
    public static final int INSPECTOR_BTN3_X1 = 1006;
    public static final int INSPECTOR_WIDE1_X1 = 862;
    public static final int INSPECTOR_WIDE2_X0 = 866;
    public static final int INSPECTOR_WIDE2_X1 = 1008;

    public static final int SETUP_X = 112;
    public static final int SETUP_Y = 44;
    public static final int SETUP_W = 800;
    public static final int SETUP_H = 680;
    public static final int SETUP_DIFF_Y = 150;
    public static final int SETUP_FUND_Y = 280;
    public static final int SETUP_SEED_Y = 356;
    public static final int SETUP_SCALE_Y = 436;
    public static final int SETUP_MODE_Y = 502;
    public static final int SETUP_MODE_H = 44;
    public static final int SETUP_DEPLOY_Y = 576;
    public static final int SETUP_DEPLOY_H = 54;
    public static final int SETUP_COL_X = 24;
    public static final int SETUP_CARD_W = 240;
    public static final int SETUP_CARD_H = 82;
    public static final int SETUP_CARD_X2 = 280;
    public static final int SETUP_CARD_X3 = 536;
    public static final int SETUP_FUND_W = 178;
    public static final int SETUP_FUND_H = 40;
    public static final int SETUP_FUND_X2 = 216;
    public static final int SETUP_FUND_X3 = 408;
    public static final int SETUP_FUND_X4 = 600;
    public static final int SETUP_SEED_MINUS_X = 280;
    public static final int SETUP_SEED_PLUS_X = 346;
    public static final int SETUP_SEED_STEP_W = 50;
    public static final int SETUP_SEED_RANDOM_X = 412;
    public static final int SETUP_SEED_RANDOM_W = 250;
    public static final int SETUP_SEED_BOX_W = 240;
    public static final int SETUP_SEED_H = 38;
    public static final int SETUP_SEED_HIT_H = 40;
    public static final int SETUP_SCALE_W = 368;
    public static final int SETUP_SCALE_H = 40;
    public static final int SETUP_SCALE_X2 = 408;
    public static final int SETUP_DEPLOY_W = 440;


    public static final int DEBRIEF_W = 520;
    public static final int DEBRIEF_H = 380;
    public static final int DEBRIEF_X = (VIEW_WIDTH - DEBRIEF_W) / 2;
    public static final int DEBRIEF_Y = (SCREEN_HEIGHT - DEBRIEF_H) / 2;
    public static final int DEBRIEF_BTN_H = 44;
    public static final int DEBRIEF_BTN_Y = DEBRIEF_Y + DEBRIEF_H - 64;
    public static final int DEBRIEF_BTN1_X = DEBRIEF_X + 20;
    public static final int DEBRIEF_BTN1_W = 245;
    public static final int DEBRIEF_BTN2_X = DEBRIEF_X + 280;
    public static final int DEBRIEF_BTN2_W = 220;

    public static final int BRIEFING_X = 96;
    public static final int BRIEFING_Y = 48;
    public static final int BRIEFING_W = 832;
    public static final int BRIEFING_H = 672;
    public static final int BRIEFING_THEATER_Y = BRIEFING_Y + 50;
    public static final int BRIEFING_THEATER_H = 28;
    public static final int BRIEFING_TAB_Y = BRIEFING_Y + 82;
    public static final int BRIEFING_TAB_H = 26;
    public static final int BRIEFING_DEPLOY_X = BRIEFING_X + 480;
    public static final int BRIEFING_DEPLOY_Y = BRIEFING_Y + BRIEFING_H - 58;
    public static final int BRIEFING_DEPLOY_W = 328;
    public static final int BRIEFING_DEPLOY_H = 44;
    public static final int BRIEFING_ABORT_X = BRIEFING_X + 24;
    public static final int BRIEFING_ABORT_Y = BRIEFING_Y + BRIEFING_H - 58;
    public static final int BRIEFING_ABORT_W = 220;
    public static final int BRIEFING_ABORT_H = 44;
    public static final int BRIEFING_TEXT_X = BRIEFING_X + 24;
    public static final int BRIEFING_TEXT_Y = BRIEFING_Y + 186;
    public static final int BRIEFING_TEXT_W = 448;
    public static final int BRIEFING_TEXT_LEADING = 15;
    public static final int BRIEFING_DIRECTIVE_W = 287;

    public static final int OBJ_X = 14;
    public static final int OBJ_Y = 14;
    public static final int OBJ_W = 360;
    public static final int OBJ_H = 104;
    public static final int OBJ_HEADER_H = 24;
    public static final int OBJ_MIN_X = OBJ_X + OBJ_W - 28;
    public static final int OBJ_MIN_Y = OBJ_Y + 4;
    public static final int OBJ_MIN_W = 22;
    public static final int OBJ_MIN_H = 18;
    public static final int OBJ_BADGE_W = 164;
    public static final int OBJ_BADGE_H = 26;

    public static final int ASSET_COLUMN_W = 156;

    public final Surface screen;
    private final ViewportRenderer viewport;
    private final SidebarRenderer sidebar;
    private final DossierRenderer dossier;
    private final ScreenRenderer screens;

    public Renderer(Surface screen) {
        this.screen = screen;
        this.viewport = new ViewportRenderer(screen);
        this.sidebar = new SidebarRenderer(screen);
        this.dossier = new DossierRenderer(screen);
        this.screens = new ScreenRenderer(screen);
    }


    public void render(GameMap map, Camera camera, WarTech game) {
        boolean fullScreenModal = game != null && (game.showSetupScreen
            || (game.campaignManager != null && game.campaignManager.briefingActive));
        if (fullScreenModal) {
            screen.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0xFF06090C);
        } else {
            screen.fillRect(0, 0, VIEW_WIDTH, SCREEN_HEIGHT, 0xFF06090C);
            viewport.renderViewport(map, camera, game);

            if (game != null && game.campaignManager != null && game.campaignManager.campaignActive && game.matchState == MatchState.IN_PROGRESS) {
                screens.renderCampaignObjectiveTracker(game);
            }

            sidebar.renderSidebar(map, camera, game);

            dossier.renderTacticalIntel(game, map);
        }

        if (game != null && game.matchState != MatchState.IN_PROGRESS && !game.showSetupScreen) {
            screens.renderMatchEndOverlay(game);
        }

        if (game != null && game.showSetupScreen) {
            screens.renderMissionSetupScreen(game);
        }

        if (game != null && game.campaignManager != null && game.campaignManager.briefingActive) {
            screens.renderCampaignBriefing(game);
        }
    }
}
