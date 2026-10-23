package wartech.core;

// Mission setup screen input: difficulty, funds, seed, scale, mode, deploy

import wartech.ai.Difficulty;
import wartech.map.MapSize;
import wartech.render.Renderer;

public final class SetupInput {
    private final WarTech game;

    public SetupInput(WarTech game) {
        this.game = game;
    }

    public void handleClick(int x, int y, int button) {
        if (button != MouseButton.LEFT) return;

        int x0 = Renderer.SETUP_X;

        // Difficulty
        int cardY = Renderer.SETUP_DIFF_Y;
        int cardX1 = x0 + Renderer.SETUP_COL_X;
        int cardX2 = x0 + Renderer.SETUP_CARD_X2;
        int cardX3 = x0 + Renderer.SETUP_CARD_X3;
        if (y >= cardY && y < cardY + Renderer.SETUP_CARD_H) {
            if (x >= cardX1 && x < cardX1 + Renderer.SETUP_CARD_W) {
                game.selectedDifficulty = Difficulty.EASY;
                game.statusToast = "DIFFICULTY: " + Difficulty.EASY.label() + " (" + Difficulty.EASY.tier() + ")";
                return;
            } else if (x >= cardX2 && x < cardX2 + Renderer.SETUP_CARD_W) {
                game.selectedDifficulty = Difficulty.MEDIUM;
                game.statusToast = "DIFFICULTY: " + Difficulty.MEDIUM.label() + " (" + Difficulty.MEDIUM.tier() + ")";
                return;
            } else if (x >= cardX3 && x < cardX3 + Renderer.SETUP_CARD_W) {
                game.selectedDifficulty = Difficulty.HARD;
                game.statusToast = "DIFFICULTY: " + Difficulty.HARD.label() + " (" + Difficulty.HARD.tier() + ")";
                return;
            }
        }

        // Credits
        int fundY = Renderer.SETUP_FUND_Y;
        int fundX1 = x0 + Renderer.SETUP_COL_X;
        int fundX2 = x0 + Renderer.SETUP_FUND_X2;
        int fundX3 = x0 + Renderer.SETUP_FUND_X3;
        int fundX4 = x0 + Renderer.SETUP_FUND_X4;
        if (y >= fundY && y < fundY + Renderer.SETUP_FUND_H) {
            if (x >= fundX1 && x < fundX1 + Renderer.SETUP_FUND_W) {
                game.startingCredits = 800;
                game.statusToast = "STARTING CREDITS: 800";
                return;
            } else if (x >= fundX2 && x < fundX2 + Renderer.SETUP_FUND_W) {
                game.startingCredits = 1200;
                game.statusToast = "STARTING CREDITS: 1200";
                return;
            } else if (x >= fundX3 && x < fundX3 + Renderer.SETUP_FUND_W) {
                game.startingCredits = 2500;
                game.statusToast = "STARTING CREDITS: 2500";
                return;
            } else if (x >= fundX4 && x < fundX4 + Renderer.SETUP_FUND_W) {
                game.startingCredits = 5000;
                game.statusToast = "STARTING CREDITS: 5000";
                return;
            }
        }

        // Map Seed
        int seedY = Renderer.SETUP_SEED_Y;
        int seedMinusX = x0 + Renderer.SETUP_SEED_MINUS_X;
        int seedPlusX = x0 + Renderer.SETUP_SEED_PLUS_X;
        int seedRandomX = x0 + Renderer.SETUP_SEED_RANDOM_X;
        if (y >= seedY && y < seedY + Renderer.SETUP_SEED_HIT_H) {
            if (x >= seedMinusX && x < seedMinusX + Renderer.SETUP_SEED_STEP_W) {
                game.mapSeed = Math.max(1L, game.mapSeed - 1L);
                game.statusToast = "SEED DECREASED: " + game.mapSeed;
                return;
            } else if (x >= seedPlusX && x < seedPlusX + Renderer.SETUP_SEED_STEP_W) {
                game.mapSeed = game.mapSeed + 1L;
                game.statusToast = "SEED INCREASED: " + game.mapSeed;
                return;
            } else if (x >= seedRandomX && x < seedRandomX + Renderer.SETUP_SEED_RANDOM_W) {
                game.mapSeed = (long) game.simRng.nextInt(1, 99998);
                game.statusToast = "NEW SEED GENERATED: " + game.mapSeed;
                return;
            }
        }

        // Map Size
        int scaleY = Renderer.SETUP_SCALE_Y;
        int scaleX1 = x0 + Renderer.SETUP_COL_X;
        int scaleX2 = x0 + Renderer.SETUP_SCALE_X2;
        if (y >= scaleY && y < scaleY + Renderer.SETUP_SCALE_H) {
            if (x >= scaleX1 && x < scaleX1 + Renderer.SETUP_SCALE_W) {
                game.selectedMapSize = MapSize.CODE_SMALL_64;
                game.statusToast = "MAP SCALE: 64x64 SKIRMISH";
                return;
            } else if (x >= scaleX2 && x < scaleX2 + Renderer.SETUP_SCALE_W) {
                game.selectedMapSize = MapSize.CODE_LARGE_128;
                game.statusToast = "MAP SCALE: 128x128 EPIC WARZONE";
                return;
            }
        }

        // Play Skirmish, Start Campaign, Load Campaign
        int modeBtnY = Renderer.SETUP_MODE_Y;
        int modeX1 = x0 + Renderer.SETUP_COL_X;
        int modeX2 = x0 + Renderer.SETUP_CARD_X2;
        int modeX3 = x0 + Renderer.SETUP_CARD_X3;
        if (y >= modeBtnY && y < modeBtnY + Renderer.SETUP_MODE_H) {
            if (x >= modeX1 && x < modeX1 + Renderer.SETUP_CARD_W) {
                game.selectedGameMode = GameMode.SKIRMISH;
                game.statusToast = "SKIRMISH MODE SELECTED";
                launchSelectedMode();
                return;
            } else if (x >= modeX2 && x < modeX2 + Renderer.SETUP_CARD_W) {
                game.selectedGameMode = GameMode.START_CAMPAIGN;
                launchSelectedMode();
                return;
            } else if (x >= modeX3 && x < modeX3 + Renderer.SETUP_CARD_W) {
                game.selectedGameMode = GameMode.LOAD_CAMPAIGN;
                launchSelectedMode();
                return;
            }
        }

        // Deploy
        int depY = Renderer.SETUP_DEPLOY_Y;
        int depX = x0 + (Renderer.SETUP_W - Renderer.SETUP_DEPLOY_W) / 2;
        if (x >= depX && x < depX + Renderer.SETUP_DEPLOY_W && y >= depY && y < depY + Renderer.SETUP_DEPLOY_H) {
            launchSelectedMode();
        }
    }

    public void launchSelectedMode() {
        switch (game.selectedGameMode) {
            case START_CAMPAIGN -> {
                game.showSetupScreen = false;
                game.campaignManager.openBriefing(0);
            }
            case LOAD_CAMPAIGN -> {
                game.showSetupScreen = false;
                game.campaignManager.openBriefing(game.campaignManager.highestUnlockedMission);
            }
            case SKIRMISH -> game.startSkirmish(game.selectedMapSize, game.selectedDifficulty,
                game.startingCredits, game.mapSeed);
        }
    }
}
