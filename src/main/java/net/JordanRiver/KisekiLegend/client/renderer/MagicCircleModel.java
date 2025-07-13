package net.JordanRiver.KisekiLegend.client.renderer;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.MagicCircleEntity;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.model.GeoModel;

public class MagicCircleModel extends GeoModel<MagicCircleEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public ResourceLocation getModelResource(MagicCircleEntity animatable) {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/magic_circle.geo.json");
        LOGGER.debug("Loading model for MagicCircleEntity: {}", model);
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(MagicCircleEntity animatable) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/entity/magic_circle.png");
        LOGGER.debug("Loading texture for MagicCircleEntity: {}", texture);
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(MagicCircleEntity animatable) {
        ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/magic_circle.animation.json");
        LOGGER.debug("Loading animation for MagicCircleEntity: {}", animation);
        return animation;
    }
}