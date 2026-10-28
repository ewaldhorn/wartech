package wartech.ai;

// The enemy's production pipeline for buildings and the FIFO unit queue.

import wartech.building.Building;
import wartech.building.BuildingType;
import wartech.combat.Effect;
import wartech.core.Faction;
import wartech.core.WarTech;
import wartech.unit.UnitType;

public final class EnemyProduction {
    private final EnemyAI ai;

    public final UnitType[] queue = new UnitType[WarTech.MAX_UNIT_QUEUE];
    public int queueCount = 0;
    public int buildSpent = 0;
    public double clock = 0.0;

    public EnemyProduction(EnemyAI ai) {
        this.ai = ai;
    }

    public void reset() {
        for (int i = 0; i < queue.length; i++) queue[i] = null;
        queueCount = 0;
        buildSpent = 0;
        clock = 0.0;
    }

    public boolean hasRoom() {
        return queueCount < WarTech.MAX_UNIT_QUEUE;
    }

    public int consumeTicks(double dt) {
        double rate = WarTech.PRODUCTION_TICKS_PER_SECOND * (ai.powerNet < 0 ? AiTuning.LOW_POWER_PRODUCTION_FACTOR : 1.0);
        clock += dt * rate;
        int ticks = (int) (clock + WarTech.PRODUCTION_CLOCK_EPSILON);
        if (ticks > 0) {
            clock -= ticks;
        }
        return ticks;
    }

    public void update(int ticks, WarTech game) {
        if (ticks <= 0) return;
        advanceFoundation(ticks, game);
        advanceUnitQueue(ticks, game);
    }

    private void advanceFoundation(int ticks, WarTech game) {
        for (int i = 0; i < game.buildingCount; i++) {
            var b = game.buildings[i];
            if (b.faction != Faction.ENEMY || !b.constructing || b.isDestroyed()) continue;

            int needed = b.type.cost - b.buildSpent;
            int spend = Math.min(ticks, Math.min(ai.credits, needed));
            if (spend > 0) {
                ai.credits -= spend;
                b.buildSpent += spend;
                b.constructionProgress = (double) b.buildSpent / b.type.cost;
                b.hp = (int) (b.maxHp * (AiTuning.FOUNDATION_MIN_HP_FRACTION
                        + (1.0 - AiTuning.FOUNDATION_MIN_HP_FRACTION) * b.constructionProgress));
            }
            if (b.buildSpent >= b.type.cost) {
                completeFoundation(b, game);
            }
            return;
        }
    }

    private void completeFoundation(Building b, WarTech game) {
        b.constructing = false;
        b.constructionProgress = 1.0;
        b.hp = b.maxHp;
        game.map.updateInfluence(game.buildings, game.buildingCount);
        if (b.type == BuildingType.REFINERY && game.unitCount < WarTech.MAX_UNITS) {
            var col = b.spawnUnit(UnitType.COLLECTOR, game.map);
            if (col.harvester != null) col.harvester.autoCollect = true;
            game.addUnit(col);
        }
    }

    private void advanceUnitQueue(int ticks, WarTech game) {
        if (queueCount <= 0) return;
        var current = queue[0];
        if (!canTrain(current, game)) {
            ai.credits += buildSpent; // refund bypasses the cap (own money back)
            buildSpent = 0;
            dequeue();
            return;
        }
        
        if (game.unitCount >= WarTech.MAX_UNITS) return;

        int needed = current.cost - buildSpent;
        int spend = Math.min(ticks, Math.min(ai.credits, needed));
        if (spend > 0) {
            ai.credits -= spend;
            buildSpent += spend;
        }
        if (buildSpent >= current.cost) {
            deploy(current, game);
            dequeue();
            buildSpent = 0;
        }
    }

    private boolean canTrain(UnitType ut, WarTech game) {
        if (ut == null) return false;
        if (!ut.isAvailableTo(Faction.ENEMY)) return false;
        if (ut == UnitType.COLLECTOR) {
            return EnemyAI.findEnemyBuilding(BuildingType.REFINERY, game) != null
                || EnemyAI.findEnemyBuilding(BuildingType.HEAVY_FACTORY, game) != null;
        }
        if (ut.requiredBuilding == null) return false;
        return EnemyAI.findEnemyBuilding(ut.requiredBuilding, game) != null;
    }

    private void deploy(UnitType ut, WarTech game) {
        Building source = game.findProductionBuilding(ut.requiredBuilding, Faction.ENEMY);
        if (ut == UnitType.COLLECTOR && source == null) {
            source = game.findProductionBuilding(BuildingType.HEAVY_FACTORY, Faction.ENEMY);
        }
        if (source == null || game.unitCount >= WarTech.MAX_UNITS) {
            ai.credits += buildSpent; 
            return;
        }
        var u = source.spawnUnit(ut, game.map);
        double spawnX = u.x;
        double spawnY = u.y;
        switch (ut) {
            case COLLECTOR -> {
                if (u.harvester != null) u.harvester.autoCollect = true;
            }
            case MECHANIC -> {
                if (u.repairer != null) u.repairer.autoRepair = true;
            }
            case null, default -> { }
        }
        if (!game.addUnit(u)) {
            ai.credits += buildSpent; // refund bypasses the cap
            return;
        }
        game.spawnEffect(Effect.TYPE_MUZZLE, spawnX, spawnY, AiTuning.SPAWN_EFFECT_SECONDS, AiTuning.SPAWN_EFFECT_SIZE);

        if (source.hasRallyPoint) {
            double rx = source.rallyX + game.simRng.jitter(AiTuning.RALLY_JITTER);
            double ry = source.rallyY + game.simRng.jitter(AiTuning.RALLY_JITTER);
            u.moveTo(rx, ry);
        } else {
            u.moveTo(spawnX + game.simRng.jitter(AiTuning.SPAWN_SPREAD_X),
                    spawnY + AiTuning.SPAWN_OFFSET_Y + game.simRng.range(0.0, AiTuning.SPAWN_SPREAD_Y));
        }
    }

    public void enqueue(UnitType ut) {
        if (queueCount >= WarTech.MAX_UNIT_QUEUE) return;
        queue[queueCount++] = ut;
    }

    private void dequeue() {
        for (int q = 0; q < queueCount - 1; q++) {
            queue[q] = queue[q + 1];
        }
        queue[queueCount - 1] = null;
        queueCount--;
    }

    public int countQueued(UnitType type) {
        int n = 0;
        for (int i = 0; i < queueCount; i++) {
            if (queue[i] == type) n++;
        }
        return n;
    }

    public int countQueuedCombatInfantry() {
        return countQueued(UnitType.RAIDER) + countQueued(UnitType.VANDAL);
    }

    public int countQueuedCombatVehicles() {
        return countQueued(UnitType.REAVER_BUGGY) + countQueued(UnitType.RAVAGER_TANK) + countQueued(UnitType.DREADNOUGHT);
    }
}
