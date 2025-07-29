package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlock;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.JordanRiver.KisekiLegend.client.model.OrbmentMachineModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class OrbmentMachineRenderer extends GeoBlockRenderer<OrbmentMachineBlockEntity> {

    public OrbmentMachineRenderer() {
        super(new OrbmentMachineModel());
    }

    @Override
    public void render(OrbmentMachineBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // Render the orbment item if present and machine is open
        if (blockEntity.hasOrbment() && blockEntity.isOpen()) {
            renderOrbmentItem(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderOrbmentItem(OrbmentMachineBlockEntity entity, float partialTicks, PoseStack poseStack,
                                   MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemStack stack = entity.getOrbment();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        // Center the orbment on the machine block - adjust these values based on your model
        poseStack.translate(0.5, 0.3, 0.44);

        // Rotate to lay flat and face block direction
        Direction facing = entity.getBlockState().getValue(OrbmentMachineBlock.FACING);
        float rotationY = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));

        poseStack.scale(0.5f, 0.5f, 0.5f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                combinedLight,
                combinedOverlay,
                poseStack,
                buffer,
                null,
                0
        );

        poseStack.popPose();
    }
}