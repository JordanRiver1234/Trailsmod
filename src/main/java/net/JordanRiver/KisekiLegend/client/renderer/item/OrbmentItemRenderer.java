package net.JordanRiver.KisekiLegend.client.renderer.item;

import net.JordanRiver.KisekiLegend.client.model.item.OrbmentItemModel;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class OrbmentItemRenderer extends GeoItemRenderer<OrbmentItem> {

    public OrbmentItemRenderer() {
        super(new OrbmentItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Skip rendering for GUI contexts - let vanilla handle it with the texture
        if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
            return; // Don't render anything, vanilla will use the texture from resources
        }

        // For all other contexts (hand-held, ground, etc.), render the 3D GeckoLib model
        super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
    }
}