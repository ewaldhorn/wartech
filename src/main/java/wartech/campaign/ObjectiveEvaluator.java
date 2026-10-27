package wartech.campaign;

// Campaign objective evaluation
// Ultimately I need to rework this, it's tedious to manage and I have some ideas how to make it
// more config-driven. But later, there's tons of other bugs to attend to.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.MatchState;
import wartech.core.WarTech;

public final class ObjectiveEvaluator {
    private final CampaignManager cm;

    public ObjectiveEvaluator(CampaignManager cm) {
        this.cm = cm;
    }

    private void bonusTask(boolean complete) {
        cm.bonusIsSurvival = false;
        if (cm.bonusStatus == BonusState.FAILED || cm.bonusStatus == BonusState.ACHIEVED) return;
        cm.bonusStatus = complete ? BonusState.ACHIEVED : BonusState.PENDING;
        cm.secondaryDone = (cm.bonusStatus == BonusState.ACHIEVED);
    }

    private void bonusSurvival(boolean broken) {
        cm.bonusIsSurvival = true;
        if (cm.bonusStatus == BonusState.FAILED) return;
        if (broken) cm.bonusStatus = BonusState.FAILED;
        cm.secondaryDone = (cm.bonusStatus == BonusState.ACHIEVED);
    }

    public void evaluateObjectives(WarTech game) {
        switch (cm.currentMissionIndex) {
            case CampaignMission.MISSION_DESERT_DAWN -> evaluateOp0(game);
            case CampaignMission.MISSION_IRON_BULWARK -> evaluateOp1(game);
            case CampaignMission.MISSION_VIPERS_NEST -> evaluateOp2(game);
            case CampaignMission.MISSION_FORWARD_REDOUBT -> evaluateOp3(game);
            case CampaignMission.MISSION_ANCIENT_ECHOES -> evaluateOp4(game);
            case CampaignMission.MISSION_SCAVENGER_RUN -> evaluateOp5(game);
            case CampaignMission.MISSION_CRIMSON_FORTRESS -> evaluateOp6(game);
            case CampaignMission.MISSION_CONVOY_EXTRACTION -> evaluateOp7(game);
            case CampaignMission.MISSION_DREAD_HARVEST -> evaluateOp8(game);
            case CampaignMission.MISSION_TITAN_AWAKENING -> evaluateOp9(game);
            case CampaignMission.MISSION_APEX_CATACLYSM -> evaluateOp10(game);
            default -> { }
        }
    }

    private void evaluateOp0(WarTech game) { // Desert Dawn
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (game.playerCredits >= m.targetCredits);
        cm.primary2Done = (cm.skitterersKilled >= m.targetKills);
        bonusSurvival(game.playerPowerNet < 0);
    }

    private void evaluateOp1(WarTech game) { // Iron Bulwark
        CampaignMission m = cm.getCurrentMission();
        boolean pYardAlive = false;
        int enemyCount = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed()) {
                if (b.faction == Faction.PLAYER && b.type == BuildingType.CON_YARD) pYardAlive = true;
                if (b.faction == Faction.ENEMY) enemyCount++;
            }
        }
        cm.primary1Done = pYardAlive;
        cm.primary1IsSurvival = true;
        cm.primary2Done = (enemyCount == 0);
        int pTurrets = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && !b.constructing
                    && b.faction == Faction.PLAYER && b.type == BuildingType.TURRET) {
                pTurrets++;
            }
        }
        bonusTask(pTurrets >= m.targetBonusCount);
        if (!pYardAlive && game.gameTime > 5.0) {
            triggerDefeat(game, "CRITICAL LOSS: CONSTRUCTION YARD DESTROYED", CampaignManager.DEFEAT_CON_YARD);
        }
    }

    private void evaluateOp2(WarTech game) { // Viper's Nest
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (cm.skitterersKilled >= m.targetKills);
        cm.primary2Done = (game.playerCredits >= m.targetCredits);
        bonusSurvival(cm.trikesLost > 0);
    }

    private void evaluateOp3(WarTech game) { // Forward Redoubt
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (cm.syndicateUnitsKilled >= m.targetKills);
        int pTurrets = 0;
        Building pYard = null;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.PLAYER) {
                if (b.type == BuildingType.TURRET && !b.constructing) pTurrets++;
                if (b.type == BuildingType.CON_YARD) pYard = b;
            }
        }
        cm.primary2Done = (pTurrets >= m.targetBonusCount);
        bonusSurvival(pYard == null || pYard.hp < pYard.maxHp);
        if (pYard == null && game.gameTime > 5.0) {
            triggerDefeat(game, "CRITICAL LOSS: CONSTRUCTION YARD DESTROYED", CampaignManager.DEFEAT_CON_YARD);
        }
    }

    private void evaluateOp4(WarTech game) { // Ancient Echoes
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (cm.walkersKilled >= m.targetKills);
        cm.primary2Done = (game.playerCredits >= m.targetCredits);
        bonusTask(cm.dronesKilled >= m.targetBonusCount);
    }

    private void evaluateOp5(WarTech game) { // Scavenger Run
        CampaignMission m = cm.getCurrentMission();
        int target = m.targetKills > 0 ? m.targetKills : CampaignManager.SCAVENGER_RUN_DRONE_TARGET;
        cm.primary1Done = (cm.dronesKilled >= target);
        int enemyCount = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) enemyCount++;
        }
        cm.primary2Done = (enemyCount == 0);
        bonusSurvival(game.threatSystem != null && game.threatSystem.reinforcementsSpawned > 0);
    }

    private void evaluateOp6(WarTech game) { // Crimson Fortress
        CampaignMission m = cm.getCurrentMission();
        boolean enemyYardAlive = false;
        boolean enemyWarFacAlive = false;
        int eTurrets = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) {
                if (b.type == BuildingType.CON_YARD) enemyYardAlive = true;
                if (b.type == BuildingType.HEAVY_FACTORY) enemyWarFacAlive = true;
                if (b.type == BuildingType.TURRET) eTurrets++;
            }
        }
        cm.primary1Done = (!enemyYardAlive && !enemyWarFacAlive);
        cm.primary2Done = (eTurrets == 0);
        bonusTask(cm.repairsPerformed >= m.targetBonusCount);
    }

    private void evaluateOp7(WarTech game) { // Convoy Extraction
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (game.playerCredits >= m.targetCredits);
        int enemyCount = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) enemyCount++;
        }
        cm.primary2Done = (enemyCount == 0);
        bonusTask(cm.repairsPerformed >= m.targetBonusCount);
    }

    private void evaluateOp8(WarTech game) { // Dread Harvest
        boolean enemyYardAlive = false;
        boolean enemyWarFacAlive = false;
        int eRefineries = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) {
                if (b.type == BuildingType.CON_YARD) enemyYardAlive = true;
                if (b.type == BuildingType.HEAVY_FACTORY) enemyWarFacAlive = true;
                if (b.type == BuildingType.REFINERY) eRefineries++;
            }
        }
        cm.primary1Done = (eRefineries == 0);
        cm.primary2Done = (!enemyYardAlive && !enemyWarFacAlive);
        bonusSurvival(cm.tanksLost > 0);
    }

    private void evaluateOp9(WarTech game) { // Titan Awakening
        CampaignMission m = cm.getCurrentMission();
        cm.primary1Done = (cm.walkersKilled >= m.targetKills);
        boolean enemyYardAlive = false;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY && b.type == BuildingType.CON_YARD) {
                enemyYardAlive = true;
                break;
            }
        }
        cm.primary2Done = !enemyYardAlive;
        double limit = m.targetTimeLimit > 0.0 ? m.targetTimeLimit : 480.0;
        bonusSurvival(game.gameTime > limit);
    }

    private void evaluateOp10(WarTech game) { // Apex Cataclysm
        CampaignMission m = cm.getCurrentMission();
        int eBldgs = 0;
        for (int i = 0; i < game.buildingCount; i++) {
            Building b = game.buildings[i];
            if (b != null && !b.isDestroyed() && b.faction == Faction.ENEMY) eBldgs++;
        }
        cm.primary1Done = (eBldgs == 0);
        cm.primary2Done = (cm.walkersKilled >= m.targetKills);
        bonusSurvival(cm.collectorsLost > 0);
    }

    public void triggerVictory(WarTech game) {
        this.cm.missionAccomplished = true;
        this.cm.missionFailed = false;
        if (cm.bonusStatus == BonusState.PENDING) {
            cm.bonusStatus = cm.bonusIsSurvival ? BonusState.ACHIEVED : BonusState.FAILED;
        }
        cm.secondaryDone = (cm.bonusStatus == BonusState.ACHIEVED);
        game.matchState = MatchState.VICTORY;
        game.matchOverTimer = 0.0;
        game.statusToast = "OPERATION COMPLETE: " + cm.getCurrentMission().codename;

        if (cm.currentMissionIndex + 1 > cm.highestUnlockedMission && cm.highestUnlockedMission < cm.missions.length - 1) {
            cm.highestUnlockedMission = cm.currentMissionIndex + 1;
        }
    }

    public void triggerDefeat(WarTech game, String reason, int code) {
        this.cm.missionFailed = true;
        this.cm.missionAccomplished = false;
        this.cm.defeatReasonCode = code;
        game.matchState = MatchState.DEFEAT;
        game.matchOverTimer = 0.0;
        game.statusToast = "OPERATION FAILED: " + reason;
    }
}
