package net.JordanRiver.KisekiLegend.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.models.UniversalFishModel;
import net.JordanRiver.KisekiLegend.entities.fish.BaseFishEntity;
import net.JordanRiver.KisekiLegend.fishing.FishRenderManager;
import net.JordanRiver.KisekiLegend.fishing.FishingGameManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UniversalFishRenderer extends GeoEntityRenderer<BaseFishEntity> {

    public UniversalFishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new UniversalFishModel());
    }

    @Override
    public void render(BaseFishEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        KisekiLegend.LOGGER.info("=== ATTEMPTING TO RENDER FISH ===");
        KisekiLegend.LOGGER.info("Entity valid: " + (entity != null && !entity.isRemoved()));

        if (entity == null || entity.isRemoved()) {
            return;
        }

        try {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

            KisekiLegend.LOGGER.info("ACTUALLY RENDERING FISH: " + entity.getFishType());
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

            poseStack.popPose();
        } catch (Exception e) {
            KisekiLegend.LOGGER.error("Failed to render fish", e);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BaseFishEntity entity) {
        if (entity == null) {
            return null;
        }
        try {
            String fishType = entity.getFishType();
            if (fishType == null || fishType.isEmpty()) {
                // Return a default or null texture if type is invalid
                return null;
            }
            return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID,
                    "textures/entity/fish/" + fishType + ".png");
        } catch (Exception e) {
            KisekiLegend.LOGGER.warn("Error getting texture for fish: " + entity.getClass().getSimpleName());
            return null; // Ensure we return null on any failure
        }
    }

    @Override
    public float getMotionAnimThreshold(BaseFishEntity animatable) {
        return 0.005f;
    }
}