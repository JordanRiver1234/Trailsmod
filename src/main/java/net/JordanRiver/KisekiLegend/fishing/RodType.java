package net.JordanRiver.KisekiLegend.fishing;

import java.util.Set;

public enum RodType {
    PROGRESS_ROD("Progress Rod", 0.0f, 1.0f,
            Set.of("earthworm", "polychaete", "shrimplet", "dace", "yamany")),

    MARINE_STAR_ROD("Marine Star", 0.05f, 1.1f,
            Set.of("crab", "kasago", "polychaete", "shrimplet")),

    PISCES_HEART("Pisces Heart", 0.25f, 1.2f,
            Set.of( "red_flies", "river_bug", "roe")),

    BAMBOO_FISHING_ROD("Bamboo Rod", 0.30f, 1.3f,
            Set.of( "crab", "dumplings", "frog", "river_snail")),

    METAL_TRIDENT_ROD("Metal Trident Rod", 0.40f, 1.4f,
            Set.of( "carp", "salmon", "sea_bass", "trout")),

    LAKELORD_II("Lakelord II", 0.65f, 1.5f,
            Set.of( "carp", "eel", "frog", "trout", "yamany")),
    AQUA_MASTER("Aqua Master", 1f, 2f,
            Set.of("dumplings", "earthworm", "frog", "polychaete", "red_flies" ,
                    "river_bug" , "river_snail", "roe", "shrimplet", "carp", "crab", "dace", "eel", "kasago", "salmon", "sea_bass", "trout", "yamany"));


    private final String displayName;
    private final float timeBonus;
    private final float catchRateMultiplier; // Bonus to catch rate
    private final Set<String> affinityBaits; // Baits this rod has affinity with

    RodType(String displayName, float timeBonus, float catchRateMultiplier, Set<String> affinityBaits) {
        this.displayName = displayName;
        this.timeBonus = timeBonus;
        this.catchRateMultiplier = catchRateMultiplier;
        this.affinityBaits = affinityBaits;
    }

    public String getDisplayName() { return displayName; }
    public float getTimeBonus() { return timeBonus; }
    public Set<String> getAffinityBaits() { return affinityBaits; }

    public float getCatchRateMultiplier() { return catchRateMultiplier; }

    public boolean hasAffinityWith(String bait) {
        return affinityBaits.contains(bait);
    }

}