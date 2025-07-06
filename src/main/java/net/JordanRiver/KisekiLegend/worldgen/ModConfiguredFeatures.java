package net.JordanRiver.KisekiLegend.worldgen;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.data.worldgen.BootstrapContext;

import java.util.List;

public class ModConfiguredFeatures {
    // Overworld-only
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_EARTH_ORE_KEY =
            registerKey("earth_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_WIND_ORE_KEY =
            registerKey("wind_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_WATER_ORE_KEY =
            registerKey("water_ore");

    // Fire in both Overworld & Nether
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_FIRE_ORE_KEY =
            registerKey("fire_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_FIRE_ORE_KEY =
            registerKey("nether_fire_ore");

    // Space only in Nether & End
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_SPACE_ORE_KEY =
            registerKey("nether_space_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_SPACE_ORE_KEY =
            registerKey("end_space_ore");

    // Mirage only in Nether & End
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_MIRAGE_ORE_KEY =
            registerKey("nether_mirage_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_MIRAGE_ORE_KEY =
            registerKey("end_mirage_ore");

    // Time only in Nether & End
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_TIME_ORE_KEY =
            registerKey("nether_time_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_TIME_ORE_KEY =
            registerKey("end_time_ore");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables     = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables= new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables       = new BlockMatchTest(Blocks.END_STONE);

        // Overworld: earth, wind, water
        register(context,
                OVERWORLD_EARTH_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(stoneReplaceables,     ModBlocks.EARTH_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.EARTH_DEEPSLATE_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                OVERWORLD_WIND_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(stoneReplaceables,     ModBlocks.WIND_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.WIND_DEEPSLATE_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                OVERWORLD_WATER_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(stoneReplaceables,     ModBlocks.WATER_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.WATER_DEEPSLATE_ORE.get().defaultBlockState())
                ), 6)
        );

        // Fire: overworld + nether
        register(context,
                OVERWORLD_FIRE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(stoneReplaceables,     ModBlocks.FIRE_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.FIRE_DEEPSLATE_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                NETHER_FIRE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(netherrackReplaceables, ModBlocks.FIRE_ORE.get().defaultBlockState())
                ), 6)
        );

        // Space: nether + end
        register(context,
                NETHER_SPACE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(netherrackReplaceables, ModBlocks.SPACE_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                END_SPACE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(endReplaceables, ModBlocks.SPACE_ORE.get().defaultBlockState())
                ), 6)
        );

        // Mirage: nether + end
        register(context,
                NETHER_MIRAGE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(netherrackReplaceables, ModBlocks.MIRAGE_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                END_MIRAGE_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(endReplaceables, ModBlocks.MIRAGE_ORE.get().defaultBlockState())
                ), 6)
        );

        // Time: nether + end
        register(context,
                NETHER_TIME_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(netherrackReplaceables, ModBlocks.TIME_ORE.get().defaultBlockState())
                ), 6)
        );
        register(context,
                END_TIME_ORE_KEY,
                Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(endReplaceables, ModBlocks.TIME_ORE.get().defaultBlockState())
                ), 6)
        );
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(
                Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name)
        );
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            F feature,
            FC configuration
    ) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
