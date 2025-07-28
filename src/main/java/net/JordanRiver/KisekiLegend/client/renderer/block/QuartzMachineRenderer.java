// --- FILE: net/JordanRiver/KisekiLegend/client/renderer/block/QuartzMachineRenderer.java ---
package net.JordanRiver.KisekiLegend.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.entity.QuartzMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation; // IMPORT THIS
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class QuartzMachineRenderer implements BlockEntityRenderer<QuartzMachineBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public QuartzMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(QuartzMachineBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        // --- 1. Render the main two-block-high model ---
        pPoseStack.pushPose();
        BlockState blockState = pBlockEntity.getBlockState();

        // CORRECTED: We now create the correct ModelResourceLocation type.
        ModelResourceLocation modelLocation = new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "quartz_machine"), "");

        BakedModel model;
        try {
            BakedModel customModel = Minecraft.getInstance().getModelManager().getModel(modelLocation);
            if (customModel != null && customModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
                model = customModel;
            } else {
                model = this.blockRenderer.getBlockModel(blockState);
            }
        } catch (Exception e) {
            model = this.blockRenderer.getBlockModel(blockState);
        }

        // This renders the model using its blockstate properties.
        this.blockRenderer.getModelRenderer().renderModel(
                pPoseStack.last(),
                pBuffer.getBuffer(RenderType.solid()),
                blockState,
                model,
                1.0f, 1.0f, 1.0f,
                pPackedLight,
                pPackedOverlay,
                ModelData.EMPTY,
                RenderType.solid()
        );
        pPoseStack.popPose();


        // --- 2. Render the floating synthesis items (this logic is unchanged) ---
        if (pBlockEntity.isSynthesizing()) {
            renderSynthesisItems(pBlockEntity, pPartialTick, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }
    }

    private void renderSynthesisItems(QuartzMachineBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        List<QuartzMachineBlockEntity.ItemRenderData> itemPositions = pBlockEntity.getFloatingItemPositions();
        if (itemPositions.isEmpty()) return;

        for (QuartzMachineBlockEntity.ItemRenderData itemData : itemPositions) {
            pPoseStack.pushPose();

            // Position the item in world space (relative to the block entity)
            pPoseStack.translate(
                    itemData.x() - pBlockEntity.getBlockPos().getX(),
                    itemData.y() - pBlockEntity.getBlockPos().getY(),
                    itemData.z() - pBlockEntity.getBlockPos().getZ()
            );

            // Add slight rotation for visual effect
            long time = pBlockEntity.getLevel().getGameTime();
            float rotation = (time + pPartialTick) * 2.0F;
            pPoseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            // Scale items slightly smaller
            pPoseStack.scale(0.3f, 0.3f, 0.3f);

            // Render the item
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    itemData.item(),
                    ItemDisplayContext.FIXED,
                    pPackedLight,
                    pPackedOverlay,
                    pPoseStack,
                    pBuffer,
                    pBlockEntity.getLevel(),
                    0
            );

            pPoseStack.popPose();
        }
    }
}