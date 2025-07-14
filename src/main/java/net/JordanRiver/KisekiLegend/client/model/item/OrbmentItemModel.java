package net.JordanRiver.KisekiLegend.client.model.item;

import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.client.CastScheduler;
import net.JordanRiver.KisekiLegend.items.OrbmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.animation.AnimationState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class OrbmentItemModel extends DefaultedItemGeoModel<OrbmentItem> {

    private static final ResourceLocation NORMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KisekiLegend.MOD_ID, "textures/item/orbment_texture.png");
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KisekiLegend.MOD_ID, "textures/item/orbment_glow.png");

    public OrbmentItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "orbment"));
    }

    @Override
    public ResourceLocation getModelResource(OrbmentItem object) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "geo/orbment.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OrbmentItem object) {
        return NORMAL_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OrbmentItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "animations/orbment.animation.json");
    }

    public RenderType getRenderType(OrbmentItem animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        // Always use the normal render type for base model
        // Glow effect is handled separately in the renderer
        return RenderType.entityCutoutNoCull(texture);
    }

    // Remove @Override and just override the method if it exists in parent
    public void setCustomAnimations(OrbmentItem animatable, long instanceId, AnimationState<OrbmentItem> animationState) {
        // Check if parent has this method before calling super
        try {
            super.setCustomAnimations(animatable, instanceId, animationState);
        } catch (Exception e) {
            // If parent doesn't have this method, just continue
        }

        // You can add casting-specific animations here if needed
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // Get the item stack from animation state data
            ItemStack stack = animationState.getData(DataTickets.ITEMSTACK);
            if (stack != null) {
                boolean isCasting = CastScheduler.hasPendingCast(player.getUUID()) &&
                        (player.getMainHandItem().is(stack.getItem()) || player.getOffhandItem().is(stack.getItem()));

                if (isCasting) {
                    // Trigger casting animation if you have one
                    // animationState.getController().setAnimation(RawAnimation.begin().thenPlay("casting"));
                }
            }
        }
    }

    // Helper method to check if the item should glow
    public boolean shouldGlow(OrbmentItem item) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        return CastScheduler.hasPendingCast(player.getUUID()) &&
                (player.getMainHandItem().is(item) || player.getOffhandItem().is(item));
    }
}