package net.JordanRiver.KisekiLegend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderer.AuraModel;
import net.JordanRiver.KisekiLegend.entity.AuraEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AuraRenderer extends GeoEntityRenderer<AuraEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    public AuraRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AuraModel());
        this.shadowRadius = 0.0f; // No shadow for aura
        LOGGER.info("AuraRenderer initialized for kisekilegend:aura_entity");
    }

    @Override
    public ResourceLocation getTextureLocation(AuraEntity entity) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/entity/aura.png");
        LOGGER.debug("Fetching texture for AuraEntity {}: {}", entity.getId(), texture);
        return texture;
    }

    @Override
    public RenderType getRenderType(AuraEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        // Use translucent render type for glowing aura effect
        RenderType renderType = RenderType.entityTranslucent(texture);
        LOGGER.debug("Getting RenderType for AuraEntity {}: {}", animatable.getId(), renderType);
        return renderType;
    }

    @Override
    public void render(AuraEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        LOGGER.info("Rendering AuraEntity {} at position {}, tick {}", entity.getId(), entity.position(), entity.tickCount);

        // Scale the aura slightly for better visibility
        poseStack.pushPose();
        poseStack.scale(1.2f, 1.2f, 1.2f);

        // Use full brightness for glowing effect
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880); // Max light value

        poseStack.popPose();

        LOGGER.debug("Finished rendering AuraEntity {}", entity.getId());
    }
}