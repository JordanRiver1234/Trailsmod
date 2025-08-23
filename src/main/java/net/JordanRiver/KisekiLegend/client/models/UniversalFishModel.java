package net.JordanRiver.KisekiLegend.client.models;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UniversalFishModel extends GeoModel<BaseFishEntity> {

    @Override
    public ResourceLocation getModelResource(BaseFishEntity animatable) {
        String fishType = animatable.getFishType();
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "geo/entity/fish/" + fishType + ".geo.json");
        KisekiLegend.LOGGER.info("Loading model: " + modelLocation);
        return modelLocation;
    }

    @Override
    public ResourceLocation getTextureResource(BaseFishEntity animatable) {
        String fishType = animatable.getFishType();
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "textures/entity/fish/" + fishType + ".png");
        KisekiLegend.LOGGER.info("Loading texture: " + textureLocation + " for fish type: " + fishType);

        // Additional debug info
        if (fishType == null || fishType.isEmpty()) {
            KisekiLegend.LOGGER.error("Fish type is null or empty for entity: " + animatable.getClass().getSimpleName());
        }

        return textureLocation;
    }

    @Override
    public ResourceLocation getAnimationResource(BaseFishEntity animatable) {
        String fishType = animatable.getFishType();
        ResourceLocation animationLocation = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                "animations/entity/fish/" + fishType + ".animation.json");
        KisekiLegend.LOGGER.info("Loading animation: " + animationLocation);
        return animationLocation;
    }
}