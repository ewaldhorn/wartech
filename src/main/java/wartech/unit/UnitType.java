package wartech.unit;

// This is so messy. I initially started out with just a Collector and then started adding more 
// and more units and this grew into a Little Shop of Horrors situation.
// The plan is to move this into config files that can be read at runtime, with properties that are
// enabled or disabled so one can have JSON or YAML files for this. 

import wartech.building.BuildingType;
import wartech.core.Faction;
import wartech.map.GameMap;

public enum UnitType {
    COLLECTOR("Collector", "COLLECTOR", BuildingType.REFINERY, 400, 800,
              Chassis.vehicle(101.0, 37.0, 2.0), Weapon.none(), 6, 2, null),
    // Vanguard roster
    SCOUT_TRIKE("Scout Trike", "TRIKE", BuildingType.LIGHT_FACTORY, 200, 220,
                Chassis.vehicle(280.0, 24.0, 1.5), Weapon.of(256.0, 12, 0.5), 10, 1, Faction.PLAYER),
    COMBAT_TANK("Combat Tank", "COMBAT TANK", BuildingType.HEAVY_FACTORY, 400, 550,
                Chassis.vehicle(173.0, 32.0, 2.0, Turret.SINGLE), Weapon.of(293.0, 36, 1.1), 8, 3, Faction.PLAYER),
    SIEGE_TANK("Battle Tank", "BATTLE TANK", BuildingType.HEAVY_FACTORY, 700, 750,
               Chassis.vehicle(112.0, 37.0, 3.0, Turret.TWIN), Weapon.of(453.0, 80, 2.2).withRocket(), 10, 4, Faction.PLAYER),
    TROOPER_RIFLE("Rifle Trooper", "RIFLE TRP", BuildingType.BARRACKS, 80, 90,
                  Chassis.infantry(133.0, 16.0), Weapon.of(192.0, 8, 0.4), 6, 0, Faction.PLAYER),
    TROOPER_MISSILE("Missile Trooper", "MSL TRP", BuildingType.BARRACKS, 150, 110,
                    Chassis.infantry(112.0, 16.0), Weapon.of(320.0, 28, 1.4).withRocket(), 8, 1, Faction.PLAYER),
    // Crimson Syndicate roster (mirrors Vanguard roles with raider profiles)
    REAVER_BUGGY("Reaver Buggy", "REAVER", BuildingType.LIGHT_FACTORY, 180, 170,
                 Chassis.vehicle(305.0, 22.0, 1.5), Weapon.of(230.0, 10, 0.4), 12, 0, Faction.ENEMY),
    RAVAGER_TANK("Ravager Tank", "RAVAGER", BuildingType.HEAVY_FACTORY, 380, 470,
                 Chassis.vehicle(185.0, 30.0, 2.0, Turret.SINGLE), Weapon.of(275.0, 30, 0.9), 8, 1, Faction.ENEMY),
    DREADNOUGHT("Dreadnought", "DREADNOUGHT", BuildingType.HEAVY_FACTORY, 650, 640,
                Chassis.vehicle(115.0, 36.0, 3.0, Turret.TWIN),
                Weapon.splash(420.0, 60, 2.0, 48.0).withRocket(), 9, 2, Faction.ENEMY),
    RAIDER("Syndicate Raider", "RAIDER", BuildingType.BARRACKS, 70, 75,
           Chassis.infantry(150.0, 15.0), Weapon.of(180.0, 7, 0.35), 6, 0, Faction.ENEMY),
    VANDAL("Syndicate Vandal", "VANDAL", BuildingType.BARRACKS, 140, 95,
           Chassis.infantry(115.0, 15.0), Weapon.of(340.0, 26, 1.5).withRocket(), 8, 0, Faction.ENEMY),
    // Shared Combat Support
    MECHANIC("Combat Mechanic", "MECHANIC", BuildingType.REPAIR_FACILITY, 250, 100,
             Chassis.infantry(120.0, 16.0), Weapon.supportReach(50.0), 6, 0, null),
    MEDIC("Combat Medic", "MEDIC", BuildingType.BARRACKS, 160, 95,
          Chassis.infantry(130.0, 16.0), Weapon.of(140.0, 4, 0.6), 6, 0, null),
    // Neutral wildlife / precursor automata
    SHARD_SKITTERER("Shard Skitterer", "SKITTERER", null, 0, 65,
                    Chassis.wildlife(267.0, 19.0, 1.0), Weapon.of(53.0, 12, 0.5), 6, 0, null),
    SCAVENGER_DRONE("Scavenger Drone", "DRONE", null, 0, 180,
                    Chassis.wildlife(227.0, 21.0, 1.5), Weapon.of(160.0, 18, 0.8), 7, 1, null),
    PRECURSOR_WALKER("Precursor Walker", "WALKER", null, 0, 700,
                     Chassis.wildlife(107.0, 35.0, 3.0), Weapon.of(333.0, 55, 1.7), 8, 3, null);

    public final String displayName;
    public final String displayNameUpper;
    public final String shortName;
    public final BuildingType requiredBuilding;
    public final int cost;
    public final int maxHp;
    public final double speed;
    public final double attackRange;
    public final int attackDamage;
    public final double attackCooldown;
    public final double radius;
    public final int sightRadiusTiles;
    public final double sightRange;
    public final int armor;
    public final double splashRadius;
    public final Faction homeFaction;
    private final Category category;
    public final double mass;
    public final Turret turret;
    public final boolean rocket;

    UnitType(String displayName, String shortName, BuildingType requiredBuilding,
             int cost, int maxHp, Chassis chassis, Weapon weapon,
             int sightRadiusTiles, int armor, Faction homeFaction) {
        this.displayName = displayName;
        this.displayNameUpper = displayName.toUpperCase();
        this.shortName = shortName;
        this.requiredBuilding = requiredBuilding;
        this.cost = cost;
        this.maxHp = maxHp;
        this.speed = chassis.speed();
        this.category = chassis.category();
        this.mass = chassis.mass();
        this.turret = chassis.turret();
        this.radius = chassis.radius();
        this.attackRange = weapon.range();
        this.attackDamage = weapon.damage();
        this.attackCooldown = weapon.cooldown();
        this.splashRadius = weapon.splashRadius();
        this.rocket = weapon.rocket();
        this.sightRadiusTiles = sightRadiusTiles;
        this.sightRange = sightRadiusTiles * GameMap.TILE_SIZE;
        this.armor = armor;
        this.homeFaction = homeFaction;
    }

    public boolean isAvailableTo(Faction faction) {
        return homeFaction == null || homeFaction == faction;
    }

    public boolean isMilitary() {
        if (this == COLLECTOR || this == MECHANIC || this == MEDIC) return false;
        return isVehicle() || isInfantry();
    }

    public boolean isVehicle() {
        return category == Category.VEHICLE;
    }

    public boolean isInfantry() {
        return category == Category.INFANTRY;
    }

    public boolean leavesWreck() {
        return category == Category.VEHICLE && this != COLLECTOR;
    }

    private enum Category { VEHICLE, INFANTRY, WILDLIFE }

    public enum Turret { NONE, SINGLE, TWIN }

    private record Chassis(Category category, double speed, double radius, double mass, Turret turret) {
        static Chassis vehicle(double speed, double radius, double mass) {
            return new Chassis(Category.VEHICLE, speed, radius, mass, Turret.NONE);
        }

        static Chassis vehicle(double speed, double radius, double mass, Turret turret) {
            return new Chassis(Category.VEHICLE, speed, radius, mass, turret);
        }

        static Chassis infantry(double speed, double radius) {
            return new Chassis(Category.INFANTRY, speed, radius, 1.0, Turret.NONE);
        }

        static Chassis wildlife(double speed, double radius, double mass) {
            return new Chassis(Category.WILDLIFE, speed, radius, mass, Turret.NONE);
        }
    }

    private record Weapon(double range, int damage, double cooldown, double splashRadius, boolean rocket) {
        static Weapon none() {
            return new Weapon(0.0, 0, 0.0, 0.0, false);
        }

        static Weapon supportReach(double range) {
            return new Weapon(range, 0, 0.0, 0.0, false);
        }

        static Weapon of(double range, int damage, double cooldown) {
            return new Weapon(range, damage, cooldown, 0.0, false);
        }

        static Weapon splash(double range, int damage, double cooldown, double splashRadius) {
            return new Weapon(range, damage, cooldown, splashRadius, false);
        }

        Weapon withRocket() {
            return new Weapon(range, damage, cooldown, splashRadius, true);
        }
    }
}
