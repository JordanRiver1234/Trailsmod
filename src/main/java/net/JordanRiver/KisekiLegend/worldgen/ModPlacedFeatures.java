package net.JordanRiver.KisekiLegend.worldgen;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class ModPlacedFeatures {
    // Overworld-only
    public static final ResourceKey<PlacedFeature> OVERWORLD_EARTH_ORE_PLACED_KEY =
            registerKey("earth_ore_placed");
    public static final ResourceKey<PlacedFeature> OVERWORLD_WIND_ORE_PLACED_KEY =
            registerKey("wind_ore_placed");
    public static final ResourceKey<PlacedFeature> OVERWORLD_WATER_ORE_PLACED_KEY =
            registerKey("water_ore_placed");

    // Fire in both Overworld & Nether
    public static final ResourceKey<PlacedFeature> OVERWORLD_FIRE_ORE_PLACED_KEY =
            registerKey("fire_ore_placed");
    public static final ResourceKey<PlacedFeature> NETHER_FIRE_ORE_PLACED_KEY =
            registerKey("nether_fire_ore_placed");

    // Space only in Nether & End
    public static final ResourceKey<PlacedFeature> NETHER_SPACE_ORE_PLACED_KEY =
            registerKey("nether_space_ore_placed");
    public static final ResourceKey<PlacedFeature> END_SPACE_ORE_PLACED_KEY =
            registerKey("end_space_ore_placed");

    // Mirage only in Nether & End
    public static final ResourceKey<PlacedFeature> NETHER_MIRAGE_ORE_PLACED_KEY =
            registerKey("nether_mirage_ore_placed");
    public static final ResourceKey<PlacedFeature> END_MIRAGE_ORE_PLACED_KEY =
            registerKey("end_mirage_ore_placed");

    // Time only in Nether & End
    public static final ResourceKey<PlacedFeature> NETHER_TIME_ORE_PLACED_KEY =
            registerKey("nether_time_ore_placed");
    public static final ResourceKey<PlacedFeature> END_TIME_ORE_PLACED_KEY =
            registerKey("end_time_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // 1) Earth, Water, Fire: "5 veins per chunk, -64 to +80"
        var commonPlacement = ModOrePlacement.commonOrePlacement(
                5,
                HeightRangePlacement.uniform(
                        VerticalAnchor.absolute(-64),
                        VerticalAnchor.absolute(80)
                )
        );

        // 2) Wind: "5 veins per chunk, high altitudes only" (e.g. y=80→200)
        var windHighPlacement = ModOrePlacement.commonOrePlacement(
                5,
                HeightRangePlacement.uniform(
                        VerticalAnchor.absolute(80),
                        VerticalAnchor.absolute(200)
                )
        );


        // Overworld: earth, wind, water, fire
        register(context,
                OVERWORLD_EARTH_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_EARTH_ORE_KEY),
                commonPlacement
        );
        register(context,
                OVERWORLD_WIND_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_WIND_ORE_KEY),
                commonPlacement
        );
        register(context,
                OVERWORLD_WATER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_WATER_ORE_KEY),
                commonPlacement
        );
        register(context,
                OVERWORLD_FIRE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_FIRE_ORE_KEY),
                commonPlacement
        );

        // Nether: fire, space, mirage, time
        register(context,
                NETHER_FIRE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_FIRE_ORE_KEY),
                commonPlacement
        );
        register(context,
                NETHER_SPACE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_SPACE_ORE_KEY),
                commonPlacement
        );
        register(context,
                NETHER_MIRAGE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_MIRAGE_ORE_KEY),
                commonPlacement
        );
        register(context,
                NETHER_TIME_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_TIME_ORE_KEY),
                commonPlacement
        );

        // End: space, mirage, time
        register(context,
                END_SPACE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.END_SPACE_ORE_KEY),
                commonPlacement
        );
        register(context,
                END_MIRAGE_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.END_MIRAGE_ORE_KEY),
                commonPlacement
        );
        register(context,
                END_TIME_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.END_TIME_ORE_KEY),
                commonPlacement
        );
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(
                Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, name)
        );
    }

    private static void register(
            BootstrapContext<PlacedFeature> context,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> config,
            List<PlacementModifier> modifiers
    ) {
        context.register(key, new PlacedFeature(config, List.copyOf(modifiers)));
    }
}
