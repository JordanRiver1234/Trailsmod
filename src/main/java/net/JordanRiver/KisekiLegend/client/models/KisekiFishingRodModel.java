package net.JordanRiver.KisekiLegend.client.models;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KisekiFishingRodModel extends GeoModel<KisekiFishingRodItem> {

    @Override
    public ResourceLocation getModelResource(KisekiFishingRodItem animatable) {
        // Use the same model for all rod types
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "geo/item/kiseki_fishing_rod.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KisekiFishingRodItem animatable) {
        // Different texture based on rod type
        String textureFile = switch (animatable.getRodType()) {
            case PROGRESS_ROD -> "progress_rod";
            case MARINE_STAR_ROD -> "marine_star_rod";
            case PISCES_HEART -> "pisces_heart";
            case BAMBOO_FISHING_ROD -> "bamboo_fishing_rod";
            case METAL_TRIDENT_ROD -> "metal_trident_rod";
            case LAKELORD_II -> "lakelord_ii";
            case AQUA_MASTER -> "aqua_master";
        };

        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "textures/item/" + textureFile + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(KisekiFishingRodItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "animations/item/kiseki_fishing_rod.animation.json");
    }
}