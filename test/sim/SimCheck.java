import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import wartech.building.BuildingType;
import wartech.combat.Effect;
import wartech.core.Faction;
import wartech.core.MatchState;
import wartech.core.WarTech;
import wartech.map.GameMap;
import wartech.unit.Stance;
import wartech.unit.Unit;
import wartech.unit.UnitType;

/**
 * SimCheck — headless deterministic regression checks for the WarTech sim.
 *
 * <p>Covers two port bugs: units wedging on open map edges (frozen chassis
 * heading in the steering fallbacks) and the enemy AI laying extra
 * foundations while its Construction Yard is still being rebuilt, plus the
 * unbounded foundation pipeline behind both. It also pins the declared unit
 * traits (rocket, turret mount, wreck) so a dropped flag cannot ship silently,
 * and the AI's turbine-on-deficit build rule (see testAiTurbineDemand).
 *
 * <p>No framework: plain javac/java against target/classes (see README).
 * Exit 0 passes, exit 1 fails; failures print which invariant broke.
 */
public class SimCheck {
    private static final double DT = 1.0 / 60.0;
    private static int failures = 0;

    private static WarTech game() throws Exception {
        Field f = WarTech.class.getDeclaredField("instance");
        f.setAccessible(true);
        return (WarTech) f.get(null);
    }

    private static void freshMatch() {
        WarTech.wasm_init(0);
    }

    private static WarTech liveGame() throws Exception {
        WarTech g = game();
        g.showSetupScreen = false;
        g.matchStarted = true;
        return g;
    }

    /** Steps the sim, keeping the player base alive so long runs never freeze on defeat. */
    private static void step(WarTech g, int frames) {
        for (int f = 0; f < frames; f++) {
            WarTech.wasm_update(DT);
            if (f % 60 == 0) healPlayer(g);
        }
        healPlayer(g);
    }

    private static void healPlayer(WarTech g) {
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (b.faction == Faction.PLAYER && !b.isDestroyed()) b.hp = b.maxHp;
        }
        for (int i = 0; i < g.unitCount; i++) {
            var u = g.units[i];
            if (u.faction == Faction.PLAYER && !u.isDestroyed()) u.hp = u.maxHp;
        }
    }

    private static void killNonPlayerUnits(WarTech g) {
        for (int i = 0; i < g.unitCount; i++) {
            var u = g.units[i];
            if (u.faction != Faction.PLAYER && !u.isDestroyed()) {
                u.hp = 0;
                u.active = false;
            }
        }
    }

    private static Unit playerTrike(WarTech g) {
        for (int i = 0; i < g.unitCount; i++) {
            var u = g.units[i];
            if (u.faction == Faction.PLAYER && u.type == UnitType.SCOUT_TRIKE && !u.isDestroyed()) return u;
        }
        return null;
    }

    private static boolean enemyYardOnline(WarTech g) {
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (!b.isDestroyed() && !b.constructing && b.faction == Faction.ENEMY && b.type == BuildingType.CON_YARD) {
                return true;
            }
        }
        return false;
    }

    private static int enemyFoundations(WarTech g) {
        int n = 0;
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (!b.isDestroyed() && b.constructing && b.faction == Faction.ENEMY) n++;
        }
        return n;
    }

    private static void check(boolean ok, String label) {
        System.out.println((ok ? "PASS " : "FAIL ") + label);
        if (!ok) failures++;
    }

    /**
     * Unit traits are declared per type (rocket, turret mount, wreck) rather than
     * as hardcoded lists in the call sites that consume them. A dropped flag is
     * silent at runtime: the unit just fires the wrong projectile or loses its
     * barrels. These expectations pin the intended roster so that mistake fails
     * here instead of shipping.
     */
    private static void testUnitTraits() {
        check(EnumSet.of(UnitType.TROOPER_MISSILE, UnitType.SIEGE_TANK,
                         UnitType.VANDAL, UnitType.DREADNOUGHT)
                  .equals(typesMatching(t -> t.rocket)),
              "unit-traits rocket projectiles");

        check(EnumSet.of(UnitType.COMBAT_TANK, UnitType.SIEGE_TANK,
                         UnitType.RAVAGER_TANK, UnitType.DREADNOUGHT)
                  .equals(typesMatching(t -> t.turret != UnitType.Turret.NONE)),
              "unit-traits turret mounts");

        check(EnumSet.of(UnitType.SIEGE_TANK, UnitType.DREADNOUGHT)
                  .equals(typesMatching(t -> t.turret == UnitType.Turret.TWIN)),
              "unit-traits twin barrels");

        check(EnumSet.of(UnitType.SCOUT_TRIKE, UnitType.COMBAT_TANK,
                         UnitType.SIEGE_TANK, UnitType.REAVER_BUGGY,
                         UnitType.RAVAGER_TANK, UnitType.DREADNOUGHT)
                  .equals(typesMatching(UnitType::leavesWreck)),
              "unit-traits wrecks are vehicles but not the Collector");

        // Coherence: these combinations make no sense if a new type is mislabelled.
        boolean coherent = true;
        for (UnitType t : UnitType.values()) {
            if (t.turret != UnitType.Turret.NONE && !t.isVehicle()) coherent = false;
            if (t.rocket && t.attackDamage <= 0) coherent = false;
        }
        check(coherent, "unit-traits only armed vehicles carry a turret or rocket");
    }

    private static Set<UnitType> typesMatching(Predicate<UnitType> p) {
        Set<UnitType> out = EnumSet.noneOf(UnitType.class);
        for (UnitType t : UnitType.values()) {
            if (p.test(t)) out.add(t);
        }
        return out;
    }

    /** A unit ordered out of a map corner must actually leave, not jitter in place. */
    private static void testCornerEscape() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        killNonPlayerUnits(g);
        step(g, 10);

        Unit trike = playerTrike(g);
        trike.x = 4050.0;
        trike.y = 4050.0;
        trike.targetX = 4050.0;
        trike.targetY = 4050.0;
        trike.moving = false;
        trike.angle = 0.8; // nose into the corner, like a fresh arrival
        double startX = trike.x;
        double startY = trike.y;

        for (int i = 0; i < g.unitCount; i++) g.units[i].selected = false;
        trike.selected = true;
        g.selectedUnit = trike;
        g.selectedBuilding = null;
        g.issueRightClickOrder(10 * GameMap.TILE_SIZE, (g.map.height - 10) * GameMap.TILE_SIZE);

        for (int s = 0; s < 30; s++) {
            step(g, 60);
            killNonPlayerUnits(g);
            g.enemyAI.attackTimer = 9999; // stay sterile: no waves mid-test
        }
        double travelled = Math.hypot(trike.x - startX, trike.y - startY);
        check(travelled > 500.0, "corner-escape travelled=" + (int) travelled + "px (want >500)");
        check(g.matchState == MatchState.IN_PROGRESS, "corner-escape sim still live");
    }

    /** While the enemy yard is down, only a yard foundation may be laid. */
    private static void testAiYardGate() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        step(g, 5 * 60);

        // Raze the yard and any foundations in flight, then watch the rebuild.
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (b.faction == Faction.ENEMY && !b.isDestroyed()
                    && (b.type == BuildingType.CON_YARD || b.constructing)) {
                b.takeDamage(999999999, Faction.PLAYER);
            }
        }
        step(g, 5);

        boolean violation = false;
        String offender = "";
        for (int s = 0; s < 60; s++) {
            step(g, 30);
            if (!enemyYardOnline(g)) {
                for (int i = 0; i < g.buildingCount; i++) {
                    var b = g.buildings[i];
                    if (!b.isDestroyed() && b.constructing && b.faction == Faction.ENEMY
                            && b.type != BuildingType.CON_YARD) {
                        violation = true;
                        offender = b.type.displayName;
                    }
                }
            }
            if (g.matchState != MatchState.IN_PROGRESS) break;
        }
        check(!violation, "ai-yard-gate no non-yard foundations while yard down"
                + (violation ? " (laid " + offender + ")" : ""));

        // The AI must still come back: a yard stands or is being paid off.
        boolean yardBack = enemyYardOnline(g);
        if (!yardBack) {
            for (int i = 0; i < g.buildingCount; i++) {
                var b = g.buildings[i];
                if (!b.isDestroyed() && b.faction == Faction.ENEMY && b.type == BuildingType.CON_YARD) yardBack = true;
            }
        }
        check(yardBack, "ai-yard-gate yard rebuild underway");
    }

    /** At most one AI foundation in flight while the yard stands; the AI must still progress. */
    private static void testAiPipelineCap() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        int maxSeen = 0;
        boolean creditsNegative = false;
        for (int s = 0; s < 150; s++) {
            step(g, 30);
            if (g.enemyAI.credits < 0) creditsNegative = true;
            if (enemyYardOnline(g)) maxSeen = Math.max(maxSeen, enemyFoundations(g));
            if (g.matchState != MatchState.IN_PROGRESS) break;
        }
        check(maxSeen <= 1, "ai-pipeline max foundations with yard up = " + maxSeen + " (want <=1)");
        check(!creditsNegative, "ai-pipeline credits never negative");

        int completed = 0;
        Set<String> types = new HashSet<>();
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (!b.isDestroyed() && !b.constructing && b.faction == Faction.ENEMY) {
                completed++;
                types.add(b.type.shortName);
            }
        }
        check(completed >= 4, "ai-pipeline completed buildings = " + completed + " " + types + " (want >=4)");
        check(g.matchState == MatchState.IN_PROGRESS, "ai-pipeline sim still live");
    }

    /** Counts enemy structures of one type; foundations included unless completedOnly. */
    private static int enemyBuildingCount(WarTech g, BuildingType type, boolean completedOnly) {
        int n = 0;
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (b.isDestroyed() || b.faction != Faction.ENEMY || b.type != type) continue;
            if (!completedOnly || !b.constructing) n++;
        }
        return n;
    }

    /** Counts enemy foundations of one type: laid, not yet paid off. */
    private static int enemyFoundationsOf(WarTech g, BuildingType type) {
        int n = 0;
        for (int i = 0; i < g.buildingCount; i++) {
            var b = g.buildings[i];
            if (!b.isDestroyed() && b.constructing && b.faction == Faction.ENEMY && b.type == type) n++;
        }
        return n;
    }

    /**
     * Turbines are built on a power deficit, never on a census count.
     *
     * <p>EnemyBuildOrder lays a turbine only while net completed power is under
     * the difficulty threshold, adding the power of turbine foundations already
     * in flight. Counting completed turbines instead would strand the AI in a
     * brownout after a turbine dies; counting nothing would stack duplicate
     * foundations. So each time a turbine foundation appears, the freshly
     * refreshed net power that the build rule compared against must be below
     * the threshold, and no second turbine foundation may be in flight behind
     * it. Radar Outposts are pinned out entirely: nothing the AI builds needs
     * one and hasRadar() has no AI consumer, so laying one is wasted budget.
     */
    private static void testAiTurbineDemand() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        int threshold = g.enemyAI.tuning().powerThreshold();

        int prevFoundations = 0;
        int maxFoundations = 0;
        int laid = 0;
        boolean allUnderDeficit = true;
        boolean radarLaid = false;

        for (int f = 0; f < 240 * 60; f++) {
            WarTech.wasm_update(DT);
            healPlayer(g);

            int foundations = enemyFoundationsOf(g, BuildingType.WIND_TURBINE);
            if (enemyBuildingCount(g, BuildingType.RADAR, false) > 0) radarLaid = true;
            maxFoundations = Math.max(maxFoundations, foundations);

            if (foundations > prevFoundations) {
                laid++;
                // powerNet is refreshed in the same think that laid the
                // foundation and foundations project no power, so this is the
                // exact value the rule tested against.
                if (g.enemyAI.powerNet >= threshold) {
                    allUnderDeficit = false;
                    System.out.println("  turbine laid at net power " + g.enemyAI.powerNet
                            + " (threshold " + threshold + ")");
                }
            }
            prevFoundations = foundations;
            if (g.matchState != MatchState.IN_PROGRESS) break;
        }

        int completedTurbines = enemyBuildingCount(g, BuildingType.WIND_TURBINE, true);
        check(allUnderDeficit, "ai-turbine-demand every turbine laid on a power deficit");
        check(maxFoundations <= 1, "ai-turbine-demand max turbine foundations in flight = " + maxFoundations);
        check(laid >= 2, "ai-turbine-demand turbines laid = " + laid + " (want >=2)");
        check(completedTurbines >= 2, "ai-turbine-demand completed turbines = " + completedTurbines + " (want >=2)");
        check(!radarLaid, "ai-turbine-demand AI never lays a Radar Outpost");
        check(g.matchState == MatchState.IN_PROGRESS, "ai-turbine-demand sim still live");
    }

    /**
     * Newly created military units default to GUARD stance (holding position
     * and engaging in range) instead of the passive DEFAULT. Support and
     * economic types (Collector, Mechanic, Medic) plus neutral wildlife keep
     * DEFAULT so their automation and provocation rules are untouched.
     */
    private static void testGuardDefault() throws Exception {
        Set<UnitType> military = EnumSet.of(UnitType.SCOUT_TRIKE, UnitType.COMBAT_TANK,
                UnitType.SIEGE_TANK, UnitType.TROOPER_RIFLE, UnitType.TROOPER_MISSILE,
                UnitType.REAVER_BUGGY, UnitType.RAVAGER_TANK, UnitType.DREADNOUGHT,
                UnitType.RAIDER, UnitType.VANDAL);
        check(military.equals(typesMatching(UnitType::isMilitary)),
                "guard-default military set is the ten faction combat types");

        boolean stancesOk = true;
        for (UnitType t : UnitType.values()) {
            Unit u = new Unit(t, Faction.PLAYER, 100.0, 200.0);
            Stance want = military.contains(t) ? Stance.GUARD : Stance.DEFAULT;
            if (u.stance != want) stancesOk = false;
            if (military.contains(t) && (u.guardAnchorX != 100.0 || u.guardAnchorY != 200.0)) stancesOk = false;
        }
        check(stancesOk, "guard-default fresh military units spawn in GUARD at their anchor");

        // A move order reposts a guard unit instead of boomeranging: without
        // this, factory rally points and retreats would walk back to the
        // spawn anchor on arrival.
        Unit tank = new Unit(UnitType.COMBAT_TANK, Faction.PLAYER, 100.0, 200.0);
        tank.moveTo(500.0, 600.0);
        check(tank.guardAnchorX == 500.0 && tank.guardAnchorY == 600.0,
                "guard-default moveTo reposts the guard anchor");

        // The opening force holds its ground too.
        freshMatch();
        WarTech g = liveGame();
        Unit trike = playerTrike(g);
        check(trike != null && trike.stance == Stance.GUARD,
                "guard-default starting trike holds in GUARD");
    }

    /** Kills every unit on the field, any faction: a sterile arena with buildings intact. */
    private static void killAllUnits(WarTech g) {
        for (int i = 0; i < g.unitCount; i++) {
            var u = g.units[i];
            if (!u.isDestroyed()) {
                u.hp = 0;
                u.active = false;
            }
        }
    }

    /** Raw frames without healing: step() would cure the test patient. Safe while both bases stand. */
    private static void stepRaw(int frames) {
        for (int f = 0; f < frames; f++) {
            WarTech.wasm_update(DT);
        }
    }

    /**
     * Medics heal instead of picking fights: an unprovoked medic never
     * auto-acquires, even with an enemy in range. One that takes a hit
     * returns fire in range, but never chases — an out-of-range target is
     * dropped so the medic goes back to its patients.
     */
    private static void testMedicHoldsFire() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        killAllUnits(g);

        double bx = 10 * GameMap.TILE_SIZE;
        double by = (g.map.height - 10) * GameMap.TILE_SIZE;
        Unit medic = new Unit(UnitType.MEDIC, Faction.PLAYER, bx, by);
        Unit troop = new Unit(UnitType.TROOPER_RIFLE, Faction.PLAYER, bx + 200.0, by + 80.0);
        troop.hp = 50; // injured: triggers the medic's heal-seek
        troop.reloadCooldown = 1e9; // frozen gun: the witness must not kill the raider
        // Frozen gun: the raider threatens without firing, so the medic stays unprovoked.
        Unit raider = new Unit(UnitType.RAIDER, Faction.ENEMY, bx + 100.0, by);
        raider.reloadCooldown = 1e9;
        g.addUnit(medic);
        g.addUnit(troop);
        g.addUnit(raider);

        stepRaw(180); // covers every acquire slice several times over
        check(medic.attackTargetUnit == null && medic.attackTargetBuilding == null,
                "medic-holds-fire unprovoked medic ignores enemy in range");
        double medicFromPost = Math.hypot(medic.x - bx, medic.y - by);
        check(medicFromPost > 150.0,
                "medic-holds-fire medic goes to its patient instead (" + (int) medicFromPost + "px from post)");

        // Provoked: a medic that takes a hit returns fire in range...
        freshMatch();
        g = liveGame();
        killAllUnits(g);
        medic = new Unit(UnitType.MEDIC, Faction.PLAYER, bx, by);
        raider = new Unit(UnitType.RAIDER, Faction.ENEMY, bx + 100.0, by);
        raider.reloadCooldown = 1e9;
        g.addUnit(medic);
        g.addUnit(raider);
        medic.threatTimer = 10.0;
        stepRaw(120);
        check(medic.attackTargetUnit == raider, "medic-holds-fire provoked medic returns fire");

        // ...but never chases: a target out of range is dropped, not pursued.
        raider.x = bx + 1000.0;
        raider.y = by;
        raider.targetX = raider.x;
        raider.targetY = raider.y;
        stepRaw(5);
        check(medic.attackTargetUnit == null && !medic.moving,
                "medic-holds-fire medic drops out-of-range target instead of chasing");

        // End to end: live fire provokes. The raider's gun is hot here, so a
        // real hit must mark the medic before it returns fire.
        freshMatch();
        g = liveGame();
        killAllUnits(g);
        medic = new Unit(UnitType.MEDIC, Faction.PLAYER, bx, by);
        raider = new Unit(UnitType.RAIDER, Faction.ENEMY, bx + 100.0, by);
        g.addUnit(medic);
        g.addUnit(raider);
        stepRaw(40);
        check(medic.threatTimer > 0.0, "medic-holds-fire taking a hit marks the medic provoked");
        check(medic.attackTargetUnit == raider, "medic-holds-fire hit medic returns live fire");
    }

    /**
     * Guards engage hostile fauna: a Shard Skitterer is always dangerous, even
     * with an expired threatTimer and no current target, so a guard whose post
     * it walks past must pick it up. A passive Scavenger Drone on the same walk
     * stays ignored: drones only fight back when provoked.
     */
    private static void testGuardEngagesSkitterers() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        killAllUnits(g);
        stepRaw(5);

        double bx = 20 * GameMap.TILE_SIZE;
        double by = (g.map.height - 20) * GameMap.TILE_SIZE;
        Unit guard = new Unit(UnitType.TROOPER_MISSILE, Faction.PLAYER, bx, by);
        g.addUnit(guard);
        check(guard.stance == Stance.GUARD, "guard-skitterer missile trooper posts in GUARD");

        // Walk-past at 150px lateral offset: inside guard range (320) while
        // never entering the skitterer's own 53px acquisition reach, so the
        // skitterer never self-aggros and the guard must engage first.
        Unit sk = new Unit(UnitType.SHARD_SKITTERER, Faction.NEUTRAL, bx - 200.0, by + 150.0);
        sk.threatTimer = 0.0;
        sk.moveTo(bx + 200.0, by + 150.0);
        g.addUnit(sk);
        check(g.isDangerousTarget(Faction.PLAYER, sk),
                "guard-skitterer unprovoked skitterer reads dangerous");

        boolean engaged = false;
        double minDist = Double.MAX_VALUE;
        for (int f = 0; f < 300; f++) {
            WarTech.wasm_update(DT);
            double d = Math.hypot(sk.x - guard.x, sk.y - guard.y);
            if (d < minDist) minDist = d;
            if (guard.attackTargetUnit == sk) engaged = true;
        }
        check(minDist > UnitType.SHARD_SKITTERER.attackRange,
                "guard-skitterer walk stayed outside skitterer aggro (" + (int) minDist + "px)");
        check(engaged, "guard-skitterer guard engages skitterer walking past its post");

        // Control: a passive drone on the same walk is not a threat.
        freshMatch();
        g = liveGame();
        killAllUnits(g);
        stepRaw(5);
        guard = new Unit(UnitType.TROOPER_MISSILE, Faction.PLAYER, bx, by);
        g.addUnit(guard);
        Unit drone = new Unit(UnitType.SCAVENGER_DRONE, Faction.NEUTRAL, bx - 200.0, by + 150.0);
        drone.threatTimer = 0.0;
        drone.moveTo(bx + 200.0, by + 150.0);
        g.addUnit(drone);
        check(!g.isDangerousTarget(Faction.PLAYER, drone),
                "guard-skitterer unprovoked drone reads harmless");
        boolean droneEngaged = false;
        for (int f = 0; f < 300; f++) {
            WarTech.wasm_update(DT);
            if (guard.attackTargetUnit == drone) droneEngaged = true;
        }
        check(!droneEngaged, "guard-skitterer guard holds fire as passive drone walks past");
    }

    private static boolean hasOrderPing(WarTech g, byte type, double x, double y) {
        for (int i = 0; i < g.effectCount; i++) {
            var fx = g.effects[i];
            if (fx.active && fx.type == type && Math.hypot(fx.x - x, fx.y - y) < 4.0) return true;
        }
        return false;
    }

    /**
     * Right-click orders leave a visible ping: a red marker on the target for
     * attack orders, a green marker at the destination for move orders, so the
     * player sees the command was accepted.
     */
    private static void testOrderMarkers() throws Exception {
        freshMatch();
        WarTech g = liveGame();
        killAllUnits(g);
        stepRaw(5);

        double bx = 20 * GameMap.TILE_SIZE;
        double by = (g.map.height - 20) * GameMap.TILE_SIZE;
        Unit troop = new Unit(UnitType.TROOPER_RIFLE, Faction.PLAYER, bx, by);
        troop.selected = true;
        g.addUnit(troop);
        Unit raider = new Unit(UnitType.RAIDER, Faction.ENEMY, bx + 100.0, by);
        raider.reloadCooldown = 1e9; // frozen gun: the witness must not fight back
        g.addUnit(raider);
        stepRaw(2); // reveal the raider's tile so the click can target it

        g.issueRightClickOrder(raider.x, raider.y);
        check(troop.attackTargetUnit == raider, "order-marker attack order lands on target");
        check(hasOrderPing(g, Effect.TYPE_ORDER_ATTACK, raider.x, raider.y),
                "order-marker attack ping spawns on the target");

        double wx = bx + 300.0;
        double wy = by + 100.0;
        g.issueRightClickOrder(wx, wy);
        check(troop.attackTargetUnit == null && troop.moving,
                "order-marker move order supersedes the attack");
        check(hasOrderPing(g, Effect.TYPE_ORDER_MOVE, wx, wy),
                "order-marker move ping spawns at the waypoint");
    }

    public static void main(String[] args) throws Exception {
        testOrderMarkers();
        testGuardEngagesSkitterers();
        testMedicHoldsFire();
        testGuardDefault();
        testCornerEscape();
        testAiYardGate();
        testAiPipelineCap();
        testAiTurbineDemand();
        testUnitTraits();
        System.out.println(failures == 0 ? "SIMCHECK PASS" : "SIMCHECK FAIL (" + failures + ")");
        if (failures != 0) System.exit(1);
    }
}
