package net.JordanRiver.KisekiLegend.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlock;
import net.JordanRiver.KisekiLegend.block.OrbmentMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

public class OrbmentMachineRenderer implements BlockEntityRenderer<OrbmentMachineBlockEntity> {

    public OrbmentMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(OrbmentMachineBlockEntity entity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemStack stack = entity.getOrbment();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

// Center the orbment on the machine block
        poseStack.translate(0.5, 0.3, 0.44);// was 0.53->0.57

// Rotate to lay flat and face block direction
        Direction facing = entity.getBlockState().getValue(OrbmentMachineBlock.FACING);
        float rotationY = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 90f;   // Swapped with EAST
            case EAST -> 270f;  // Swapped with WEST
            default -> 0f;
        };

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));     // lay flat
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90)); // rotate with block

        poseStack.scale(0.5f, 0.5f, 0.5f); // size

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