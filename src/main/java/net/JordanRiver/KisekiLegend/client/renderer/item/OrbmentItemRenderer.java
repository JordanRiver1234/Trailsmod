package net.JordanRiver.KisekiLegend.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.client.model.item.OrbmentItemModel;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OrbmentItemRenderer extends GeoItemRenderer<OrbmentItem> {

    public OrbmentItemRenderer() {
        super(new OrbmentItemModel());
        System.out.println("GeoItemRenderer: OrbmentItemRenderer is loaded!");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Only render 3D model for hand contexts, not for GUI/hotbar
        switch (transformType) {
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
            case GROUND:
            case FIXED:
                // Use 3D GeckoLib model for these contexts
                super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
                break;

            case GUI:
            case HEAD:
            case NONE:
            default:
                // Don't render - let default 2D model handle these
                break;
        }
    }


    public RenderType getRenderType(OrbmentItem animatable, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}