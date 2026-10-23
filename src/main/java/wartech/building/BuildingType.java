package wartech.building;

// Building specifications and costs
// Ideally needs to be moved into some sort of config file arrangement, but later.

public enum BuildingType {
    CON_YARD("Construction Yard", "CON YARD", 3, 3, 1000, 10, 1500),
    WIND_TURBINE("Wind Turbine", "WIND TURBINE", 2, 2, 300, 35, 600),
    REFINERY("Crystal Refinery", "REFINERY", 3, 2, 800, -15, 1000),
    BARRACKS("Barracks", "BARRACKS", 2, 2, 400, -10, 700),
    LIGHT_FACTORY("Light Factory", "LIGHT FACT", 3, 2, 600, -20, 850),
    HEAVY_FACTORY("War Factory", "WAR FACT", 3, 2, 1000, -30, 1200),
    RADAR("Radar Outpost", "RADAR", 2, 2, 500, -25, 750),
    TURRET("Gun Turret", "TURRET", 1, 1, 250, -10, 450),
    REPAIR_FACILITY("Repair Facility", "REPAIR FAC", 3, 2, 700, -20, 850);

    public final String displayName;
    public final String displayNameUpper;
    public final String shortName;
    public final int widthTiles;
    public final int heightTiles;
    public final int cost;
    public final int powerNet;
    public final int maxHp;

    BuildingType(String displayName, String shortName, int widthTiles, int heightTiles, int cost, int powerNet, int maxHp) {
        this.displayName = displayName;
        this.displayNameUpper = displayName.toUpperCase();
        this.shortName = shortName;
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.cost = cost;
        this.powerNet = powerNet;
        this.maxHp = maxHp;
    }

    public BuildingType getRequiredBuilding() {
        return switch (this) {
            case WIND_TURBINE -> CON_YARD;
            case REFINERY, BARRACKS -> WIND_TURBINE;
            case LIGHT_FACTORY, HEAVY_FACTORY, REPAIR_FACILITY, RADAR -> REFINERY;
            case TURRET -> BARRACKS;
            case CON_YARD -> null;
        };
    }

    public boolean canProduceUnits() {
        return switch (this) {
            case BARRACKS, LIGHT_FACTORY, HEAVY_FACTORY, REPAIR_FACILITY -> true;
            default -> false;
        };
    }

    // Does this have units exiting? We need to make sure there's room for that
    public boolean deploysUnitsAtExit() {
        return this == REFINERY || canProduceUnits();
    }
}
