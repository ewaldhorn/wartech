package wartech.render;

// Display strings for building types, built once at class load.

import wartech.building.BuildingType;

final class BuildingLabels {
    final String costText;
    final String buttonMetaText;
    final String hoverStatsText;
    final String powerOutputText;
    final String footprintText;
    final String dossierLine1;
    final String dossierLine2;
    final String hoverLine1;
    final String hoverLine2;
    final String hoverLine3;

    private static final BuildingLabels[] CACHE = new BuildingLabels[BuildingType.values().length];

    static {
        for (BuildingType type : BuildingType.values()) {
            CACHE[type.ordinal()] = new BuildingLabels(type);
        }
    }

    static BuildingLabels of(BuildingType type) {
        return CACHE[type.ordinal()];
    }

    private BuildingLabels(BuildingType type) {
        String power = signed(type.powerNet);
        costText = "$" + type.cost;
        buttonMetaText = power + "P " + type.widthTiles + "x" + type.heightTiles;
        hoverStatsText = "COST: $" + type.cost + "  PWR: " + power;
        powerOutputText = "POWER OUTPUT: " + power + " MW";
        footprintText = "FOOTPRINT: " + type.widthTiles + "x" + type.heightTiles + " TILES";

        String[] dossier = describeDossier(type);
        dossierLine1 = dossier[0];
        dossierLine2 = dossier[1];
        String[] hover = describeHover(type);
        hoverLine1 = hover[0];
        hoverLine2 = hover[1];
        hoverLine3 = hover[2];
    }

    private static String signed(int powerNet) {
        return (powerNet >= 0 ? "+" : "") + powerNet;
    }

    private static String[] describeDossier(BuildingType type) {
        return switch (type) {
            case WIND_TURBINE -> new String[] {
                "Generates clean power grid.", "Prevents plant brownouts." };
            case REFINERY -> new String[] {
                "Crystal unloading dock.", "Converts spice into cash." };
            case BARRACKS -> new String[] {
                "Garrison facility.", "Trains rifle/msl troopers." };
            case LIGHT_FACTORY -> new String[] {
                "Light vehicle plant.", "Builds rapid Scout Trikes." };
            case HEAVY_FACTORY -> new String[] {
                "Heavy armor foundry.", "Produces Combat/Battle tanks." };
            case TURRET -> new String[] {
                "Automated defense gun.", "360 degree anti-armor cannon." };
            case RADAR -> new String[] {
                "Radar surveillance dish.", "Reveals sector threats." };
            case REPAIR_FACILITY -> new String[] {
                "Vehicle repair facility.", "Automates depot mechanics." };
            case CON_YARD -> new String[] {
                "Command headquarters.", "Coordinates base expansion." };
        };
    }

    private static String[] describeHover(BuildingType type) {
        return switch (type) {
            case WIND_TURBINE -> new String[] {
                "Powers all base systems.", "Essential for factories.", "Surplus prevents stalls." };
            case REFINERY -> new String[] {
                "Deploys 1 Collector.", "Mines raw energy crystal.", "Deposits cash in treasury." };
            case BARRACKS -> new String[] {
                "Infantry garrison.", "Trains Rifle & Missile", "Troopers to fight swarms." };
            case LIGHT_FACTORY -> new String[] {
                "Light vehicle plant.", "Manufactures fast Scout", "Trikes for patrol & raids." };
            case HEAVY_FACTORY -> new String[] {
                "Heavy armor foundry.", "Builds Combat Tanks &", "Siege Cannons to assault." };
            case REPAIR_FACILITY -> new String[] {
                "Vehicle Repair Depot.", "Trains Base Mechanics.", "Repairs vehicles & base." };
            case TURRET -> new String[] {
                "Automated defense gun.", "Fires shells at hostile", "swarms and enemy tanks." };
            case RADAR -> new String[] {
                "Long-range radar outpost.", "Sweeps tactical sectors.", "Detects enemy attacks." };
            case CON_YARD -> new String[] {
                "Command Headquarters.", "Expands base influence.", "Core base foundation." };
        };
    }
}
