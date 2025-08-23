package net.JordanRiver.KisekiLegend.fishing;

public enum FishRarity {
    COMMON(4, 1.0f, 100),      // 4x4 bounding box, normal speed, 100 stamina
    UNCOMMON(5, 1.2f, 150),    // 5x5 bounding box, 20% faster, 150 stamina
    RARE(6, 1.5f, 200),        // 6x6 bounding box, 50% faster, 200 stamina
    LEGENDARY(7, 2.0f, 300);   // 7x7 bounding box, double speed, 300 stamina

    private final int boundingBoxSize;
    private final float speedMultiplier;
    private final int baseStamina;

    FishRarity(int boundingBoxSize, float speedMultiplier, int baseStamina) {
        this.boundingBoxSize = boundingBoxSize;
        this.speedMultiplier = speedMultiplier;
        this.baseStamina = baseStamina;
    }

    public int getBoundingBoxSize() { return boundingBoxSize; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public int getBaseStamina() { return baseStamina; }
}