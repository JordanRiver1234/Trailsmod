package net.JordanRiver.KisekiLegend.client.renderer;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.model.GeoModel;

public class AuraModel extends GeoModel<AuraEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public ResourceLocation getModelResource(AuraEntity animatable) {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/aura_pulse.geo.json");
        LOGGER.debug("Loading model for AuraEntity: {}", model);
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(AuraEntity animatable) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/entity/aura.png");
        LOGGER.debug("Loading texture for AuraEntity: {}", texture);
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(AuraEntity animatable) {
        ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/aura_pulse.animation.json");
        LOGGER.debug("Loading animation for AuraEntity: {}", animation);
        return animation;
    }
}