package net.JordanRiver.KisekiLegend.client.renderer.item;

import com.mojang.blaze3d.systems.RenderSystem;
import net.JordanRiver.KisekiLegend.client.model.item.OrbmentItemModel;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

public class OrbmentItemRenderer extends GeoItemRenderer<OrbmentItem> {
    private static final ResourceLocation GUI_ICON =
            ResourceLocation.fromNamespaceAndPath("kisekilegend", "textures/gui/orbment.png");

    public OrbmentItemRenderer() {
        super(new OrbmentItemModel());
    }


    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Only render in 3D for non-GUI contexts
        if (context != ItemDisplayContext.GUI && context != ItemDisplayContext.FIXED) {
            super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }
        // Else: use vanilla 2D icon (via item model JSON)
    }
}