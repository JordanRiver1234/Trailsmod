package net.JordanRiver.KisekiLegend.item.enhancement;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.*;

public class MaterialQualitySystem {

    // Material quality data structure
    public static class MaterialData {
        private final int baseQuality;
        private final Map<String, TraitData> traits;
        private final Map<String, EffectData> effects;

        public MaterialData(int baseQuality, Map<String, TraitData> traits, Map<String, EffectData> effects) {
            this.baseQuality = baseQuality;
            this.traits = traits != null ? traits : new HashMap<>();
            this.effects = effects != null ? effects : new HashMap<>();
        }

        public int getBaseQuality() { return baseQuality; }
        public Map<String, TraitData> getTraits() { return traits; }
        public Map<String, EffectData> getEffects() { return effects; }
    }

    public static class TraitData {
        private final String name;
        private final int level;
        private final TraitType type;

        public TraitData(String name, int level, TraitType type) {
            this.name = name;
            this.level = level;
            this.type = type;
        }

        public String getName() { return name; }
        public int getLevel() { return level; }
        public TraitType getType() { return type; }
    }

    public static class EffectData {
        private final String name;
        private final float value;
        private final int duration;
        private final EffectType type;

        public EffectData(String name, float value, int duration, EffectType type) {
            this.name = name;
            this.value = value;
            this.duration = duration;
            this.type = type;
        }

        public String getName() { return name; }
        public float getValue() { return value; }
        public int getDuration() { return duration; }
        public EffectType getType() { return type; }
    }

    public enum TraitType {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    public enum EffectType {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    // Material quality database
    private static final Map<Item, MaterialData> MATERIAL_QUALITY_DB = new HashMap<>();

    static {
        initializeMaterialQualities();
    }

    private static void initializeMaterialQualities() {
        // Water Materials - Generally healing/purifying
        addMaterial(Items.KELP, 150,
                Map.of("Healing", new TraitData("Healing", 2, TraitType.POSITIVE),
                        "Sponge", new TraitData("Sponge", 1, TraitType.NEUTRAL)),
                Map.of("HP Gain M", new EffectData("HP Gain M", 2.0f, 300, EffectType.POSITIVE)));

        addMaterial(Items.SEAGRASS, 120,
                Map.of("Natural Medicine", new TraitData("Natural Medicine", 1, TraitType.POSITIVE)),
                Map.of("Water Breathing", new EffectData("Water Breathing", 1.0f, 600, EffectType.POSITIVE)));

        addMaterial(Items.SEA_PICKLE, 180,
                Map.of("Light Glow", new TraitData("Light Glow", 2, TraitType.POSITIVE),
                        "Sticky Goo S", new TraitData("Sticky Goo S", 1, TraitType.NEGATIVE)),
                Map.of("Night Vision", new EffectData("Night Vision", 1.0f, 400, EffectType.POSITIVE)));

        // Fire Materials - Generally offensive/destructive
        addMaterial(Items.BLAZE_POWDER, 300,
                Map.of("Smoldering Lunacy", new TraitData("Smoldering Lunacy", 3, TraitType.POSITIVE),
                        "Power Throw", new TraitData("Power Throw", 2, TraitType.NEGATIVE)),
                Map.of("Fire Resistance", new EffectData("Fire Resistance", 1.0f, 600, EffectType.POSITIVE),
                        "Inflict Burn M", new EffectData("Inflict Burn M", 1.5f, 100, EffectType.NEGATIVE)));

        addMaterial(Items.MAGMA_CREAM, 250,
                Map.of("Dissolving Heat S", new TraitData("Dissolving Heat S", 2, TraitType.POSITIVE),
                        "Rapid", new TraitData("Rapid", 1, TraitType.NEGATIVE)),
                Map.of("ATK Up M", new EffectData("ATK Up M", 1.2f, 400, EffectType.POSITIVE)));

        addMaterial(Items.GUNPOWDER, 200,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.POSITIVE),
                        "Explosive", new TraitData("Explosive", 3, TraitType.NEGATIVE)),
                Map.of("Physical Damage M", new EffectData("Physical Damage M", 2.0f, 300, EffectType.POSITIVE),
                        "Self Harm", new EffectData("Self Harm", 0.5f, 60, EffectType.NEGATIVE)));

        // Earth Materials - Generally defensive/stable
        addMaterial(Items.CLAY_BALL, 100,
                Map.of("Defense Charge", new TraitData("Defense Charge", 1, TraitType.POSITIVE),
                        "Power Throw", new TraitData("Power Throw", 1, TraitType.NEGATIVE)),
                Map.of("DEF Up S", new EffectData("DEF Up S", 0.8f, 500, EffectType.POSITIVE)));

        addMaterial(Items.BRICK, 140,
                Map.of("Steel Protection", new TraitData("Steel Protection", 2, TraitType.POSITIVE)),
                Map.of("Guardian Mirror S", new EffectData("Guardian Mirror S", 1.5f, 600, EffectType.POSITIVE)));

        addMaterial(Items.COBBLESTONE, 80,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL),
                        "Slowdown S", new TraitData("Slowdown S", 2, TraitType.NEGATIVE)),
                Map.of("SPD Down S", new EffectData("SPD Down S", 0.2f, 200, EffectType.NEGATIVE)));

        // Wind Materials - Generally speed/agility
        addMaterial(Items.FEATHER, 160,
                Map.of("Speed Charge", new TraitData("Speed Charge", 2, TraitType.POSITIVE),
                        "Power Throw", new TraitData("Power Throw", 1, TraitType.NEGATIVE)),
                Map.of("Levitation", new EffectData("Levitation", 0.5f, 300, EffectType.POSITIVE)));

        addMaterial(Items.PHANTOM_MEMBRANE, 220,
                Map.of("Free Soul", new TraitData("Free Soul", 3, TraitType.POSITIVE),
                        "Curse Strength", new TraitData("Curse Strength", 2, TraitType.NEGATIVE)),
                Map.of("Slow Falling", new EffectData("Slow Falling", 1.0f, 400, EffectType.POSITIVE),
                        "ATK Down S", new EffectData("ATK Down S", 0.5f, 200, EffectType.NEGATIVE)));

        // Jewels - High quality with mixed effects
        addMaterial(Items.DIAMOND, 400,
                Map.of("Best Quality", new TraitData("Best Quality", 4, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 1, TraitType.NEGATIVE)),
                Map.of("Enhance Critical +20%", new EffectData("Enhance Critical +20%", 2.0f, 1200, EffectType.POSITIVE)));

        addMaterial(Items.EMERALD, 350,
                Map.of("Expensive+", new TraitData("Expensive+", 3, TraitType.POSITIVE),
                        "Curse Protection", new TraitData("Curse Protection", 2, TraitType.NEGATIVE)),
                Map.of("Money Magnet", new EffectData("Money Magnet", 1.5f, 800, EffectType.POSITIVE)));

        addMaterial(Items.AMETHYST_SHARD, 280,
                Map.of("Healing+", new TraitData("Healing+", 2, TraitType.POSITIVE),
                        "Resonant", new TraitData("Resonant", 1, TraitType.NEUTRAL)),
                Map.of("Magic Veil", new EffectData("Magic Veil", 1.0f, 600, EffectType.POSITIVE)));

        addMaterial(Items.LAPIS_LAZULI, 200,
                Map.of("Skill Charge", new TraitData("Skill Charge", 2, TraitType.POSITIVE)),
                Map.of("Enhance Skills +10%", new EffectData("Enhance Skills +10%", 1.3f, 600, EffectType.POSITIVE)));

        // Plants - Variable quality with nature-based effects
        addMaterial(Items.WHEAT, 90,
                Map.of("Healing", new TraitData("Healing", 1, TraitType.POSITIVE),
                        "Quality", new TraitData("Quality", 1, TraitType.NEUTRAL)),
                Map.of("Healing Taste S", new EffectData("Healing Taste S", 1.0f, 400, EffectType.POSITIVE)));

        addMaterial(Items.SUGAR_CANE, 110,
                Map.of("Mild Sweetness", new TraitData("Mild Sweetness", 1, TraitType.POSITIVE),
                        "Sticky Goo S", new TraitData("Sticky Goo S", 1, TraitType.NEGATIVE)),
                Map.of("SPD Up S", new EffectData("SPD Up S", 0.8f, 300, EffectType.POSITIVE)));

        addMaterial(Items.CACTUS, 130,
                Map.of("Grievous Wound S", new TraitData("Grievous Wound S", 2, TraitType.NEGATIVE),
                        "Defense Charge", new TraitData("Defense Charge", 1, TraitType.POSITIVE)),
                Map.of("Inflict Thorn S", new EffectData("Inflict Thorn S", 1.0f, 400, EffectType.NEGATIVE),
                        "Poison Cure", new EffectData("Poison Cure", 0.5f, 300, EffectType.POSITIVE)));

        // Mystery items - High variability
        addMaterial(Items.ENDER_PEARL, 320,
                Map.of("Overflowing Courage", new TraitData("Overflowing Courage", 3, TraitType.POSITIVE),
                        "Rapid+", new TraitData("Rapid+", 2, TraitType.NEGATIVE)),
                Map.of("Random Teleport", new EffectData("Random Teleport", 1.0f, 1, EffectType.POSITIVE),
                        "Surprise! S", new EffectData("Surprise! S", 0.1f, 100, EffectType.NEGATIVE)));

        addMaterial(Items.GHAST_TEAR, 380,
                Map.of("Twilight Invitation S", new TraitData("Twilight Invitation S", 3, TraitType.NEGATIVE),
                        "Terrific Healing", new TraitData("Terrific Healing", 4, TraitType.POSITIVE)),
                Map.of("HP Regen L", new EffectData("HP Regen L", 2.0f, 600, EffectType.POSITIVE),
                        "Inflict Curse S", new EffectData("Inflict Curse S", 1.0f, 300, EffectType.NEGATIVE)));

        // Jewel Tag
        addMaterial(Items.QUARTZ, 220,
                Map.of("High Quality", new TraitData("High Quality", 2, TraitType.POSITIVE)),
                Map.of("Remove Debuffs", new EffectData("Remove Debuffs", 1.0f, 600, EffectType.NEUTRAL)));
        addMaterial(Items.PRISMARINE_SHARD, 180,
                Map.of("Natural Medicine", new TraitData("Natural Medicine", 2, TraitType.POSITIVE),
                        "Sharp Edge S", new TraitData("Sharp Edge S", 1, TraitType.POSITIVE)),
                Map.of("Water Breathing", new EffectData("Water Breathing", 1.0f, 800, EffectType.POSITIVE)));
        addMaterial(Items.PRISMARINE_CRYSTALS, 210,
                Map.of("Natural Medicine", new TraitData("Natural Medicine", 2, TraitType.POSITIVE),
                        "Light Glow", new TraitData("Light Glow", 2, TraitType.POSITIVE)),
                Map.of("Night Vision", new EffectData("Night Vision", 1.0f, 600, EffectType.POSITIVE)));

        // Water Material Tag
        addMaterial(Items.WATER_BUCKET, 50,
                Map.of("Flowing Wisdom", new TraitData("Flowing Wisdom", 1, TraitType.NEUTRAL), "Quality", new TraitData("Quality", 1, TraitType.POSITIVE)),
                Map.of("HP Gain S", new EffectData("HP Gain S", 1.0f, 100, EffectType.POSITIVE)));
        addMaterial(Items.ICE, 60,
                Map.of("Icy Echo", new TraitData("Icy Echo", 1, TraitType.NEUTRAL), "Sticky Goo S", new TraitData("Sticky Goo S", 1, TraitType.NEGATIVE)),
                Map.of("Inflict Frostbite S", new EffectData("Inflict Frostbite S", 1.0f, 200, EffectType.NEGATIVE)));
        addMaterial(Items.PACKED_ICE, 80,
                Map.of("Icy Echo", new TraitData("Icy Echo", 2, TraitType.NEUTRAL), "Defense Charge", new TraitData("Defense Charge", 1, TraitType.POSITIVE)),
                Map.of("Inflict Frostbite M", new EffectData("Inflict Frostbite M", 1.5f, 300, EffectType.NEGATIVE)));
        addMaterial(Items.BLUE_ICE, 120,
                Map.of("Perpetual Ice S", new TraitData("Perpetual Ice S", 1, TraitType.NEUTRAL), "Sticky Goo M", new TraitData("Sticky Goo M", 2, TraitType.NEGATIVE)),
                Map.of("Ice Damage S", new EffectData("Ice Damage S", 1.0f, 200, EffectType.NEGATIVE)));
        addMaterial(Items.SNOWBALL, 30,
                Map.of("Icy Echo", new TraitData("Icy Echo", 1, TraitType.NEUTRAL), "Power Throw", new TraitData("Power Throw", 2, TraitType.NEGATIVE)),
                Map.of("Inflict Frostbite S", new EffectData("Inflict Frostbite S", 0.5f, 100, EffectType.NEGATIVE)));
        addMaterial(Items.SNOW_BLOCK, 40,
                Map.of("Icy Echo", new TraitData("Icy Echo", 1, TraitType.NEUTRAL), "Soft Texture", new TraitData("Soft Texture", 1, TraitType.NEUTRAL)), null);
        addMaterial(Items.NAUTILUS_SHELL, 300,
                Map.of("Primordial Power", new TraitData("Primordial Power", 2, TraitType.POSITIVE), "Secret Rainbow", new TraitData("Secret Rainbow", 2, TraitType.NEUTRAL)),
                Map.of("Water Breathing", new EffectData("Water Breathing", 1.0f, 900, EffectType.POSITIVE)));
        addMaterial(Items.HEART_OF_THE_SEA, 700,
                Map.of("Glorious Soul", new TraitData("Glorious Soul", 4, TraitType.POSITIVE), "Divine Petal", new TraitData("Divine Petal", 5, TraitType.POSITIVE)),
                Map.of("Defense Veil", new EffectData("Defense Veil", 5.0f, 1800, EffectType.POSITIVE)));
        addMaterial(Items.SPONGE, 150,
                Map.of("Sponge", new TraitData("Sponge", 3, TraitType.POSITIVE)), null);
        addMaterial(Items.WET_SPONGE, 100,
                Map.of("Sticky Goo S", new TraitData("Sticky Goo S", 2, TraitType.NEGATIVE), "Slowdown S", new TraitData("Slowdown S", 2, TraitType.NEGATIVE)), null);
        addMaterial(Items.COD, 80,
                Map.of("Healing Taste S", new TraitData("Healing Taste S", 1, TraitType.POSITIVE), "Quality", new TraitData("Quality", 1, TraitType.NEUTRAL)),
                Map.of("HP Gain S", new EffectData("HP Gain S", 2.0f, 100, EffectType.POSITIVE)));
        addMaterial(Items.SALMON, 100,
                Map.of("Healing Taste M", new TraitData("Healing Taste M", 2, TraitType.POSITIVE), "Rich Flavor", new TraitData("Rich Flavor", 1, TraitType.POSITIVE)),
                Map.of("Feeling Full S", new EffectData("Feeling Full S", 1.2f, 200, EffectType.POSITIVE)));
        addMaterial(Items.PUFFERFISH, 150,
                Map.of("Assassin Poison S", new TraitData("Assassin Poison S", 3, TraitType.NEGATIVE), "Area Bonus", new TraitData("Area Bonus", 1, TraitType.NEUTRAL)),
                Map.of("Inflict Poison M", new EffectData("Inflict Poison M", 2.0f, 300, EffectType.NEGATIVE), "Water Breathing", new EffectData("Water Breathing", 1.0f, 600, EffectType.POSITIVE)));

        // Earth Material Tag
        addMaterial(Items.DIRT, 10,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Curse Protection", new TraitData("Curse Protection", 1, TraitType.NEGATIVE)), null);
        addMaterial(Items.SAND, 30,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Rapid", new TraitData("Rapid", 1, TraitType.NEGATIVE)), null);
        addMaterial(Items.GRAVEL, 40,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Rapid+", new TraitData("Rapid+", 2, TraitType.NEGATIVE)), null);

        // Fire Material Tag
        addMaterial(Items.LAVA_BUCKET, 280,
                Map.of("Smoldering Lunacy", new TraitData("Smoldering Lunacy", 3, TraitType.POSITIVE), "Explosive", new TraitData("Explosive", 3, TraitType.NEGATIVE)),
                Map.of("Fire Damage L", new EffectData("Fire Damage L", 2.0f, 100, EffectType.POSITIVE)));
        addMaterial(Items.FLINT_AND_STEEL, 150,
                Map.of("Critical", new TraitData("Critical", 2, TraitType.POSITIVE)),
                Map.of("Inflict Burn S", new EffectData("Inflict Burn S", 1.0f, 20, EffectType.POSITIVE)));
        addMaterial(Items.BLAZE_ROD, 320,
                Map.of("Smoldering Lunacy", new TraitData("Smoldering Lunacy", 3, TraitType.POSITIVE), "War God's Power", new TraitData("War God's Power", 2, TraitType.POSITIVE)),
                Map.of("Fire Damage XL", new EffectData("Fire Damage XL", 3.0f, 1200, EffectType.POSITIVE)));
        addMaterial(Items.MAGMA_BLOCK, 220,
                Map.of("Dissolving Heat S", new TraitData("Dissolving Heat S", 2, TraitType.POSITIVE), "Grievous Wound S", new TraitData("Grievous Wound S", 2, TraitType.NEGATIVE)), null);
        addMaterial(Items.FIRE_CHARGE, 180,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.POSITIVE), "Power Throw", new TraitData("Power Throw", 2, TraitType.NEGATIVE)),
                Map.of("Explosive", new EffectData("Explosive", 1.0f, 1, EffectType.POSITIVE)));
        addMaterial(Items.COAL, 100,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Dissolving Heat S", new TraitData("Dissolving Heat S", 2, TraitType.POSITIVE)),
                Map.of("ATK Up S", new EffectData("ATK Up S", 1.0f, 800, EffectType.POSITIVE)));
        addMaterial(Items.CHARCOAL, 90,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Dissolving Heat S", new TraitData("Dissolving Heat S", 2, TraitType.POSITIVE)),
                Map.of("ATK Up S", new EffectData("ATK Up S", 1.0f, 800, EffectType.POSITIVE)));

        // Space Material Tag
        addMaterial(Items.END_STONE, 250,
                Map.of("Mystic Life", new TraitData("Mystic Life", 2, TraitType.NEUTRAL), "Steel Protection", new TraitData("Steel Protection", 3, TraitType.POSITIVE)), null);
        addMaterial(Items.PURPUR_BLOCK, 280,
                Map.of("Secret Rainbow", new TraitData("Secret Rainbow", 2, TraitType.NEUTRAL), "Resonant", new TraitData("Resonant", 2, TraitType.NEUTRAL)), null);
        addMaterial(Items.CHORUS_FRUIT, 200,
                Map.of("Overflowing Courage", new TraitData("Overflowing Courage", 2, TraitType.POSITIVE), "Fantasy Spore", new TraitData("Fantasy Spore", 1, TraitType.NEUTRAL)),
                Map.of("Random Teleport", new EffectData("Random Teleport", 1.0f, 1, EffectType.NEUTRAL)));
        addMaterial(Items.SHULKER_SHELL, 350,
                Map.of("Dragonscale Protection", new TraitData("Dragonscale Protection", 4, TraitType.POSITIVE), "Steel Protection", new TraitData("Steel Protection", 4, TraitType.POSITIVE)),
                Map.of("Levitation", new EffectData("Levitation", 1.0f, 1200, EffectType.POSITIVE)));
        addMaterial(Items.OBSIDIAN, 300,
                Map.of("Indestructible Shield", new TraitData("Indestructible Shield", 4, TraitType.POSITIVE), "Slowdown M", new TraitData("Slowdown M", 3, TraitType.NEGATIVE)), null);
        addMaterial(Items.CRYING_OBSIDIAN, 320,
                Map.of("Twilight Invitation S", new TraitData("Twilight Invitation S", 2, TraitType.NEUTRAL), "Mystic Life", new TraitData("Mystic Life", 2, TraitType.NEUTRAL)),
                Map.of("KO Recovery S", new EffectData("KO Recovery S", 1.0f, 1, EffectType.POSITIVE)));

        // Wind Material Tag
        addMaterial(Items.ELYTRA, 800,
                Map.of("Speed of Light", new TraitData("Speed of Light", 5, TraitType.POSITIVE), "Rarest", new TraitData("Rarest", 4, TraitType.POSITIVE), "Power Throw+", new TraitData("Power Throw+", 2, TraitType.NEGATIVE)),
                Map.of("Wind Rider", new EffectData("Wind Rider", 1.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.ARROW, 50,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL)), null);
        addMaterial(Items.OAK_LEAVES, 20,
                Map.of("Natural Medicine", new TraitData("Natural Medicine", 1, TraitType.POSITIVE), "Power Throw", new TraitData("Power Throw", 2, TraitType.NEGATIVE)), null);

        // Time Material Tag
        addMaterial(Items.CLOCK, 250,
                Map.of("Reverse Hour Hand", new TraitData("Reverse Hour Hand", 2, TraitType.NEUTRAL), "High Quality", new TraitData("High Quality", 2, TraitType.POSITIVE)),
                Map.of("SPD Up M", new EffectData("SPD Up M", 1.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.REDSTONE, 150,
                Map.of("Thunder Burn", new TraitData("Thunder Burn", 2, TraitType.POSITIVE), "Curse Protection", new TraitData("Curse Protection", 1, TraitType.NEGATIVE)),
                Map.of("Lightning Damage S", new EffectData("Lightning Damage S", 1.0f, 1, EffectType.POSITIVE)));
        addMaterial(Items.SANDSTONE, 50,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL), "Critical", new TraitData("Critical", 2, TraitType.NEGATIVE)), null);
        addMaterial(Items.SOUL_SAND, 180,
                Map.of("Infinite Energy", new TraitData("Infinite Energy", 2, TraitType.NEUTRAL), "Slowdown M", new TraitData("Slowdown M", 2, TraitType.NEGATIVE)), null);

        // Mirage Material Tag
        addMaterial(Items.GLASS, 80,
                Map.of("Light Glow", new TraitData("Light Glow", 2, TraitType.POSITIVE), "Power Throw+", new TraitData("Power Throw+", 3, TraitType.NEGATIVE)), null);
        addMaterial(Items.ENDER_EYE, 350,
                Map.of("Free Soul", new TraitData("Free Soul", 3, TraitType.POSITIVE), "Rapid+", new TraitData("Rapid+", 2, TraitType.NEGATIVE)),
                Map.of("Eye for Materials", new EffectData("Eye for Materials", 1.0f, 100, EffectType.POSITIVE)));

        // Mystery Tag
        addMaterial(Items.DRAGON_EGG, 999,
                Map.of("Stats Power", new TraitData("Stats Power", 5, TraitType.POSITIVE), "Glorious Soul", new TraitData("Glorious Soul", 5, TraitType.POSITIVE)),
                Map.of("Dragon Slayer", new EffectData("Dragon Slayer", 99.9f, 9999, EffectType.POSITIVE)));
        addMaterial(Items.NETHER_STAR, 950,
                Map.of("Glorious Soul", new TraitData("Glorious Soul", 5, TraitType.POSITIVE), "War God's Power", new TraitData("War God's Power", 5, TraitType.POSITIVE)),
                Map.of("All Stats Up L", new EffectData("All Stats Up L", 5.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.WITHER_SKELETON_SKULL, 400,
                Map.of("Hazy Outline S", new TraitData("Hazy Outline S", 3, TraitType.NEGATIVE), "Twilight Invitation S", new TraitData("Twilight Invitation S", 2, TraitType.NEUTRAL)),
                Map.of("Inflict Curse L", new EffectData("Inflict Curse L", 2.0f, 400, EffectType.NEGATIVE)));
        addMaterial(Items.ECHO_SHARD, 450,
                Map.of("Overflowing Courage", new TraitData("Overflowing Courage", 3, TraitType.NEUTRAL), "Glittering Darkness", new TraitData("Glittering Darkness", 3, TraitType.NEUTRAL)),
                Map.of("KO Recovery M", new EffectData("KO Recovery M", 1.0f, 1, EffectType.POSITIVE)));
        addMaterial(Items.GOAT_HORN, 200,
                Map.of("Thunder Current S", new TraitData("Thunder Current S", 2, TraitType.NEUTRAL)),
                Map.of("Thunderclap S", new EffectData("Thunderclap S", 1.0f, 1, EffectType.POSITIVE)));

        // Accessory Tag
        addMaterial(Items.SPYGLASS, 180,
                Map.of("Clear Head S", new TraitData("Clear Head S", 2, TraitType.POSITIVE)),
                Map.of("Eye for Materials", new EffectData("Eye for Materials", 1.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.COMPASS, 150,
                Map.of("Clear Head S", new TraitData("Clear Head S", 2, TraitType.POSITIVE)),
                Map.of("Treasure Hunter", new EffectData("Treasure Hunter", 1.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.RECOVERY_COMPASS, 300,
                Map.of("Free Soul", new TraitData("Free Soul", 3, TraitType.POSITIVE)),
                Map.of("KO Recovery S", new EffectData("KO Recovery S", 1.0f, 0, EffectType.POSITIVE)));
        addMaterial(Items.TOTEM_OF_UNDYING, 600,
                Map.of("Glorious Soul", new TraitData("Glorious Soul", 5, TraitType.POSITIVE), "Divine Petal", new TraitData("Divine Petal", 5, TraitType.POSITIVE)),
                Map.of("Resist KO +10%", new EffectData("Resist KO +10%", 1.0f, 1, EffectType.POSITIVE)));

        // Bomb Tag
        addMaterial(Items.TNT, 250,
                Map.of("Destructive+", new TraitData("Destructive+", 3, TraitType.POSITIVE), "Power Throw+", new TraitData("Power Throw+", 3, TraitType.NEGATIVE)),
                Map.of("Explosive", new EffectData("Explosive", 4.0f, 1, EffectType.POSITIVE)));

        // Cooking Tag
        addMaterial(Items.BOWL, 40, Map.of("Quality", new TraitData("Quality", 1, TraitType.POSITIVE)), null);
        addMaterial(Items.BUCKET, 80, Map.of("High Quality", new TraitData("High Quality", 2, TraitType.POSITIVE)), null);
        addMaterial(Items.MILK_BUCKET, 100,
                Map.of("High Quality", new TraitData("High Quality", 2, TraitType.POSITIVE), "Healing", new TraitData("Healing", 2, TraitType.POSITIVE)),
                Map.of("Remove Ailments", new EffectData("Remove Ailments", 1.0f, 1, EffectType.POSITIVE)));

        // Dessert Tag
        addMaterial(Items.COOKIE, 80,
                Map.of("Critical", new TraitData("Critical", 1, TraitType.POSITIVE),
                        "Rapid", new TraitData("Rapid", 2, TraitType.NEGATIVE)),
                Map.of("Mild Sweetness", new EffectData("Mild Sweetness", 1.0f, 200, EffectType.POSITIVE)));

        addMaterial(Items.HONEY_BOTTLE, 120,
                Map.of("Healing", new TraitData("Healing", 2, TraitType.POSITIVE),
                        "Power Throw", new TraitData("Power Throw", 1, TraitType.NEGATIVE)),
                Map.of("Poison Cure", new EffectData("Poison Cure", 1.0f, 1, EffectType.POSITIVE),
                        "Sweetness", new EffectData("Sweetness", 1.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.APPLE, 90,
                Map.of("HP Charge", new TraitData("HP Charge", 1, TraitType.POSITIVE)),
                Map.of("HP Gain S", new EffectData("HP Gain S", 1.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.GOLDEN_APPLE, 350,
                Map.of("Healing+", new TraitData("Healing+", 3, TraitType.POSITIVE),
                        "Critical+", new TraitData("Critical+", 2, TraitType.POSITIVE),
                        "Expensive", new TraitData("Expensive", 2, TraitType.NEGATIVE)),
                Map.of("HP Gain L", new EffectData("HP Gain L", 2.0f, 1, EffectType.POSITIVE),
                        "HP Regen M", new EffectData("HP Regen M", 1.0f, 600, EffectType.POSITIVE)));

        addMaterial(Items.ENCHANTED_GOLDEN_APPLE, 850,
                Map.of("Healing++", new TraitData("Healing++", 5, TraitType.POSITIVE),
                        "Critical++", new TraitData("Critical++", 4, TraitType.POSITIVE),
                        "Stats Charge+", new TraitData("Stats Charge+", 3, TraitType.POSITIVE),
                        "Expensive++", new TraitData("Expensive++", 3, TraitType.NEGATIVE)),
                Map.of("HP Gain XL", new EffectData("HP Gain XL", 4.0f, 1, EffectType.POSITIVE),
                        "All Stats Up L", new EffectData("All Stats Up L", 2.0f, 1200, EffectType.POSITIVE),
                        "Energy Surge L", new EffectData("Energy Surge L", 1.0f, 1800, EffectType.POSITIVE)));

        // Elixir Tag
        addMaterial(Items.EXPERIENCE_BOTTLE, 180,
                Map.of("Quality", new TraitData("Quality", 2, TraitType.POSITIVE),
                        "Destructive", new TraitData("Destructive", 1, TraitType.POSITIVE)),
                Map.of("XP Gain", new EffectData("XP Gain", 5.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.DRAGON_BREATH, 400,
                Map.of("Destructive+", new TraitData("Destructive+", 4, TraitType.POSITIVE),
                        "Area Bonus", new TraitData("Area Bonus", 3, TraitType.POSITIVE),
                        "Power Throw+", new TraitData("Power Throw+", 2, TraitType.NEGATIVE)),
                Map.of("Fire Damage L", new EffectData("Fire Damage L", 3.0f, 1, EffectType.POSITIVE),
                        "Inflict Burn L", new EffectData("Inflict Burn L", 1.0f, 400, EffectType.NEGATIVE)));

        // Food Tag
        addMaterial(Items.BREAD, 100,
                Map.of("HP Charge", new TraitData("HP Charge", 2, TraitType.POSITIVE)),
                Map.of("Healing Taste S", new EffectData("Healing Taste S", 1.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.COOKED_BEEF, 140,
                Map.of("Attack Charge", new TraitData("Attack Charge", 2, TraitType.POSITIVE),
                        "Defense Charge", new TraitData("Defense Charge", 1, TraitType.POSITIVE)),
                Map.of("Healing Taste M", new EffectData("Healing Taste M", 1.5f, 1, EffectType.POSITIVE),
                        "ATK Up S", new EffectData("ATK Up S", 1.0f, 400, EffectType.POSITIVE)));

        addMaterial(Items.GOLDEN_CARROT, 330,
                Map.of("Quality+", new TraitData("Quality+", 3, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 2, TraitType.POSITIVE),
                        "Expensive", new TraitData("Expensive", 2, TraitType.NEGATIVE)),
                Map.of("Night Vision", new EffectData("Night Vision", 1.0f, 600, EffectType.POSITIVE),
                        "Healing Taste L", new EffectData("Healing Taste L", 2.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.SUSPICIOUS_STEW, 160,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.NEUTRAL),
                        "Critical", new TraitData("Critical", 2, TraitType.NEUTRAL),
                        "Rapid", new TraitData("Rapid", 3, TraitType.NEGATIVE)),
                Map.of("Random Effect", new EffectData("Random Effect", 1.0f, 300, EffectType.NEUTRAL),
                        "Inflict Poison S", new EffectData("Inflict Poison S", 0.3f, 200, EffectType.NEGATIVE)));

        // Ingot Tag
        addMaterial(Items.IRON_INGOT, 150,
                Map.of("Defense Charge", new TraitData("Defense Charge", 2, TraitType.POSITIVE),
                        "Power Throw", new TraitData("Power Throw", 1, TraitType.NEGATIVE)),
                Map.of("DEF Up S", new EffectData("DEF Up S", 1.0f, 800, EffectType.POSITIVE)));

        addMaterial(Items.GOLD_INGOT, 200,
                Map.of("Expensive", new TraitData("Expensive", 3, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 2, TraitType.POSITIVE),
                        "Defense Charge", new TraitData("Defense Charge", 2, TraitType.NEGATIVE)),
                Map.of("Critical Rate Up S", new EffectData("Critical Rate Up S", 1.0f, 600, EffectType.POSITIVE)));

        addMaterial(Items.COPPER_INGOT, 120,
                Map.of("Speed Charge", new TraitData("Speed Charge", 2, TraitType.POSITIVE),
                        "Quality", new TraitData("Quality", 1, TraitType.NEGATIVE)),
                Map.of("SPD Up S", new EffectData("SPD Up S", 1.0f, 600, EffectType.POSITIVE)));

        addMaterial(Items.NETHERITE_INGOT, 900,
                Map.of("Stats Charge+", new TraitData("Stats Charge+", 5, TraitType.POSITIVE),
                        "Destructive++", new TraitData("Destructive++", 4, TraitType.POSITIVE),
                        "Quality++", new TraitData("Quality++", 4, TraitType.POSITIVE),
                        "Expensive++", new TraitData("Expensive++", 4, TraitType.NEGATIVE)),
                Map.of("All Stats Up L", new EffectData("All Stats Up L", 2.0f, 1200, EffectType.POSITIVE),
                        "Fire Resist Up+", new EffectData("Fire Resist Up+", 1.0f, 0, EffectType.POSITIVE),
                        "Reduce Damage -10%", new EffectData("Reduce Damage -10%", 1.0f, 0, EffectType.POSITIVE)));

        // Magic Tool Tag
        addMaterial(Items.BOOK, 100,
                Map.of("Skill Charge", new TraitData("Skill Charge", 1, TraitType.POSITIVE)),
                Map.of("Enhance Skills +3%", new EffectData("Enhance Skills +3%", 1.0f, 0, EffectType.POSITIVE)));

        addMaterial(Items.ENCHANTED_BOOK, 250,
                Map.of("Skill Charge+", new TraitData("Skill Charge+", 3, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 2, TraitType.POSITIVE)),
                Map.of("Enhance Skills +7%", new EffectData("Enhance Skills +7%", 1.0f, 0, EffectType.POSITIVE),
                        "Critical Rate Up S", new EffectData("Critical Rate Up S", 1.0f, 0, EffectType.POSITIVE)));

        addMaterial(Items.PAPER, 30,
                Map.of("Rapid", new TraitData("Rapid", 2, TraitType.POSITIVE),
                        "Destructive", new TraitData("Destructive", 2, TraitType.NEGATIVE)),
                Map.of("Weaken Items +3%", new EffectData("Weaken Items +3%", 1.0f, 0, EffectType.POSITIVE)));

        // Medicinal Tag
        addMaterial(Items.GLISTERING_MELON_SLICE, 200,
                Map.of("Healing+", new TraitData("Healing+", 2, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 1, TraitType.POSITIVE)),
                Map.of("HP Gain M", new EffectData("HP Gain M", 1.0f, 1, EffectType.POSITIVE),
                        "Poison Cure", new EffectData("Poison Cure", 1.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.SPIDER_EYE, 100,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.NEGATIVE),
                        "Quality", new TraitData("Quality", 2, TraitType.NEGATIVE)),
                Map.of("Inflict Poison M", new EffectData("Inflict Poison M", 1.0f, 200, EffectType.NEGATIVE),
                        "Poison Damage XS", new EffectData("Poison Damage XS", 1.0f, 300, EffectType.NEGATIVE)));

        addMaterial(Items.FERMENTED_SPIDER_EYE, 150,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.NEGATIVE),
                        "Critical", new TraitData("Critical", 1, TraitType.NEGATIVE),
                        "Area Bonus", new TraitData("Area Bonus", 1, TraitType.POSITIVE)),
                Map.of("Inflict Curse M", new EffectData("Inflict Curse M", 1.0f, 400, EffectType.NEGATIVE),
                        "All Stats Down S", new EffectData("All Stats Down S", 1.0f, 300, EffectType.NEGATIVE)));

        // Oil Tag
        addMaterial(Items.SLIME_BALL, 130,
                Map.of("Area Bonus", new TraitData("Area Bonus", 2, TraitType.POSITIVE),
                        "Speed Charge", new TraitData("Speed Charge", 1, TraitType.POSITIVE),
                        "Rapid", new TraitData("Rapid", 2, TraitType.NEGATIVE)),
                Map.of("Inflict Slow S", new EffectData("Inflict Slow S", 1.0f, 300, EffectType.NEGATIVE),
                        "Evasion Up S", new EffectData("Evasion Up S", 1.0f, 400, EffectType.POSITIVE)));

        // Ore Tag
        addMaterial(Items.RAW_IRON, 130,
                Map.of("Defense Charge", new TraitData("Defense Charge", 2, TraitType.NEUTRAL),
                        "Quality", new TraitData("Quality", 2, TraitType.NEGATIVE)),
                Map.of("DEF Up S", new EffectData("DEF Up S", 0.8f, 600, EffectType.POSITIVE)));

        addMaterial(Items.RAW_GOLD, 180,
                Map.of("Expensive", new TraitData("Expensive", 2, TraitType.NEUTRAL),
                        "Quality", new TraitData("Quality", 2, TraitType.NEGATIVE)),
                Map.of("Critical Rate Up S", new EffectData("Critical Rate Up S", 0.8f, 400, EffectType.POSITIVE)));

        addMaterial(Items.RAW_COPPER, 100,
                Map.of("Speed Charge", new TraitData("Speed Charge", 1, TraitType.NEUTRAL),
                        "Quality", new TraitData("Quality", 3, TraitType.NEGATIVE)),
                Map.of("SPD Up S", new EffectData("SPD Up S", 0.7f, 400, EffectType.POSITIVE)));

        // Plant Tag
        addMaterial(Items.NETHER_WART, 180,
                Map.of("Destructive", new TraitData("Destructive", 3, TraitType.POSITIVE),
                        "Quality", new TraitData("Quality", 1, TraitType.NEGATIVE)),
                Map.of("Enhance Items +5%", new EffectData("Enhance Items +5%", 1.0f, 0, EffectType.POSITIVE),
                        "Fire Resist Up", new EffectData("Fire Resist Up", 1.0f, 0, EffectType.POSITIVE)));

        addMaterial(Items.COCOA_BEANS, 100,
                Map.of("HP Charge", new TraitData("HP Charge", 2, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 1, TraitType.POSITIVE)),
                Map.of("Mild Sweetness", new EffectData("Mild Sweetness", 1.0f, 200, EffectType.POSITIVE),
                        "SPD Up S", new EffectData("SPD Up S", 0.5f, 300, EffectType.POSITIVE)));

        // Poison Tag
        addMaterial(Items.POISONOUS_POTATO, 50,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.NEGATIVE),
                        "Quality", new TraitData("Quality", 3, TraitType.NEGATIVE)),
                Map.of("Inflict Poison L", new EffectData("Inflict Poison L", 1.0f, 300, EffectType.NEGATIVE),
                        "HP Gain XS", new EffectData("HP Gain XS", 0.5f, 1, EffectType.POSITIVE)));

        addMaterial(Items.ROTTEN_FLESH, 20,
                Map.of("Healing", new TraitData("Healing", 3, TraitType.NEGATIVE),
                        "Quality", new TraitData("Quality", 4, TraitType.NEGATIVE)),
                Map.of("Inflict Poison M", new EffectData("Inflict Poison M", 1.0f, 400, EffectType.NEGATIVE),
                        "HP Gain XS", new EffectData("HP Gain XS", 0.3f, 1, EffectType.POSITIVE)));

        // Spice Tag
        addMaterial(Items.SUGAR, 80,
                Map.of("Speed Charge", new TraitData("Speed Charge", 1, TraitType.POSITIVE),
                        "Rapid", new TraitData("Rapid", 1, TraitType.POSITIVE)),
                Map.of("SPD Up S", new EffectData("SPD Up S", 1.0f, 300, EffectType.POSITIVE),
                        "Sweetness", new EffectData("Sweetness", 1.0f, 1, EffectType.POSITIVE)));

        // Sundry Tag
        addMaterial(Items.STICK, 10,
                Map.of("Quality", new TraitData("Quality", 1, TraitType.NEUTRAL)),
                null);

        addMaterial(Items.FLINT, 50,
                Map.of("Destructive", new TraitData("Destructive", 1, TraitType.POSITIVE)),
                Map.of("Physical Damage XS", new EffectData("Physical Damage XS", 1.0f, 1, EffectType.POSITIVE)));

        addMaterial(Items.STRING, 40,
                Map.of("Area Bonus", new TraitData("Area Bonus", 1, TraitType.POSITIVE)),
                Map.of("Inflict Slow S", new EffectData("Inflict Slow S", 0.5f, 200, EffectType.NEGATIVE)));

        addMaterial(Items.BONE, 60,
                Map.of("Attack Charge", new TraitData("Attack Charge", 1, TraitType.POSITIVE),
                        "Defense Charge", new TraitData("Defense Charge", 2, TraitType.NEGATIVE)),
                Map.of("ATK Up S", new EffectData("ATK Up S", 0.8f, 400, EffectType.POSITIVE)));

        addMaterial(Items.LEATHER, 100,
                Map.of("Defense Charge", new TraitData("Defense Charge", 2, TraitType.POSITIVE),
                        "Speed Charge", new TraitData("Speed Charge", 2, TraitType.POSITIVE)),
                Map.of("DEF Up S", new EffectData("DEF Up S", 1.0f, 600, EffectType.POSITIVE),
                        "Reduce Damage -3%", new EffectData("Reduce Damage -3%", 1.0f, 0, EffectType.POSITIVE)));

        addMaterial(Items.EGG, 50,
                Map.of("HP Charge", new TraitData("HP Charge", 1, TraitType.POSITIVE),
                        "Defense Charge", new TraitData("Defense Charge", 3, TraitType.NEGATIVE)),
                Map.of("HP Gain XS", new EffectData("HP Gain XS", 1.0f, 1, EffectType.POSITIVE)));

        // Supplement Tag
        addMaterial(Items.GLOWSTONE_DUST, 180,
                Map.of("Destructive", new TraitData("Destructive", 2, TraitType.POSITIVE),
                        "Critical", new TraitData("Critical", 2, TraitType.POSITIVE)),
                Map.of("Enhance Items +5%", new EffectData("Enhance Items +5%", 1.0f, 0, EffectType.POSITIVE),
                        "Light Blessing S", new EffectData("Light Blessing S", 1.0f, 600, EffectType.POSITIVE)));

        // Cloth/Wool/Threads Tag
        addMaterial(Items.WHITE_WOOL, 70,
                Map.of("HP Charge", new TraitData("HP Charge", 2, TraitType.POSITIVE),
                        "Defense Charge", new TraitData("Defense Charge", 1, TraitType.POSITIVE),
                        "Destructive", new TraitData("Destructive", 2, TraitType.NEGATIVE)),
                Map.of("HP Regen S", new EffectData("HP Regen S", 1.0f, 600, EffectType.POSITIVE),
                        "Fire Vulnerability", new EffectData("Fire Vulnerability", 1.0f, 0, EffectType.NEGATIVE)));
    }

    private static void addMaterial(Item item, int quality, Map<String, TraitData> traits, Map<String, EffectData> effects) {
        MATERIAL_QUALITY_DB.put(item, new MaterialData(quality, traits, effects));
    }

    // Public API methods
    public static MaterialData getMaterialData(Item item) {
        return MATERIAL_QUALITY_DB.get(item);
    }

    public static int getBaseQuality(Item item) {
        MaterialData data = getMaterialData(item);
        return data != null ? data.getBaseQuality() : 100; // Default quality
    }

    public static Map<String, TraitData> getMaterialTraits(Item item) {
        MaterialData data = getMaterialData(item);
        return data != null ? data.getTraits() : new HashMap<>();
    }

    public static Map<String, EffectData> getMaterialEffects(Item item) {
        MaterialData data = getMaterialData(item);
        return data != null ? data.getEffects() : new HashMap<>();
    }

    public static boolean hasMaterialData(Item item) {
        return MATERIAL_QUALITY_DB.containsKey(item);
    }

    // Calculate combined quality from multiple materials
    public static int calculateCombinedQuality(List<Item> materials) {
        if (materials.isEmpty()) return 100;

        int totalQuality = 0;
        int count = 0;

        for (Item material : materials) {
            totalQuality += getBaseQuality(material);
            count++;
        }

        return totalQuality / count; // Average quality
    }

    // Get all traits from a list of materials
    public static Map<String, Integer> getCombinedTraits(List<Item> materials) {
        Map<String, Integer> combinedTraits = new HashMap<>();

        for (Item material : materials) {
            Map<String, TraitData> materialTraits = getMaterialTraits(material);
            for (Map.Entry<String, TraitData> entry : materialTraits.entrySet()) {
                TraitData trait = entry.getValue();
                combinedTraits.merge(trait.getName(), trait.getLevel(), Integer::sum);
            }
        }

        return combinedTraits;
    }

    // Get all effects from a list of materials
    public static List<EffectData> getCombinedEffects(List<Item> materials) {
        List<EffectData> combinedEffects = new ArrayList<>();

        for (Item material : materials) {
            Map<String, EffectData> materialEffects = getMaterialEffects(material);
            combinedEffects.addAll(materialEffects.values());
        }

        return combinedEffects;
    }
}