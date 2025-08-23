package net.JordanRiver.KisekiLegend.fishing;

import java.util.Map;

public class FishData {
    private final String name;
    private final FishRarity rarity;
    private final Map<String, Integer> baitWeights; // bait -> weight
    private final int stamina;
    private final float speed; // movement speed multiplier
    private final float catchZoneSize; // multiplier for catch zone
    private final boolean canBeBait;
    private final String[] sepithRewards; // sepith types given when caught

    public FishData(String name, FishRarity rarity, Map<String, Integer> baitWeights,
                    int stamina, float speed, float catchZoneSize, boolean canBeBait, String... sepithRewards) {
        this.name = name;
        this.rarity = rarity;
        this.baitWeights = baitWeights;
        this.stamina = stamina;
        this.speed = speed;
        this.catchZoneSize = catchZoneSize;
        this.canBeBait = canBeBait;
        this.sepithRewards = sepithRewards;
    }

    // Getters
    public String getName() { return name; }
    public FishRarity getRarity() { return rarity; }
    public Map<String, Integer> getBaitWeights() { return baitWeights; }
    public int getStamina() { return stamina; }
    public float getSpeed() { return speed; }
    public float getCatchZoneSize() { return catchZoneSize; }
    public boolean canBeBait() { return canBeBait; }
    public String[] getSepithRewards() { return sepithRewards; }

    public int getWeightForBait(String bait) {
        return baitWeights.getOrDefault(bait, 0);
    }
}
