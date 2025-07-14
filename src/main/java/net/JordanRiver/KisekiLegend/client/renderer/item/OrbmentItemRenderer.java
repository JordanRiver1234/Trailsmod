package net.JordanRiver.KisekiLegend.client.renderer.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.CastScheduler;
import net.JordanRiver.KisekiLegend.client.model.item.OrbmentItemModel;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OrbmentItemRenderer extends GeoItemRenderer<OrbmentItem> {
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KisekiLegend.MOD_ID, "textures/item/orbment_glow.png");

    public OrbmentItemRenderer() {
        super(new OrbmentItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();

        // Scale for different view contexts
        switch (context) {
            case GUI -> {
                // Increased scale for hotbar/GUI - adjust these values as needed
                poseStack.scale(1.5f, 1.5f, 1.5f);

                // Rotate for GUI context (side-to-side rotation)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0f));

                // Push to the right and down very slightly
                poseStack.translate(-4f, -0.50f, 0.0f);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> poseStack.scale(0.8f, 0.8f, 0.8f);
            case GROUND -> poseStack.scale(0.6f, 0.6f, 0.6f);
            case HEAD -> poseStack.scale(0.5f, 0.5f, 0.5f);
            default -> poseStack.scale(1.0f, 1.0f, 1.0f);
        }

        // Check if we should render with glow effect
        Player player = Minecraft.getInstance().player;
        boolean shouldGlow = player != null &&
                CastScheduler.hasPendingCast(player.getUUID()) &&
                (player.getMainHandItem().is(stack.getItem()) || player.getOffhandItem().is(stack.getItem()));

        if (shouldGlow) {
            // Render glow effect first (behind the main model)
            renderGlowEffect(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // Render the main model
        super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderGlowEffect(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Cast the item to OrbmentItem for proper type compatibility
        if (!(stack.getItem() instanceof OrbmentItem orbmentItem)) {
            return;
        }

        // Create pulsing blue glow effect
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float alpha = 0.4f + 0.4f * Mth.sin(time * (float) Math.PI * 2);

        poseStack.pushPose();

        // Scale up slightly for glow effect
        poseStack.scale(1.02f, 1.02f, 1.02f);
        poseStack.translate(-0.01f, -0.01f, -0.01f);

        // Create a temporary model with glow texture for rendering
        OrbmentItemModel glowModel = new OrbmentItemModel() {
            @Override
            public ResourceLocation getTextureResource(OrbmentItem object) {
                return GLOW_TEXTURE;
            }

            public RenderType getRenderType(OrbmentItem animatable, ResourceLocation texture,
                                            MultiBufferSource bufferSource, float partialTick) {
                // Use translucent render type for glow effect
                return RenderType.entityTranslucent(texture);
            }
        };

        // Create a temporary renderer with the glow model
        GeoItemRenderer<OrbmentItem> glowRenderer = new GeoItemRenderer<OrbmentItem>(glowModel) {};

        // Render with glow effect
        glowRenderer.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }
}