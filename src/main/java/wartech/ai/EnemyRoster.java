package wartech.ai;

// Decides which units the enemy queues: support, infantry or vehicles.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.core.WarTech;
import wartech.unit.UnitType;

public final class EnemyRoster {
    private final EnemyAI ai;

    public EnemyRoster(EnemyAI ai) {
        this.ai = ai;
    }

    public void update(AiCensus c, WarTech game) {
        queueSupport(c, game);
        queueInfantry(c, game);
        queueVehicles(c, game);
    }

    private void queueSupport(AiCensus c, WarTech game) {
        var production = ai.production;
        AiTuning.Tier tier = ai.tuning();

        if (EnemyAI.findEnemyBuilding(BuildingType.REPAIR_FACILITY, game) != null) {
            int maxMechanics = c.repairFacilities * tier.maxMechanicsPerFacility();
            if (c.mechanics + production.countQueued(UnitType.MECHANIC) < maxMechanics && production.hasRoom()) {
                production.enqueue(UnitType.MECHANIC);
            }
        }

        if (EnemyAI.findEnemyBuilding(BuildingType.BARRACKS, game) != null) {
            int maxMedics = tier.maxMedics();
            if (c.medics + production.countQueued(UnitType.MEDIC) < maxMedics
                    && game.simRng.chance(AiTuning.MEDIC_QUEUE_CHANCE)
                    && production.hasRoom()) {
                production.enqueue(UnitType.MEDIC);
            }
        }
    }

    private void queueInfantry(AiCensus c, WarTech game) {
        if (EnemyAI.findEnemyBuilding(BuildingType.BARRACKS, game) == null) return;
        var production = ai.production;
        int maxInfantry = ai.tuning().maxInfantry();
        if (c.combatInfantry + production.countQueuedCombatInfantry() >= maxInfantry || !production.hasRoom()) return;

        double roll = game.simRng.nextDouble();
        production.enqueue(roll < ai.tuning().vandalShare() ? UnitType.VANDAL : UnitType.RAIDER);
    }

    private void queueVehicles(AiCensus c, WarTech game) {
        Building heavyFact = EnemyAI.findEnemyBuilding(BuildingType.HEAVY_FACTORY, game);
        Building lightFact = EnemyAI.findEnemyBuilding(BuildingType.LIGHT_FACTORY, game);
        if (heavyFact == null && lightFact == null) return;

        var production = ai.production;
        int maxVehicles = ai.tuning().maxVehicles();
        if (c.combatVehicles + production.countQueuedCombatVehicles() >= maxVehicles || !production.hasRoom()) return;

        double roll = game.simRng.nextDouble();
        double heavyChance = ai.tuning().dreadnoughtShare();
        if (heavyFact != null && roll < heavyChance) {
            production.enqueue(UnitType.DREADNOUGHT);
        } else if (heavyFact != null) {
            production.enqueue(UnitType.RAVAGER_TANK);
        } else if (lightFact != null) {
            production.enqueue(UnitType.REAVER_BUGGY);
        }
    }
}
