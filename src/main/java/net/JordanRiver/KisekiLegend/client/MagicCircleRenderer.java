package net.JordanRiver.KisekiLegend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.renderer.MagicCircleModel;
import net.JordanRiver.KisekiLegend.entity.MagicCircleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MagicCircleRenderer extends GeoEntityRenderer<MagicCircleEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    public MagicCircleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MagicCircleModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCircleEntity entity) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/entity/magic_circle.png");
        return texture;
    }

    @Override
    public RenderType getRenderType(MagicCircleEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        LOGGER.debug("Getting RenderType for MagicCircleEntity {}: {}", animatable.getId(), RenderType.entityTranslucent(texture));
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(MagicCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        LOGGER.info("Rendering MagicCircleEntity {} at position {}, tick {}", entity.getId(), entity.position(), entity.tickCount);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        LOGGER.debug("Finished rendering MagicCircleEntity {}", entity.getId());
    }
}