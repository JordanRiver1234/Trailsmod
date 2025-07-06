package net.JordanRiver.KisekiLegend.worldgen;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBiomeModifiers {
    // Overworld only
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_EARTH_ORE =
            registerKey("add_earth_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_WIND_ORE =
            registerKey("add_wind_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_WATER_ORE =
            registerKey("add_water_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_WATER_ORE_RIVER  = registerKey("add_water_ore_river");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_WATER_ORE_OCEAN = registerKey("add_water_ore_ocean");

    // Fire in Overworld & Nether
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_FIRE_ORE =
            registerKey("add_fire_ore_overworld");
    public static final ResourceKey<BiomeModifier> ADD_NETHER_FIRE_ORE =
            registerKey("add_fire_ore_nether");

    // Space in Nether & End
    public static final ResourceKey<BiomeModifier> ADD_NETHER_SPACE_ORE =
            registerKey("add_space_ore_nether");
    public static final ResourceKey<BiomeModifier> ADD_END_SPACE_ORE =
            registerKey("add_space_ore_end");

    // Mirage in Nether & End
    public static final ResourceKey<BiomeModifier> ADD_NETHER_MIRAGE_ORE =
            registerKey("add_mirage_ore_nether");
    public static final ResourceKey<BiomeModifier> ADD_END_MIRAGE_ORE =
            registerKey("add_mirage_ore_end");

    // Time in Nether & End
    public static final ResourceKey<BiomeModifier> ADD_NETHER_TIME_ORE =
            registerKey("add_time_ore_nether");
    public static final ResourceKey<BiomeModifier> ADD_END_TIME_ORE =
            registerKey("add_time_ore_end");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // 1) Earth: underground in all Overworld biomes
        context.register(ADD_OVERWORLD_EARTH_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.OVERWORLD_EARTH_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // 2) Wind: underground only in mountain (high-altitude) biomes
        context.register(ADD_OVERWORLD_WIND_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_MOUNTAIN),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.OVERWORLD_WIND_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // 3) Water: underground only in river biomes (near water)
        context.register(ADD_OVERWORLD_WATER_ORE_RIVER,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_RIVER),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.OVERWORLD_WATER_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // b) Oceans
        context.register(ADD_OVERWORLD_WATER_ORE_OCEAN,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_OCEAN),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.OVERWORLD_WATER_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
        // To include oceans, duplicate above using BiomeTags.IS_OCEAN

        // 4) Fire: both Overworld & Nether
        context.register(ADD_OVERWORLD_FIRE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.OVERWORLD_FIRE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
        context.register(ADD_NETHER_FIRE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.NETHER_FIRE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // 5) Space: Nether & End
        context.register(ADD_NETHER_SPACE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.NETHER_SPACE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
        context.register(ADD_END_SPACE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_END),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.END_SPACE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // 6) Mirage: Nether & End
        context.register(ADD_NETHER_MIRAGE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.NETHER_MIRAGE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
        context.register(ADD_END_MIRAGE_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_END),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.END_MIRAGE_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );

        // 7) Time: Nether & End
        context.register(ADD_NETHER_TIME_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_NETHER),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.NETHER_TIME_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
        context.register(ADD_END_TIME_ORE,
                new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                        biomes.getOrThrow(BiomeTags.IS_END),
                        HolderSet.direct(
                                placed.getOrThrow(ModPlacedFeatures.END_TIME_ORE_PLACED_KEY)
                        ),
                        GenerationStep.Decoration.UNDERGROUND_ORES
                )
        );
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(
                ForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name)
        );
    }
}
