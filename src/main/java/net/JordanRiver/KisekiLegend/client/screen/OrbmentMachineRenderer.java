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

// ✅ Center & lower
        poseStack.translate(0.5, 0.28, 0.5);

// ✅ Rotate based on block facing
        Direction facing = entity.getBlockState().getValue(OrbmentMachineBlock.FACING);
        float rotationY = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90)); // flat
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY)); // face direction

// ✅ Scale (you can tweak this)
        poseStack.scale(0.5f, 0.5f, 0.5f);

// 🎯 Render the item
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