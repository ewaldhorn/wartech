package wartech.render;

// Display strings for unit types, built once at class load to reduce GC churn quite a lot

import wartech.unit.UnitType;

final class UnitLabels {
    final String costText;
    final String prereqHpText;
    final String hoverStatsText;
    final String speedArmorText;
    final String damageRangeText;
    final String prerequisiteText;
    final String hoverLine1;
    final String hoverLine2;
    final String hoverLine3;

    private static final UnitLabels[] CACHE = new UnitLabels[UnitType.values().length];

    static {
        for (UnitType type : UnitType.values()) {
            CACHE[type.ordinal()] = new UnitLabels(type);
        }
    }

    static UnitLabels of(UnitType type) {
        return CACHE[type.ordinal()];
    }

    private UnitLabels(UnitType type) {
        costText = "$" + type.cost;
        prereqHpText = (type.requiredBuilding != null ? type.requiredBuilding.shortName : "-")
            + " HP:" + type.maxHp;
        hoverStatsText = "COST: $" + type.cost + "  HP: " + type.maxHp;
        speedArmorText = "Speed: " + (int) type.speed + " px/s  Armor: " + type.armor;
        damageRangeText = "Damage: " + type.attackDamage + "  Range: " + (int) type.attackRange;
        prerequisiteText = "Prerequisite: "
            + (type.requiredBuilding != null ? type.requiredBuilding.displayName : "None");

        String line1 = damageRangeText;
        String line2 = speedArmorText;
        String line3 = prerequisiteText;

        // dirty manual overrides for now, this whole approach needs a redesign
        switch (type) {
            case MECHANIC -> {
                line1 = "Repairs vehicles/structures";
                line2 = "Cost: 1 credit / 10 HP";
                line3 = "Limit 2 per Repair Facility";
            }
            case MEDIC -> {
                line1 = "Heals nearby troops (+5 HP)";
                line2 = "Heal pulse: every 30 ticks";
                line3 = "Light sidearm self-defense";
            }
            default -> { }
        }
        hoverLine1 = line1;
        hoverLine2 = line2;
        hoverLine3 = line3;
    }
}
