package wartech.ai;

// Builds a snapshot of all the AI assets like buildings, units, things in flight etc.
// Rebuilt at the start of every think().

import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.core.WarTech;

public final class AiCensus {
    public int powerNet;
    public int pendingTurbinePower; // wind turbine(s) under construction
    public int foundations; // structures under construction, turbines included

    // How many of what do we have, including what's currently being built
    // Otherwise we build a new barracks before the first one is done
    public int conYards;
    public int refineries;
    public int heavyFactories;
    public int lightFactories;
    public int barracks;
    public int repairFacilities;
    public int turrets;

    // How many of what do we have, unit wise.
    // Do we have enough collectors? Should we rather build combat units?
    public int collectors;
    public int mechanics;
    public int medics;
    public int combatInfantry;
    public int combatVehicles;

    // Refresh the status of the census, we zero out everything then do a recount
    public void refresh(WarTech game) {
        powerNet = 0;
        pendingTurbinePower = 0;
        foundations = 0;
        conYards = 0;
        refineries = 0;
        heavyFactories = 0;
        lightFactories = 0;
        barracks = 0;
        repairFacilities = 0;
        turrets = 0;
        collectors = 0;
        mechanics = 0;
        medics = 0;
        combatInfantry = 0;
        combatVehicles = 0;

        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.isDestroyed() || b.faction != Faction.ENEMY) continue; // rubbish or not ours, we don't care

            if (b.constructing) {
                foundations++;
                if (b.type == BuildingType.WIND_TURBINE) pendingTurbinePower += b.type.powerNet;
            } else {
                powerNet += b.type.powerNet;
            }

            switch (b.type) {
                case CON_YARD -> conYards++;
                case REFINERY -> refineries++;
                case HEAVY_FACTORY -> heavyFactories++;
                case LIGHT_FACTORY -> lightFactories++;
                case BARRACKS -> barracks++;
                case REPAIR_FACILITY -> repairFacilities++;
                case TURRET -> turrets++;
                case WIND_TURBINE, RADAR -> { } // no use yet, we already know what's in flight and AI doesn't care about radar, never builds it
            }
        }

        for (int i = 0; i < game.unitCount; i++) {
            var u = game.units[i];
            if (u.isDestroyed() || u.faction != Faction.ENEMY) continue; // dead or not ours, don't care
            switch (u.type) {
                case COLLECTOR -> collectors++;
                case MECHANIC -> mechanics++;
                case MEDIC -> medics++;
                default -> {
                    // only care if it's infantry or vehicles
                    if (u.type.isInfantry()) combatInfantry++;
                    else if (u.type.isVehicle()) combatVehicles++;
                }
            }
        }
    }
}
