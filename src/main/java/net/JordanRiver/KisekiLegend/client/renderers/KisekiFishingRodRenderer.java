package net.JordanRiver.KisekiLegend.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.client.models.KisekiFishingRodModel;
import net.JordanRiver.KisekiLegend.items.KisekiFishingRodItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class KisekiFishingRodRenderer extends GeoItemRenderer<KisekiFishingRodItem> {

    public KisekiFishingRodRenderer() {
        super(new KisekiFishingRodModel());
    }
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof KisekiFishingRodItem rodItem) {
            // Use Geckolib rendering - call the parent method
            super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        }
    }
}