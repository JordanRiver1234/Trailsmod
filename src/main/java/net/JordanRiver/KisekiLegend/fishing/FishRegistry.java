package net.JordanRiver.KisekiLegend.fishing;

import java.util.*;

public class FishRegistry {
    private static final Map<String, FishData> FISH_DATA = new HashMap<>();
    private static final Map<String, List<FishData>> BAIT_TO_FISH = new HashMap<>();

    static {
        registerFishData();
        buildBaitMappings();
    }

    private static void registerFishData() {
        // Corrected fish data based on reference - weights represent catch chance with specific bait
        register(new FishData("crab", FishRarity.COMMON,
                Map.of("earthworm", 60, "polychaete", 40, "shrimplet", 50),
                90, 0.8f, 0.8f, true, "earth", "water"));

        register(new FishData("dace", FishRarity.COMMON,
                Map.of("river_bug", 30, "roe", 30, "red_flies", 90),
                90, 1.0f, 0.85f, true, "water"));

        register(new FishData("gold_angelfish", FishRarity.COMMON,
                Map.of("polychaete", 60, "shrimplet", 30),
                90, 1.1f, 0.8f, false, "water", "space"));

        register(new FishData("liberl_carp", FishRarity.UNCOMMON,
                Map.of("dumplings", 70, "red_flies", 40, "river_bug", 40),
                100, 1.3f, 0.75f, false, "water", "earth"));

        register(new FishData("kasago", FishRarity.UNCOMMON,
                Map.of("polychaete", 70, "shrimplet", 70),
                90, 1.4f, 0.7f, true, "fire", "earth"));

        register(new FishData("valleria_bass", FishRarity.RARE,
                Map.of("frog", 90, "dace", 60, "yamany", 90),
                115, 1.6f, 0.65f, false, "water", "earth"));

        register(new FishData("rockeater", FishRarity.RARE,
                Map.of("earthworm", 40, "frog", 40, "river_bug", 50, "river_snail", 60, "roe", 40, "crab", 60, "dace", 60, "yamany", 50),
                115, 1.8f, 0.6f, false, "earth", "time"));

        register(new FishData("great_blackfish", FishRarity.RARE,
                Map.of("polychaete", 50, "shrimplet", 50, "dace", 60),
                115, 1.5f, 0.65f, false, "water", "wind"));

        register(new FishData("carp", FishRarity.UNCOMMON,
                Map.of("earthworm", 70, "dumplings", 40, "river_snail", 80, "crab", 60),
                100, 1.25f, 0.75f, true, "water", "earth"));

        register(new FishData("octopus", FishRarity.RARE,
                Map.of("shrimplet", 50, "kasago", 50, "crab", 60),
                115, 2.0f, 0.5f, false, "water", "mirage"));

        register(new FishData("rainbow_trout", FishRarity.RARE,
                Map.of("earthworm", 60, "river_bug", 60, "roe", 70),
                133, 1.7f, 0.55f, false, "water", "space", "time", "wind", "earth", "mirage", "fire"));

        register(new FishData("trout", FishRarity.RARE,
                Map.of("earthworm", 60, "roe", 40, "red_flies", 30, "shrimplet", 30),
                133, 1.6f, 0.6f, true, "water", "wind"));

        register(new FishData("eel", FishRarity.RARE,
                Map.of("earthworm", 80),
                133, 1.9f, 0.45f, true, "earth", "time"));

        register(new FishData("salmon", FishRarity.COMMON,
                Map.of("river_bug", 60, "roe", 40, "dace", 40),
                115, 1.5f, 0.6f, true, "water", "fire"));

        register(new FishData("claudine", FishRarity.RARE,
                Map.of("dace", 60, "kasago", 80, "crab", 60, "polychaete", 30, "shrimplet", 30),
                160, 2.2f, 0.4f, false, "mirage", "space"));

        register(new FishData("snakehead", FishRarity.RARE,
                Map.of("frog", 40, "dace", 60, "yamany", 40, "trout", 80, "eel", 60, "carp", 70, "salmon", 70),
                160, 2.5f, 0.35f, false, "earth", "mirage", "wind"));

        register(new FishData("pearlglass", FishRarity.LEGENDARY,
                Map.of("earthworm", 20, "dumplings", 20, "river_snail", 40),
                160, 2.0f, 0.4f, false, "space", "mirage", "water"));

        register(new FishData("garvelze", FishRarity.LEGENDARY,
                Map.of("trout", 50, "eel", 40, "carp", 30, "salmon", 50),
                200, 2.8f, 0.3f, false, "fire", "space", "time"));

        register(new FishData("sea_bass", FishRarity.UNCOMMON,
                Map.of("shrimplet", 70, "dace", 60, "kasago", 60),
                133, 1.6f, 0.55f, true, "water", "wind"));

        register(new FishData("gigangora", FishRarity.LEGENDARY,
                Map.of("kasago", 30, "salmon", 40, "sea_bass", 50),
                200, 3.0f, 0.25f, false, "earth", "space", "time"));

        register(new FishData("mahimahi", FishRarity.COMMON,
                Map.of("dace", 50, "salmon", 70, "sea_bass", 70),
                160, 2.3f, 0.35f, false, "water", "fire", "space"));

        register(new FishData("tiger_rockfish", FishRarity.RARE,
                Map.of("earthworm", 40, "red_flies", 50, "river_bug", 60, "roe", 60),
                133, 1.4f, 0.6f, false, "fire", "earth"));

        register(new FishData("granakor", FishRarity.LEGENDARY,
                Map.of("crab", 40),
                160, 2.1f, 0.4f, false, "earth", "space"));

        register(new FishData("blue_marlin", FishRarity.LEGENDARY,
                Map.of("salmon", 40, "sea_bass", 30),
                200, 2.7f, 0.3f, false, "water", "space", "time"));

        register(new FishData("yamany", FishRarity.COMMON,
                Map.of("river_bug", 70, "roe", 80, "red_flies", 60),
                100, 1.0f, 0.8f, true, "wind"));

        register(new FishData("dynatrad", FishRarity.LEGENDARY,
                Map.of("trout", 50, "salmon", 40),
                267, 3.5f, 0.2f, false, "time", "space", "mirage"));
    }

    private static void register(FishData fishData) {
        FISH_DATA.put(fishData.getName(), fishData);
    }

    private static void buildBaitMappings() {
        // First, build mappings for regular baits
        for (FishData fish : FISH_DATA.values()) {
            for (String bait : fish.getBaitWeights().keySet()) {
                BAIT_TO_FISH.computeIfAbsent(bait, k -> new ArrayList<>()).add(fish);
            }
        }

        // Then, add fish that can be used as bait
        for (FishData fish : FISH_DATA.values()) {
            if (fish.canBeBait()) {
                String fishName = fish.getName();
                // Add this fish to the bait mappings so it can be used as bait
                BAIT_TO_FISH.computeIfAbsent(fishName, k -> new ArrayList<>());

                // Find all fish that can be caught using this fish as bait
                for (FishData targetFish : FISH_DATA.values()) {
                    if (targetFish.getBaitWeights().containsKey(fishName)) {
                        BAIT_TO_FISH.get(fishName).add(targetFish);
                    }
                }
            }
        }
    }

    public static FishData getFishData(String name) {
        return FISH_DATA.get(name);
    }

    public static FishData getRandomFish(String bait, RodType rodType) {
        List<FishData> possibleFish = BAIT_TO_FISH.get(bait);
        if (possibleFish == null || possibleFish.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (FishData fish : possibleFish) {
            int baseWeight = fish.getWeightForBait(bait);

            // Apply rod affinity bonus
            if (rodType.hasAffinityWith(bait)) {
                baseWeight = (int)(baseWeight * 1.5f); // 50% bonus for affinity baits
            }

            // Apply rod catch rate multiplier
            baseWeight = (int)(baseWeight * rodType.getCatchRateMultiplier());

            totalWeight += baseWeight;
        }

        int randomWeight = new Random().nextInt(totalWeight);
        int currentWeight = 0;

        for (FishData fish : possibleFish) {
            int baseWeight = fish.getWeightForBait(bait);

            // Apply same bonuses as above
            if (rodType.hasAffinityWith(bait)) {
                baseWeight = (int)(baseWeight * 1.5f);
            }
            baseWeight = (int)(baseWeight * rodType.getCatchRateMultiplier());

            currentWeight += baseWeight;
            if (randomWeight < currentWeight) {
                return fish;
            }
        }

        return possibleFish.get(0); // Fallback
    }

    // Keep the old method for backward compatibility
    public static FishData getRandomFish(String bait) {
        return getRandomFish(bait, RodType.PROGRESS_ROD); // Default to basic rod
    }

    public static List<FishData> getFishForBait(String bait) {
        return BAIT_TO_FISH.getOrDefault(bait, new ArrayList<>());
    }
}