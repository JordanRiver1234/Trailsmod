package net.JordanRiver.KisekiLegend.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.JordanRiver.KisekiLegend.KisekiLegend;
import net.JordanRiver.KisekiLegend.block.OrbalTableBlock;
import net.JordanRiver.KisekiLegend.block.entity.OrbalTableBlockEntity;
import net.JordanRiver.KisekiLegend.client.model.OrbalTableModel;
import net.JordanRiver.KisekiLegend.items.QuartzItem;
import net.JordanRiver.KisekiLegend.util.WeaponSlotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.List;

public class OrbalTableRenderer extends GeoBlockRenderer<OrbalTableBlockEntity> {
    private final ItemRenderer itemRenderer;
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KisekiLegend.MOD_ID, "textures/gui/orbal_table.png");



    private VertexConsumer getSafeGUIBuffer(MultiBufferSource bufferSource) {
        return bufferSource.getBuffer(RenderType.entityCutout(GUI_TEXTURE));
    }
    public OrbalTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new OrbalTableModel());
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }
    @Override
    public void actuallyRender(PoseStack poseStack, OrbalTableBlockEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {

        // Render the main block model first
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, color);

        // Get weapon from both sources
        ItemStack currentWeapon = animatable.getWeaponItem();
        if (currentWeapon.isEmpty()) {
            currentWeapon = animatable.getInventory().getStackInSlot(0);
        }

        if (!currentWeapon.isEmpty()) {
            renderWeaponAndSlots(poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        } else {
        }

        // Render GUI screens (monitors)
    }

    private void renderWeaponAndSlots(PoseStack poseStack, OrbalTableBlockEntity blockEntity,
                                      MultiBufferSource bufferSource, float partialTick,
                                      int packedLight, int packedOverlay) {

        ItemStack weapon = blockEntity.getWeaponItem();
        if (weapon.isEmpty()) {
            weapon = blockEntity.getInventory().getStackInSlot(0);
        }

        if (weapon.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        Direction facing = blockEntity.getBlockState().getValue(OrbalTableBlock.FACING);
        poseStack.translate(0.1, 1.05, 0); // Center on block, slightly above surface


        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
        }

        poseStack.scale(0.7f, 0.7f, 0.7f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45)); // Slight angle for better visibility


        // Render weapon with proper lighting
        itemRenderer.renderStatic(weapon, ItemDisplayContext.GROUND,
                240, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);

        poseStack.popPose();
    }
    private void renderSlots(PoseStack poseStack, WeaponSlotData slotData, MultiBufferSource bufferSource,
                             float partialTick, int packedLight, int packedOverlay) {
        // Just log slot info for debugging, don't render anything
        for (WeaponSlotData.RenderSlotInfo slotInfo : slotData.getRenderInfo()) {
        }
    }

    private void renderGUIScreen(PoseStack poseStack, OrbalTableBlockEntity blockEntity,
                                 MultiBufferSource bufferSource, int packedLight) {

        if (!blockEntity.isMonitorOpen()) {
            return; // Only render when monitor is open
        }

        poseStack.pushPose();

        // Position the GUI screen relative to the block
        // Adjust these coordinates based on your block model
        Direction facing = blockEntity.getBlockState().getValue(OrbalTableBlock.FACING);

        // Base position - adjust these to match your computer screen location
        float screenX = 0.0f;
        float screenY = 0.6f; // Height of the screen
        float screenZ = 0.0f;

        // Adjust position based on block facing direction
        switch (facing) {
            case NORTH -> {
                screenX = 0.7f;  // Right side of block
                screenZ = 0.0f;
            }
            case SOUTH -> {
                screenX = -0.7f; // Left side of block
                screenZ = 0.0f;
            }
            case EAST -> {
                screenX = 0.0f;
                screenZ = 0.7f;  // Back of block
            }
            case WEST -> {
                screenX = 0.0f;
                screenZ = -0.7f; // Front of block
            }
        }

        poseStack.translate(screenX, screenY, screenZ);

        // Face the screen towards the player/correct direction
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
        }

        // Scale the GUI to fit the screen size
        float guiScale = 0.008f; // Adjust this to make GUI bigger/smaller
        poseStack.scale(guiScale, guiScale, 0.001f);

        // Render the GUI texture
        VertexConsumer buffer =  bufferSource.getBuffer(RenderType.entityCutout(GUI_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        // GUI dimensions (adjust based on your GUI size)
        float guiWidth = 256f;  // Your GUI width
        float guiHeight = 240f; // Your GUI height

        float halfWidth = guiWidth / 2f;
        float halfHeight = guiHeight / 2f;


        // Add animated elements if needed

        poseStack.popPose();
    }




















    private int getElementColor(String element) {
        return switch (element.toLowerCase()) {
            case "earth" -> 0x8B4513; // Brown
            case "water" -> 0x0066CC; // Blue
            case "fire" -> 0xFFD92222;  // Red-Orange
            case "wind" -> 0x90EE90;  // Light Green
            case "time" -> 0x9370DB;  // Purple
            case "space" -> 0xFFD9D522; // Yellow
            case "mirage" -> 0xFF888888; // Grey
            default -> 0xFF404040;      // Gray
        };
    }

    @Override
    public boolean shouldRenderOffScreen(OrbalTableBlockEntity blockEntity) {
        return !blockEntity.getWeaponItem().isEmpty();
    }

    @Override
    public int getViewDistance() {
        return 64; // Increase render distance
    }
}
